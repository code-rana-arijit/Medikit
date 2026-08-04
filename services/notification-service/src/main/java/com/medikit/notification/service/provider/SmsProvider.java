package com.medikit.notification.service.provider;

import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationType;
import com.medikit.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsProvider implements NotificationService {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("MOCK SMS provider -> to={}, body={}",
                notification.getRecipient(), notification.getBody());
        return true;
    }
}
