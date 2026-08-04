package com.medikit.common.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private int status;
    private String code;
    private String message;
    private Instant timestamp;
    private Map<String, Object> details;

    public static ApiError of(int status, String code, String message, Map<String, Object> details) {
        return new ApiError(status, code, message, Instant.now(), details);
    }

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, Instant.now(), null);
    }
}
