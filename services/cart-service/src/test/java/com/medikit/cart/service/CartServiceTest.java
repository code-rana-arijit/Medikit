package com.medikit.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.cart.model.Cart;
import com.medikit.cart.model.CartItem;
import com.medikit.common.web.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private CartService cartService;

    private UUID userId;
    private UUID pharmacyId;

    @BeforeEach
    void setUp() {
        cartService = new CartService(redisTemplate, objectMapper, 72);
        userId = UUID.randomUUID();
        pharmacyId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void addItem_createsCartAndComputesTotals() throws Exception {
        when(valueOperations.get(cartKey(userId))).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(objectMapper.writeValueAsString(any(Cart.class))).thenReturn("{}");

        CartItem item = item(UUID.randomUUID(), "Paracetamol", 2, new BigDecimal("10.00"), new BigDecimal("12.00"));

        Cart cart = cartService.addItem(userId, pharmacyId, item);

        assertThat(cart.getPharmacyId()).isEqualTo(pharmacyId);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(cart.getDiscount()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("20.00"));
        verify(valueOperations).set(eq(cartKey(userId)), eq("{}"), any(Duration.class));
    }

    @Test
    void addItem_rejectsItemFromDifferentPharmacy() throws Exception {
        UUID otherPharmacy = UUID.randomUUID();

        Cart existing = new Cart();
        existing.setUserId(userId);
        existing.setPharmacyId(pharmacyId);
        existing.setItems(new ArrayList<>(List.of(item(
                UUID.randomUUID(), "Ibuprofen", 1, new BigDecimal("8.00"), new BigDecimal("10.00")))));

        when(valueOperations.get(cartKey(userId))).thenReturn("existing-json");
        when(objectMapper.readValue("existing-json", Cart.class)).thenReturn(existing);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        CartItem newItem = item(UUID.randomUUID(), "Aspirin", 1, new BigDecimal("5.00"), new BigDecimal("7.00"));

        assertThatThrownBy(() -> cartService.addItem(userId, otherPharmacy, newItem))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart can only contain items from one pharmacy at a time");
    }

    @Test
    void addItem_mergesQuantityWhenSameProductExists() throws Exception {
        UUID productId = UUID.randomUUID();

        Cart existing = new Cart();
        existing.setUserId(userId);
        existing.setPharmacyId(pharmacyId);
        existing.setItems(new ArrayList<>(List.of(item(
                productId, "Paracetamol", 2, new BigDecimal("10.00"), new BigDecimal("12.00")))));

        when(valueOperations.get(cartKey(userId))).thenReturn("existing-json");
        when(objectMapper.readValue("existing-json", Cart.class)).thenReturn(existing);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(objectMapper.writeValueAsString(any(Cart.class))).thenReturn("{}");

        Cart cart = cartService.addItem(userId, pharmacyId,
                item(productId, "Paracetamol", 3, new BigDecimal("10.00"), new BigDecimal("12.00")));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void updateQuantity_zeroRemovesItem() throws Exception {
        UUID productId = UUID.randomUUID();

        Cart existing = new Cart();
        existing.setUserId(userId);
        existing.setPharmacyId(pharmacyId);
        existing.setItems(new ArrayList<>(List.of(item(
                productId, "Paracetamol", 5, new BigDecimal("10.00"), new BigDecimal("12.00")))));

        when(valueOperations.get(cartKey(userId))).thenReturn("existing-json");
        when(objectMapper.readValue("existing-json", Cart.class)).thenReturn(existing);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(objectMapper.writeValueAsString(any(Cart.class))).thenReturn("{}");

        Cart cart = cartService.updateQuantity(userId, productId, 0);

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getPharmacyId()).isNull();
        assertThat(cart.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cart.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cart.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private String cartKey(UUID id) {
        return "medikit:cart:" + id;
    }

    private CartItem item(UUID productId, String name, int quantity, BigDecimal unitPrice, BigDecimal mrp) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setProductName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setMrp(mrp);
        return item;
    }
}
