package com.medikit.notification.service.provider;

import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationType;
import com.medikit.notification.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email delivery via SMTP (production wiring uses Amazon SES SMTP endpoint).
 * <p>
 * Enabled via {@code medikit.notification.ses.enabled=true}. Requires
 * {@code spring.mail.*} configuration pointing at the SES SMTP endpoint.
 * </p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "medikit.notification.ses.enabled", havingValue = "true")
public class SesEmailProvider implements NotificationService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SesEmailProvider(JavaMailSender mailSender,
                            @Value("${medikit.notification.ses.from-address:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), notification.getBody().contains("<"));
            mailSender.send(message);
            log.info("SES email sent to {}", notification.getRecipient());
            return true;
        } catch (Exception e) {
            log.error("SES email delivery failed to {}", notification.getRecipient(), e);
            return false;
        }
    }
}
