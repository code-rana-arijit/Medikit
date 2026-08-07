package com.medikit.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Deterministic in-memory gateway used for local development and tests.
 * Always succeeds unless the amount exceeds the single-transaction limit.
 */
@Component
@ConditionalOnProperty(name = "medikit.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    @Override
    public String name() {
        return "MOCK_GATEWAY";
    }

    @Override
    public GatewayOrder createOrder(CreateOrderRequest request) {
        log.info("[mock] createOrder {} amount={} {}", request.merchantRefId(), request.amount(), request.currency());
        return new GatewayOrder("mock_" + request.merchantRefId(), null, Map.of());
    }

    @Override
    public GatewayResult capture(String providerRef, BigDecimal amount, String currency) {
        if (amount != null && amount.compareTo(MAX_AMOUNT) > 0) {
            return new GatewayResult(false, providerRef, "Amount exceeds single transaction limit");
        }
        return new GatewayResult(true, providerRef, "captured");
    }

    @Override
    public GatewayResult refund(String providerRef, BigDecimal amount, String currency) {
        return new GatewayResult(true, providerRef, "refunded");
    }

    @Override
    public WebhookEvent parseWebhook(String rawBody, Map<String, String> headers) {
        return new WebhookEvent(null, null, null, null);
    }
}
