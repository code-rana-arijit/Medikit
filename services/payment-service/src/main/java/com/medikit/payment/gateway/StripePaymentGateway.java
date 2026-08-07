package com.medikit.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Stripe adapter.
 * <p>
 * Docs: https://docs.stripe.com/api/payment_intents
 * </p>
 */
@Component
@ConditionalOnProperty(name = "medikit.payment.provider", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public StripePaymentGateway(RestClient.Builder builder,
                                ObjectMapper objectMapper,
                                @Value("${medikit.payment.stripe.secret-key:}") String secretKey,
                                @Value("${medikit.payment.stripe.webhook-secret:}") String webhookSecret) {
        this.client = builder
                .baseUrl("https://api.stripe.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .build();
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String name() {
        return "STRIPE";
    }

    @Override
    public GatewayOrder createOrder(CreateOrderRequest request) {
        Map<String, String> body = Map.of(
                "amount", request.amount().movePointRight(2).toBigInteger().toString(), // minor units
                "currency", request.currency(),
                "description", request.description() == null ? "Medikit order" : request.description(),
                "metadata[medikitRef]", request.merchantRefId(),
                "automatic_payment_methods[enabled]", "true");
        JsonNode resp = post("/payment_intents", body);
        String id = resp.path("id").asText();
        return new GatewayOrder(id, null, Map.of("clientSecret", resp.path("client_secret").asText()));
    }

    @Override
    public GatewayResult capture(String providerRef, BigDecimal amount, String currency) {
        JsonNode resp = post("/payment_intents/" + providerRef + "/capture", Map.of());
        boolean ok = "succeeded".equalsIgnoreCase(resp.path("status").asText());
        return new GatewayResult(ok, providerRef, resp.path("status").asText());
    }

    @Override
    public GatewayResult refund(String providerRef, BigDecimal amount, String currency) {
        Map<String, String> body = Map.of(
                "payment_intent", providerRef,
                "amount", amount.movePointRight(2).toBigInteger().toString());
        JsonNode resp = post("/refunds", body);
        boolean ok = resp.has("id");
        return new GatewayResult(ok, providerRef, resp.path("status").asText("succeeded"));
    }

    @Override
    public WebhookEvent parseWebhook(String rawBody, Map<String, String> headers) {
        String signature = headers.getOrDefault("stripe-signature", "");
        if (webhookSecret != null && !webhookSecret.isBlank() && !verifySignature(rawBody, signature)) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String eventType = node.path("type").asText();
            JsonNode intent = node.path("data").path("object");
            String providerRef = intent.path("id").asText();
            String status = intent.path("status").asText();
            BigDecimal amount = intent.path("amount").isMissingNode() ? null
                    : new BigDecimal(intent.path("amount").asLong()).movePointLeft(2);
            return new WebhookEvent(providerRef, eventType, status, amount);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed Stripe webhook payload", e);
        }
    }

    private JsonNode post(String path, Map<?, ?> form) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            StringBuilder sb = new StringBuilder();
            form.forEach((k, v) -> {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(k).append('=').append(v);
            });
            return client.post().uri(path).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(sb.toString())
                    .retrieve().body(JsonNode.class);
        } catch (Exception e) {
            log.error("[stripe] POST {} failed", path, e);
            throw new IllegalStateException("Stripe request failed: " + e.getMessage(), e);
        }
    }

    private boolean verifySignature(String body, String signature) {
        try {
            // t=timestamp,v1=hexmac
            String v1 = null;
            for (String part : signature.split(",")) {
                if (part.startsWith("v1=")) {
                    v1 = part.substring(3);
                }
            }
            if (v1 == null) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expectedHex = toHex(expected);
            return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8),
                    v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[stripe] signature verification failed", e);
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
