package com.medikit.order.entity;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    FAILED
}
