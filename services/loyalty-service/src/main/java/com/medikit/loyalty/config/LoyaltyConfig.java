package com.medikit.loyalty.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoyaltyProperties.class)
public class LoyaltyConfig {
}
