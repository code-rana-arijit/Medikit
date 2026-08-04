package com.medikit.prescription.config;

import com.medikit.prescription.service.LocalStorageService;
import com.medikit.prescription.service.MockStorageService;
import com.medikit.prescription.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "medikit.storage.type", havingValue = "LOCAL", matchIfMissing = true)
    public StorageService localStorageService(
            @Value("${medikit.storage.local-path:./data/uploads}") String localPath) {
        return new LocalStorageService(localPath);
    }

    @Bean
    @ConditionalOnProperty(name = "medikit.storage.type", havingValue = "MOCK")
    public StorageService mockStorageService() {
        return new MockStorageService();
    }
}
