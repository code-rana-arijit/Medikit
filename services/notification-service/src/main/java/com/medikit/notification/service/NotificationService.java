package com.medikit.notification.service;

import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationType;

public interface NotificationService {

    boolean supports(NotificationType type);

    boolean send(Notification notification);
}
