package com.medikit.notification.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationType;
import com.medikit.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Twilio SMS provider.
 * <p>
 * Enabled via {@code medikit.notification.twilio.enabled=true}.
 * </p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "medikit.notification.twilio.enabled", havingValue = "true")
public class TwilioSmsProvider implements NotificationService {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String fromNumber;

    public TwilioSmsProvider(ObjectMapper objectMapper,
                             @Value("${medikit.notification.twilio.account-sid:}") String accountSid,
                             @Value("${medikit.notification.twilio.auth-token:}") String authToken,
                             @Value("${medikit.notification.twilio.from-number:}") String fromNumber) {
        String creds = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        this.client = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + creds)
                .build();
        this.objectMapper = objectMapper;
        this.fromNumber = fromNumber;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        String body = "Body=" + encode(notification.getBody())
                + "&From=" + encode(fromNumber)
                + "&To=" + encode(notification.getRecipient());
        try {
            JsonNode resp = client.post()
                    .uri("/Messages.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String status = resp != null ? resp.path("status").asText() : "";
            log.info("Twilio SMS to {} status={}", notification.getRecipient(), status);
            return !"failed".equalsIgnoreCase(status) && resp != null && resp.has("sid");
        } catch (Exception e) {
            log.error("Twilio SMS delivery failed to {}", notification.getRecipient(), e);
            return false;
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
