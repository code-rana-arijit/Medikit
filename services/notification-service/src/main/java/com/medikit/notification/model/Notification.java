package com.medikit.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String id;
    private NotificationType type;
    private String channel;
    private String recipient;
    private String subject;
    private String body;
    private NotificationStatus status;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
