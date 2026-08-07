package com.medikit.notification.service.provider;

import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationType;
import com.medikit.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "medikit.notification.mock", havingValue = "true", matchIfMissing = true)
public class PushProvider implements NotificationService {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("MOCK PUSH provider -> device={}, subject={}, body={}",
                notification.getRecipient(), notification.getSubject(), notification.getBody());
        return true;
    }
}
