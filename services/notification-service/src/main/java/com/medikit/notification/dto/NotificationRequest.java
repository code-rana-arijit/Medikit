package com.medikit.notification.dto;

import com.medikit.notification.model.NotificationType;

public record NotificationRequest(
        NotificationType type,
        String recipient,
        String subject,
        String body
) {
}
