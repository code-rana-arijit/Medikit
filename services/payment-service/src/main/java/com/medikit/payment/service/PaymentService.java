package com.medikit.payment.service;

import com.medikit.common.audit.AuditService;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.payment.dto.PaymentRequest;
import com.medikit.payment.dto.PaymentResponse;
import com.medikit.payment.dto.RefundRequest;
import com.medikit.payment.entity.Payment;
import com.medikit.payment.gateway.PaymentGateway;
import com.medikit.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String IDEMPOTENCY_PREFIX = "medikit:payment:idem:";

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final PaymentGateway gateway;
    private final AuditService auditService;
    private final boolean autoCapture;

    public PaymentService(PaymentRepository paymentRepository,
                          EventPublisher eventPublisher,
                          StringRedisTemplate redisTemplate,
                          PaymentGateway gateway,
                          AuditService auditService,
                          @Value("${medikit.payment.auto-capture:true}") boolean autoCapture) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
        this.gateway = gateway;
        this.auditService = auditService;
        this.autoCapture = autoCapture;
    }

    @Transactional
    public PaymentResponse initiate(PaymentRequest request) {
        if (paymentRepository.existsByOrderId(request.orderId())) {
            return PaymentResponse.from(paymentRepository.findByOrderId(request.orderId()).orElseThrow());
        }

        String idempotencyKey = request.idempotencyKey() != null
                ? request.idempotencyKey()
                : request.orderId().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENCY_PREFIX + idempotencyKey, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ConflictException("Duplicate payment request");
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .merchantRefId(generateRefId())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "INR")
                .method(request.method() != null ? request.method() : "CARD")
                .status(Payment.PaymentStatus.INITIATED)
                .provider(gateway.name())
                .build();

        PaymentGateway.GatewayOrder order = gateway.createOrder(new PaymentGateway.CreateOrderRequest(
                payment.getMerchantRefId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                "Medikit order " + payment.getOrderId(),
                null));

        payment.setMetadata(order.checkoutUrl() != null ? "{\"checkoutUrl\":\"" + order.checkoutUrl() + "\"}" : null);

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(Topics.PAYMENT_INITIATED, saved.getOrderId().toString(),
                Map.of("paymentId", saved.getId().toString(), "orderId", saved.getOrderId().toString()));

        if (autoCapture) {
            capture(saved.getOrderId().toString());
        }

        return PaymentResponse.from(saved);
    }

    @Transactional
    public PaymentResponse capture(String orderId) {
        Payment payment = paymentRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            return PaymentResponse.from(payment);
        }

        PaymentGateway.GatewayResult result = gateway.capture(
                payment.getMerchantRefId(), payment.getAmount(), payment.getCurrency());

        if (!result.success()) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(result.message());
            paymentRepository.save(payment);
            eventPublisher.publish(Topics.PAYMENT_FAILED, orderId,
                    Map.of("orderId", orderId, "reason", result.message()));
            return PaymentResponse.from(payment);
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setCaptured(true);
        payment.setCapturedAt(Instant.now());
        paymentRepository.save(payment);

        eventPublisher.publish(Topics.PAYMENT_COMPLETED, orderId,
                Map.of("paymentId", payment.getId().toString(),
                        "orderId", orderId,
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency()));

        auditService.record(AuditService.AuditAction.PAYMENT_CAPTURED, null, "SYSTEM", "payment",
                payment.getId().toString(), Map.of(
                        "orderId", payment.getOrderId().toString(),
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency(),
                        "provider", payment.getProvider()));

        log.info("Payment {} captured for order {}", payment.getId(), orderId);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse refund(RefundRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded");
        }
        if (request.amount() != null && request.amount().compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount exceeds payment amount");
        }

        BigDecimal refundAmount = request.amount() != null ? request.amount() : payment.getAmount();
        PaymentGateway.GatewayResult result = gateway.refund(
                payment.getMerchantRefId(), refundAmount, payment.getCurrency());

        if (!result.success()) {
            throw new IllegalStateException("Refund failed at gateway: " + result.message());
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        auditService.record(AuditService.AuditAction.PAYMENT_REFUNDED, null, "SYSTEM", "payment",
                payment.getId().toString(), Map.of(
                        "orderId", payment.getOrderId().toString(),
                        "amount", refundAmount,
                        "currency", payment.getCurrency()));

        log.info("Payment {} refunded for order {}", payment.getId(), request.orderId());
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getByOrder(String orderId) {
        return PaymentResponse.from(paymentRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new NotFoundException("Payment not found")));
    }

    /**
     * Handle a gateway webhook. Maps provider events to internal state and
     * emits the corresponding domain event.
     */
    @Transactional
    public void handleWebhook(String rawBody, Map<String, String> headers) {
        PaymentGateway.WebhookEvent event = gateway.parseWebhook(rawBody, headers);
        if (event.providerRef() == null || event.providerRef().isBlank()) {
            return;
        }
        Payment payment = paymentRepository.findByMerchantRefId(event.providerRef())
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook for unknown merchant ref {}", event.providerRef());
            return;
        }
        if (event.status() == null || event.status().isBlank()) {
            return;
        }
        switch (event.status().toLowerCase()) {
            case "captured", "succeeded", "authorized" -> {
                if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setCaptured(true);
                    payment.setCapturedAt(Instant.now());
                    paymentRepository.save(payment);
                    eventPublisher.publish(Topics.PAYMENT_COMPLETED, payment.getOrderId().toString(),
                            Map.of("paymentId", payment.getId().toString(),
                                    "orderId", payment.getOrderId().toString(),
                                    "amount", payment.getAmount(),
                                    "currency", payment.getCurrency()));
                }
            }
            case "failed", "declined" -> {
                if (payment.getStatus() != Payment.PaymentStatus.FAILED) {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason("Webhook status: " + event.status());
                    paymentRepository.save(payment);
                    eventPublisher.publish(Topics.PAYMENT_FAILED, payment.getOrderId().toString(),
                            Map.of("orderId", payment.getOrderId().toString(), "reason", event.status()));
                }
            }
            case "refunded" -> {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                eventPublisher.publish(Topics.PAYMENT_REFUNDED, payment.getOrderId().toString(),
                        Map.of("orderId", payment.getOrderId().toString()));
            }
            default -> log.debug("Ignoring webhook status {}", event.status());
        }
    }

    private String generateRefId() {
        return "MDKPAY" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
