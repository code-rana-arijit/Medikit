package com.medikit.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction over a real payment gateway provider.
 * <p>
 * Implementations translate Medikit payment intents into provider-specific
 * API calls. The {@code MockPaymentGateway} remains the default for local
 * development; Razorpay and Stripe adapters are selected via
 * {@code medikit.payment.provider}.
 * </p>
 */
public interface PaymentGateway {

    String name();

    /**
     * Create a payment/order intent at the provider.
     *
     * @return map with provider-specific fields, at minimum {@code providerRef}
     */
    GatewayOrder createOrder(CreateOrderRequest request);

    /**
     * Capture an authorized payment.
     */
    GatewayResult capture(String providerRef, BigDecimal amount, String currency);

    /**
     * Issue a refund against a captured payment.
     */
    GatewayResult refund(String providerRef, BigDecimal amount, String currency);

    /**
     * Validate and parse an incoming webhook payload.
     *
     * @return map containing {@code providerRef}, {@code eventType} and
     *         optionally {@code amount}/{@code status}
     * @throws IllegalArgumentException when signature validation fails
     */
    WebhookEvent parseWebhook(String rawBody, Map<String, String> headers);

    record CreateOrderRequest(String merchantRefId, BigDecimal amount, String currency, String method,
                              String description, String callbackUrl) {
    }

    record GatewayOrder(String providerRef, String checkoutUrl, Map<String, String> additional) {
    }

    record GatewayResult(boolean success, String providerRef, String message) {
    }

    record WebhookEvent(String providerRef, String eventType, String status, BigDecimal amount) {
    }
}
