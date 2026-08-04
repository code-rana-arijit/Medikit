package com.medikit.cart.controller;

import com.medikit.cart.dto.AddItemRequest;
import com.medikit.cart.dto.CartResponse;
import com.medikit.cart.dto.UpdateQuantityRequest;
import com.medikit.cart.model.Cart;
import com.medikit.cart.model.CartItem;
import com.medikit.cart.service.CartService;
import com.medikit.common.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCartByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(CartResponse.from(cartService.getCart(userId)));
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(CartResponse.from(cartService.getCart(currentUserId())));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddItemRequest request) {
        Cart cart = cartService.addItem(currentUserId(), request.pharmacyId(), toItem(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        Cart cart = cartService.updateQuantity(currentUserId(), productId, request.quantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable UUID productId) {
        Cart cart = cartService.removeItem(currentUserId(), productId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart() {
        cartService.clearCart(currentUserId());
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }

    private CartItem toItem(AddItemRequest request) {
        CartItem item = new CartItem();
        item.setProductId(request.productId());
        item.setProductName(request.productName());
        item.setUnitPrice(request.unitPrice());
        item.setMrp(request.mrp());
        item.setQuantity(request.quantity());
        item.setImageUrl(request.imageUrl());
        item.setPrescriptionRequired(request.prescriptionRequired());
        return item;
    }

    private UUID currentUserId() {
        return UUID.fromString(UserContext.currentUserId());
    }
}
