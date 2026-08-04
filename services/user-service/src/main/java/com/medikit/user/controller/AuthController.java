package com.medikit.user.controller;

import com.medikit.user.dto.AuthResponse;
import com.medikit.user.dto.LoginRequest;
import com.medikit.user.dto.OtpRequest;
import com.medikit.user.dto.RefreshTokenRequest;
import com.medikit.user.dto.RegisterRequest;
import com.medikit.user.dto.UserResponse;
import com.medikit.user.service.AuthService;
import com.medikit.user.security.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestParam String phone) {
        String otp = otpService.generate(phone);
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "otp", otp
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<UserResponse> verifyOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.verifyPhoneOtp(request.phone(), request.otp()));
    }
}
