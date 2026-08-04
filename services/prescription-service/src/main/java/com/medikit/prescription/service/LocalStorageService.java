package com.medikit.prescription.service;

import com.medikit.common.web.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class LocalStorageService implements StorageService {

    private final Path storagePath;

    public LocalStorageService(String localPath) {
        this.storagePath = Path.of(localPath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(storagePath);
            String filename = UUID.randomUUID() + "-"
                    + Objects.requireNonNullElse(file.getOriginalFilename(), "prescription.jpg");
            Path target = storagePath.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store prescription image");
        }
    }
}
