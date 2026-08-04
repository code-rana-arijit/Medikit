package com.medikit.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "medikit.notification")
public class NotificationProperties {

    private boolean enabled = true;
    private int dedupTtlSeconds = 600;
    private String defaultRecipient = "customer@medikit.com";
}
