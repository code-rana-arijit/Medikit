package com.medikit.prescription.config;

import com.medikit.prescription.service.LocalStorageService;
import com.medikit.prescription.service.MockStorageService;
import com.medikit.prescription.service.S3StorageService;
import com.medikit.prescription.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

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

    @Bean
    @ConditionalOnProperty(name = "medikit.storage.type", havingValue = "S3")
    public StorageService s3StorageService(S3Client s3Client,
                                           @Value("${medikit.storage.s3.bucket}") String bucket,
                                           @Value("${medikit.storage.s3.public-base-url:}") String publicBaseUrl) {
        return new S3StorageService(s3Client, bucket, publicBaseUrl);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "medikit.storage.type", havingValue = "S3")
    public S3Client s3Client(@Value("${medikit.storage.s3.endpoint:}") String endpoint,
                             @Value("${medikit.storage.s3.region:us-east-1}") String region,
                             @Value("${medikit.storage.s3.access-key:}") String accessKey,
                             @Value("${medikit.storage.s3.secret-key:}") String secretKey) {
        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
