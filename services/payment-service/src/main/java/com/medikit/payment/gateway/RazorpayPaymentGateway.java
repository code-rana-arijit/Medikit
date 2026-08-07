package com.medikit.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Razorpay adapter.
 * <p>
 * Docs: https://razorpay.com/docs/api/payments/order/
 * </p>
 */
@Component
@ConditionalOnProperty(name = "medikit.payment.provider", havingValue = "razorpay")
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayPaymentGateway(RestClient.Builder builder,
                                  ObjectMapper objectMapper,
                                  @Value("${medikit.payment.razorpay.key-id:}") String keyId,
                                  @Value("${medikit.payment.razorpay.key-secret:}") String keySecret,
                                  @Value("${medikit.payment.razorpay.webhook-secret:}") String webhookSecret) {
        String creds = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        this.client = builder
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + creds)
                .build();
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String name() {
        return "RAZORPAY";
    }

    @Override
    public GatewayOrder createOrder(CreateOrderRequest request) {
        Map<String, Object> body = Map.of(
                "amount", request.amount().movePointRight(2).longValueExact(), // paise
                "currency", request.currency(),
                "receipt", request.merchantRefId(),
                "notes", Map.of("medikitRef", request.merchantRefId()));
        JsonNode resp = post("/orders", body);
        String orderId = resp.path("id").asText();
        String checkout = "https://checkout.razorpay.com/v1/payment/order_id=" + orderId;
        return new GatewayOrder(orderId, checkout, Map.of());
    }

    @Override
    public GatewayResult capture(String providerRef, BigDecimal amount, String currency) {
        Map<String, Object> body = Map.of(
                "amount", amount.movePointRight(2).longValueExact(),
                "currency", currency);
        JsonNode resp = post("/payments/" + providerRef + "/capture", body);
        boolean ok = "captured".equalsIgnoreCase(resp.path("status").asText());
        return new GatewayResult(ok, providerRef, resp.path("status").asText());
    }

    @Override
    public GatewayResult refund(String providerRef, BigDecimal amount, String currency) {
        Map<String, Object> body = Map.of(
                "amount", amount.movePointRight(2).longValueExact(),
                "currency", currency);
        JsonNode resp = post("/payments/" + providerRef + "/refund", body);
        boolean ok = resp.has("id") || "processed".equalsIgnoreCase(resp.path("status").asText());
        return new GatewayResult(ok, providerRef, resp.path("status").asText("processed"));
    }

    @Override
    public WebhookEvent parseWebhook(String rawBody, Map<String, String> headers) {
        String signature = headers.getOrDefault("x-razorpay-signature", "");
        if (webhookSecret != null && !webhookSecret.isBlank() && !verifySignature(rawBody, signature)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String eventType = node.path("event").asText();
            JsonNode payment = node.path("payload").path("payment").path("entity");
            String providerRef = payment.path("id").asText();
            String status = payment.path("status").asText();
            BigDecimal amount = payment.path("amount").isMissingNode() ? null
                    : new BigDecimal(payment.path("amount").asLong()).movePointLeft(2);
            return new WebhookEvent(providerRef, eventType, status, amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed Razorpay webhook payload", e);
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(body);
            return client.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve().body(JsonNode.class);
        } catch (Exception e) {
            log.error("[razorpay] POST {} failed", path, e);
            throw new IllegalStateException("Razorpay request failed: " + e.getMessage(), e);
        }
    }

    private boolean verifySignature(String body, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expectedHex = toHex(expected);
            return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[razorpay] signature verification failed", e);
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
