package com.medikit.prescription.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file);
}
