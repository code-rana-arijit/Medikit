package com.medikit.payment.service;

import com.medikit.payment.dto.PaymentRequest;
import com.medikit.payment.entity.Payment;
import com.medikit.payment.gateway.PaymentGateway;
import com.medikit.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Mock
    private com.medikit.common.event.EventPublisher eventPublisher;

    @Mock
    private PaymentGateway gateway;

    @Mock
    private com.medikit.common.audit.AuditService auditService;

    private PaymentService paymentService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, eventPublisher, redisTemplate, gateway, auditService, true);
    }

    @Test
    void capture_completesPaymentAndMarksCaptured() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .merchantRefId("REF123")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .method("CARD")
                .status(Payment.PaymentStatus.INITIATED)
                .provider("MOCK_GATEWAY")
                .build();

        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(gateway.capture(any(), any(), any()))
                .thenReturn(new PaymentGateway.GatewayResult(true, "REF123", "captured"));

        var response = paymentService.capture(payment.getOrderId().toString());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.capturedAt()).isNotNull();
    }
}
