package com.medikit.notification.service;

import com.medikit.notification.config.NotificationProperties;
import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationStatus;
import com.medikit.notification.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {

    @Mock
    private NotificationService emailProvider;

    @Mock
    private NotificationService smsProvider;

    @Mock
    private NotificationProperties properties;

    private NotificationSender notificationSender;

    @BeforeEach
    void setUp() {
        notificationSender = new NotificationSender(List.of(emailProvider, smsProvider), properties);
    }

    private Notification notification(NotificationType type) {
        return Notification.builder()
                .id("n-1")
                .type(type)
                .channel(type.name().toLowerCase())
                .recipient("customer@medikit.com")
                .subject("Test subject")
                .body("Test body")
                .metadata(Map.of())
                .build();
    }

    @Test
    void send_dispatchesToMatchingProviderAndMarksSent() {
        when(properties.isEnabled()).thenReturn(true);
        when(emailProvider.supports(NotificationType.EMAIL)).thenReturn(true);
        when(emailProvider.send(any(Notification.class))).thenReturn(true);

        Notification result = notificationSender.send(notification(NotificationType.EMAIL));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailProvider).send(any(Notification.class));
        verify(smsProvider, never()).send(any(Notification.class));
    }

    @Test
    void send_marksFailedWhenProviderReturnsFalse() {
        when(properties.isEnabled()).thenReturn(true);
        when(emailProvider.supports(NotificationType.EMAIL)).thenReturn(true);
        when(emailProvider.send(any(Notification.class))).thenReturn(false);

        Notification result = notificationSender.send(notification(NotificationType.EMAIL));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailProvider).send(any(Notification.class));
    }

    @Test
    void send_logsOnlyWhenNotificationsDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Notification result = notificationSender.send(notification(NotificationType.EMAIL));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailProvider, never()).send(any(Notification.class));
        verify(smsProvider, never()).send(any(Notification.class));
    }
}
