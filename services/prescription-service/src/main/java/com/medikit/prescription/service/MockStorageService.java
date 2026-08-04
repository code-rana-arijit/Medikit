package com.medikit.prescription.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class MockStorageService implements StorageService {

    @Override
    public String store(MultipartFile file) {
        return "https://mock.storage.medikit.local/prescriptions/" + UUID.randomUUID() + ".jpg";
    }
}
