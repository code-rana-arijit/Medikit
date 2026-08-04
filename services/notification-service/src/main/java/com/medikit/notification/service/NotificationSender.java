package com.medikit.notification.service;

import com.medikit.notification.config.NotificationProperties;
import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationStatus;
import com.medikit.notification.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationSender {

    private final List<NotificationService> providers;
    private final NotificationProperties properties;

    public NotificationSender(List<NotificationService> providers, NotificationProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    public Notification send(NotificationType type, String channel, String recipient, String subject,
                             String body, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.SENT)
                .timestamp(Instant.now())
                .metadata(metadata)
                .build();
        return send(notification);
    }

    public Notification send(Notification notification) {
        if (!properties.isEnabled()) {
            log.warn("Notification delivery disabled - would send {} to [{}]",
                    notification.getType(), notification.getRecipient());
            notification.setStatus(NotificationStatus.SENT);
            return notification;
        }

        NotificationService provider = providers.stream()
                .filter(p -> p.supports(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No provider registered for notification type " + notification.getType()));

        try {
            boolean delivered = provider.send(notification);
            notification.setStatus(delivered ? NotificationStatus.SENT : NotificationStatus.FAILED);
        } catch (Exception e) {
            log.error("Failed to send {} notification to [{}]",
                    notification.getType(), notification.getRecipient(), e);
            notification.setStatus(NotificationStatus.FAILED);
        }
        return notification;
    }
}
