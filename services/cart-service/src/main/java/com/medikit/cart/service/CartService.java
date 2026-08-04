package com.medikit.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.cart.model.Cart;
import com.medikit.cart.model.CartItem;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "medikit:cart:";
    private static final String LOCK_KEY_PREFIX = "medikit:cart:lock:";
    private static final long LOCK_TTL_SECONDS = 5;
    private static final int LOCK_RETRY_ATTEMPTS = 10;
    private static final long LOCK_RETRY_DELAY_MS = 50;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlHours;

    public CartService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${medikit.cart.ttl-hours:72}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttlHours = ttlHours;
    }

    public Cart getCart(UUID userId) {
        String json = redisTemplate.opsForValue().get(cartKey(userId));
        if (json == null) {
            return emptyCart(userId);
        }
        return deserialize(json);
    }

    public Cart addItem(UUID userId, UUID pharmacyId, CartItem item) {
        return withLock(userId, () -> {
            Cart cart = getCart(userId);
            if (cart.getPharmacyId() != null && !cart.getPharmacyId().equals(pharmacyId)) {
                throw new BadRequestException("Cart can only contain items from one pharmacy at a time");
            }
            cart.setPharmacyId(pharmacyId);
            cart.getItems().stream()
                    .filter(existing -> existing.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.setQuantity(existing.getQuantity() + item.getQuantity()),
                            () -> cart.getItems().add(item));
            recompute(cart);
            cart.setUpdatedAt(Instant.now());
            save(userId, cart);
            return cart;
        });
    }

    public Cart updateQuantity(UUID userId, UUID productId, int newQuantity) {
        return withLock(userId, () -> {
            Cart cart = getCart(userId);
            CartItem item = cart.getItems().stream()
                    .filter(existing -> existing.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Item not found in cart"));
            if (newQuantity <= 0) {
                cart.getItems().remove(item);
            } else {
                item.setQuantity(newQuantity);
            }
            recompute(cart);
            if (cart.getItems().isEmpty()) {
                cart.setPharmacyId(null);
            }
            cart.setUpdatedAt(Instant.now());
            save(userId, cart);
            return cart;
        });
    }

    public Cart removeItem(UUID userId, UUID productId) {
        return withLock(userId, () -> {
            Cart cart = getCart(userId);
            boolean removed = cart.getItems().removeIf(existing -> existing.getProductId().equals(productId));
            if (!removed) {
                throw new NotFoundException("Item not found in cart");
            }
            recompute(cart);
            if (cart.getItems().isEmpty()) {
                cart.setPharmacyId(null);
            }
            cart.setUpdatedAt(Instant.now());
            save(userId, cart);
            return cart;
        });
    }

    public void clearCart(UUID userId) {
        withLock(userId, () -> {
            redisTemplate.delete(cartKey(userId));
            return null;
        });
    }

    private void save(UUID userId, Cart cart) {
        try {
            redisTemplate.opsForValue().set(
                    cartKey(userId),
                    objectMapper.writeValueAsString(cart),
                    Duration.ofHours(ttlHours));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cart", e);
        }
    }

    private Cart deserialize(String json) {
        try {
            return objectMapper.readValue(json, Cart.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cart", e);
        }
    }

    private void recompute(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = cart.getItems().stream()
                .map(item -> item.getMrp()
                        .subtract(item.getUnitPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cart.setDiscount(discount);
        cart.setTotal(subtotal);
    }

    private Cart emptyCart(UUID userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setDiscount(BigDecimal.ZERO);
        cart.setTotal(BigDecimal.ZERO);
        cart.setUpdatedAt(Instant.now());
        return cart;
    }

    private <T> T withLock(UUID userId, Supplier<T> action) {
        String lockKey = lockKey(userId);
        String token = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            for (int attempt = 0; attempt < LOCK_RETRY_ATTEMPTS; attempt++) {
                Boolean locked = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, token, Duration.ofSeconds(LOCK_TTL_SECONDS));
                acquired = Boolean.TRUE.equals(locked);
                if (acquired) {
                    break;
                }
                if (attempt < LOCK_RETRY_ATTEMPTS - 1) {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                }
            }
            if (!acquired) {
                throw new ConflictException("Could not acquire cart lock for user " + userId);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring cart lock", e);
        } finally {
            if (acquired) {
                String current = redisTemplate.opsForValue().get(lockKey);
                if (token.equals(current)) {
                    redisTemplate.delete(lockKey);
                }
            }
        }
    }

    private String cartKey(UUID userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String lockKey(UUID userId) {
        return LOCK_KEY_PREFIX + userId;
    }
}
