package com.medikit.prescription.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * S3-compatible object storage for prescription images.
 * <p>
 * Works with AWS S3, MinIO, or any S3-compatible endpoint via
 * {@code medikit.storage.s3.endpoint}. Keys are UUID-named and stored in
 * a per-environment bucket with a privacy-first prefix.
 * </p>
 */
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(S3Client s3Client, String bucket, String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String store(MultipartFile file) {
        String key = "prescriptions/" + UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Stored prescription {} in bucket {} ({} bytes)", key, bucket, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload for " + key, e);
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
        }
        return "s3://" + bucket + "/" + key;
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return ".bin";
        }
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot).toLowerCase() : ".bin";
        // restrict to known image extensions
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".heic" -> ext;
            default -> ".bin";
        };
    }
}
