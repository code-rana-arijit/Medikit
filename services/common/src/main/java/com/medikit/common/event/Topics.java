package com.medikit.common.event;

public final class Topics {

    private Topics() {
    }

    public static final String ORDER_CREATED = "medikit.order.created";
    public static final String ORDER_CONFIRMED = "medikit.order.confirmed";
    public static final String ORDER_CANCELLED = "medikit.order.cancelled";
    public static final String ORDER_FAILED = "medikit.order.failed";
    public static final String ORDER_COMPLETED = "medikit.order.completed";

    public static final String PAYMENT_INITIATED = "medikit.payment.initiated";
    public static final String PAYMENT_COMPLETED = "medikit.payment.completed";
    public static final String PAYMENT_FAILED = "medikit.payment.failed";
    public static final String PAYMENT_REFUNDED = "medikit.payment.refunded";

    public static final String INVENTORY_RESERVED = "medikit.inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "medikit.inventory.reservation.failed";
    public static final String INVENTORY_RELEASED = "medikit.inventory.released";
    public static final String INVENTORY_DEDUCTED = "medikit.inventory.deducted";

    public static final String DELIVERY_ASSIGNED = "medikit.delivery.assigned";
    public static final String DELIVERY_UPDATED = "medikit.delivery.updated";

    public static final String NOTIFICATION_SEND = "medikit.notification.send";

    public static final String PRODUCT_UPDATED = "medikit.product.updated";

    public static final String STOCK_UPDATED = "medikit.stock.updated";

    public static final String AUDIT_EVENTS = "medikit.audit.events";
}
