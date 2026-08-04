package com.medikit.payment.service;

import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.payment.dto.PaymentRequest;
import com.medikit.payment.dto.PaymentResponse;
import com.medikit.payment.dto.RefundRequest;
import com.medikit.payment.entity.Payment;
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
    private final boolean autoCapture;

    public PaymentService(PaymentRepository paymentRepository,
                          EventPublisher eventPublisher,
                          StringRedisTemplate redisTemplate,
                          @Value("${medikit.payment.auto-capture:true}") boolean autoCapture) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
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
                .provider("MOCK_GATEWAY")
                .build();

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

        // Mock gateway always succeeds unless amount exceeds threshold
        if (payment.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Amount exceeds single transaction limit");
            paymentRepository.save(payment);
            eventPublisher.publish(Topics.PAYMENT_FAILED, orderId,
                    Map.of("orderId", orderId, "reason", "Amount exceeds single transaction limit"));
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

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Payment {} refunded for order {}", payment.getId(), request.orderId());
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getByOrder(String orderId) {
        return PaymentResponse.from(paymentRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new NotFoundException("Payment not found")));
    }

    private String generateRefId() {
        return "MDKPAY" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
