package com.medikit.order.service;

import com.medikit.order.client.DiscountClient;
import com.medikit.order.dto.CreateOrderRequest;
import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderStatus;
import com.medikit.order.repository.OrderRepository;
import com.medikit.order.saga.OrderSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    @Mock
    private DiscountClient discountClient;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID pharmacyId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pharmacyId = UUID.randomUUID();
    }

    @Test
    void createOrder_buildsOrderWithTotals() {
        var item = new CreateOrderRequest.OrderItemRequest(
                UUID.randomUUID(),
                "Paracetamol",
                2,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                false);
        var address = new CreateOrderRequest.AddressInfo("123 Main St", 12.9, 77.6);
        var request = new CreateOrderRequest(pharmacyId, List.of(item), address, "CARD", null, null);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.empty());
        when(sagaOrchestrator.reserveInventory(any())).thenReturn(true);
        when(sagaOrchestrator.initiatePayment(any())).thenReturn(true);

        var response = orderService.createOrder(userId, request);

        assertThat(response.orderNumber()).startsWith("MDK");
        assertThat(response.subtotal()).isEqualByComparingTo("20.00");
        assertThat(response.deliveryFee()).isEqualByComparingTo("29.00");
        assertThat(response.discount()).isEqualByComparingTo("10.00");
        assertThat(response.total()).isEqualByComparingTo("49.00");
    }

    @Test
    void createOrder_appliesDiscountCodeWhenValid() {
        var item = new CreateOrderRequest.OrderItemRequest(
                UUID.randomUUID(),
                "Paracetamol",
                2,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                false);
        var address = new CreateOrderRequest.AddressInfo("123 Main St", 12.9, 77.6);
        var request = new CreateOrderRequest(pharmacyId, List.of(item), address, "CARD", null, "MEDIKIT-ABC123");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.empty());
        when(sagaOrchestrator.reserveInventory(any())).thenReturn(true);
        when(sagaOrchestrator.initiatePayment(any())).thenReturn(true);
        when(discountClient.validate(anyMap())).thenReturn(Map.of("discountAmount", "20"));
        when(discountClient.redeem(anyMap())).thenReturn(Map.of("status", "USED"));

        var response = orderService.createOrder(userId, request);

        assertThat(response.discount()).isEqualByComparingTo("30.00");
        assertThat(response.total()).isEqualByComparingTo("29.00");
    }

    @Test
    void createOrder_rejectsInvalidDiscountCode() {
        var item = new CreateOrderRequest.OrderItemRequest(
                UUID.randomUUID(),
                "Paracetamol",
                2,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                false);
        var address = new CreateOrderRequest.AddressInfo("123 Main St", 12.9, 77.6);
        var request = new CreateOrderRequest(pharmacyId, List.of(item), address, "CARD", null, "BOGUS");

        when(discountClient.validate(anyMap())).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .hasMessageContaining("could not be validated");
    }

    @Test
    void createOrder_appliesPercentageDiscountCode() {
        var item = new CreateOrderRequest.OrderItemRequest(
                UUID.randomUUID(),
                "Paracetamol",
                2,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                false);
        var address = new CreateOrderRequest.AddressInfo("123 Main St", 12.9, 77.6);
        var request = new CreateOrderRequest(pharmacyId, List.of(item), address, "CARD", null, "PROMO-TEST1");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.empty());
        when(sagaOrchestrator.reserveInventory(any())).thenReturn(true);
        when(sagaOrchestrator.initiatePayment(any())).thenReturn(true);
        when(discountClient.validate(anyMap())).thenReturn(Map.of(
                "discountType", "PERCENTAGE",
                "percentage", "10"));
        when(discountClient.redeem(anyMap())).thenReturn(Map.of("status", "USED"));

        var response = orderService.createOrder(userId, request);

        // subtotal 20.00 * 10% = 2.00 off; total = 20 + 29 - 2 = 47.00
        assertThat(response.discount()).isEqualByComparingTo("12.00");
        assertThat(response.total()).isEqualByComparingTo("47.00");
    }

    @Test
    void createOrder_rejectsFirstOrderOnlyCodeForExistingUser() {
        var item = new CreateOrderRequest.OrderItemRequest(
                UUID.randomUUID(),
                "Paracetamol",
                2,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                false);
        var address = new CreateOrderRequest.AddressInfo("123 Main St", 12.9, 77.6);
        var request = new CreateOrderRequest(pharmacyId, List.of(item), address, "CARD", null, "WELCOME");

        when(orderRepository.existsByUserId(userId)).thenReturn(true);
        when(discountClient.validate(anyMap())).thenReturn(Map.of(
                "discountType", "FIXED",
                "discountAmount", "50",
                "firstOrderOnly", true));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .hasMessageContaining("only valid on your first order");
    }
}
