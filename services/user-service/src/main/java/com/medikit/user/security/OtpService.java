package com.medikit.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    private static final String OTP_KEY_PREFIX = "medikit:otp:";
    private static final String ATTEMPT_KEY_PREFIX = "medikit:otp:attempts:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final int maxAttempts;

    public OtpService(
            StringRedisTemplate redisTemplate,
            @Value("${medikit.otp.ttl-seconds:300}") long ttlSeconds,
            @Value("${medikit.otp.max-attempts:5}") int maxAttempts) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
        this.maxAttempts = maxAttempts;
    }

    public String generate(String phone) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(otpKey(phone), otp, Duration.ofSeconds(ttlSeconds));
        redisTemplate.delete(attemptsKey(phone));
        return otp;
    }

    public boolean verify(String phone, String otp) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey(phone));
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey(phone), Duration.ofSeconds(ttlSeconds));
        }
        if (attempts != null && attempts > maxAttempts) {
            redisTemplate.delete(otpKey(phone));
            return false;
        }
        String stored = redisTemplate.opsForValue().get(otpKey(phone));
        if (stored != null && stored.equals(otp)) {
            redisTemplate.delete(otpKey(phone));
            redisTemplate.delete(attemptsKey(phone));
            return true;
        }
        return false;
    }

    public boolean hasTooManyAttempts(String phone) {
        String attempts = redisTemplate.opsForValue().get(attemptsKey(phone));
        return attempts != null && Integer.parseInt(attempts) > maxAttempts;
    }

    private String otpKey(String phone) {
        return OTP_KEY_PREFIX + phone;
    }

    private String attemptsKey(String phone) {
        return ATTEMPT_KEY_PREFIX + phone;
    }
}
