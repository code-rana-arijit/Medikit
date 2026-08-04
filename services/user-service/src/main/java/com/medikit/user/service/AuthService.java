package com.medikit.user.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.user.dto.LoginRequest;
import com.medikit.user.dto.RefreshTokenRequest;
import com.medikit.user.dto.RegisterRequest;
import com.medikit.user.entity.User;
import com.medikit.user.entity.UserRole;
import com.medikit.user.repository.UserRepository;
import com.medikit.user.security.JwtService;
import com.medikit.user.security.OtpService;
import com.medikit.user.dto.UserResponse;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "medikit:refresh:";
    private static final String LOGIN_FAIL_PREFIX = "medikit:login:fail:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OtpService otpService,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone number already registered");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public com.medikit.user.dto.AuthResponse login(LoginRequest request) {
        String failKey = LOGIN_FAIL_PREFIX + request.identifier();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= 10) {
            throw new BadRequestException("Too many failed login attempts. Try again later.");
        }

        User user = userRepository.findByEmail(request.identifier().toLowerCase())
                .or(() -> userRepository.findByPhone(request.identifier()))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, Duration.ofMinutes(15));
            throw new BadRequestException("Invalid credentials");
        }

        redisTemplate.delete(failKey);
        return issueTokens(user);
    }

    public com.medikit.user.dto.AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (Exception e) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Not a refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);
        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (stored == null || !stored.equals(request.refreshToken())) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public UserResponse verifyPhoneOtp(String phone, String otp) {
        if (!otpService.verify(phone, otp)) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPhoneVerified(true);
        return toResponse(userRepository.save(user));
    }

    public void logout(UUID userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    private com.medikit.user.dto.AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                Duration.ofSeconds(604800));

        return com.medikit.user.dto.AuthResponse.of(
                accessToken,
                refreshToken,
                900,
                toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getCreatedAt());
    }
}
