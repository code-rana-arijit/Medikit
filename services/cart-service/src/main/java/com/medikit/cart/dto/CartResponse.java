package com.medikit.cart.dto;

import com.medikit.cart.model.Cart;
import com.medikit.cart.model.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        List<CartItem> items,
        UUID pharmacyId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        int itemCount
) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getItems(),
                cart.getPharmacyId(),
                cart.getSubtotal(),
                cart.getDiscount(),
                cart.getTotal(),
                cart.getItems().size());
    }
}
