package com.medikit.distributor.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.distributor.dto.DistributorOrderItemRequest;
import com.medikit.distributor.dto.DistributorOrderRequest;
import com.medikit.distributor.dto.DistributorOrderResponse;
import com.medikit.distributor.entity.CatalogItem;
import com.medikit.distributor.entity.DistributorOrder;
import com.medikit.distributor.entity.DistributorOrderItem;
import com.medikit.distributor.entity.DistributorOrderStatus;
import com.medikit.distributor.entity.DistributorProfile;
import com.medikit.distributor.repository.CatalogItemRepository;
import com.medikit.distributor.repository.DistributorOrderRepository;
import com.medikit.distributor.repository.DistributorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributorOrderServiceTest {

    @Mock
    private DistributorOrderRepository orderRepository;

    @Mock
    private DistributorProfileRepository profileRepository;

    @Mock
    private CatalogItemRepository catalogRepository;

    @Mock
    private DistributorService distributorService;

    @InjectMocks
    private DistributorOrderService orderService;

    private UUID buyerId;
    private UUID distributorId;
    private UUID productId;
    private DistributorProfile distributor;
    private CatalogItem catalog;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        distributorId = UUID.randomUUID();
        productId = UUID.randomUUID();
        distributor = DistributorProfile.builder()
                .id(distributorId)
                .userId(UUID.randomUUID())
                .shopName("Medi Wholesale")
                .active(true)
                .build();
        catalog = CatalogItem.builder()
                .distributorId(distributorId)
                .productId(productId)
                .productName("Paracetamol 500mg")
                .unitPrice(new BigDecimal("12.50"))
                .packSize(20)
                .stockQty(100)
                .build();
    }

    @Test
    void placeOrder_createsOrderAndDeductsStock() {
        when(profileRepository.findById(distributorId)).thenReturn(Optional.of(distributor));
        when(catalogRepository.findByDistributorIdAndProductId(distributorId, productId))
                .thenReturn(Optional.of(catalog));
        when(orderRepository.save(any(DistributorOrder.class))).thenAnswer(i -> i.getArgument(0));

        DistributorOrderResponse response = orderService.placeOrder(buyerId,
                new DistributorOrderRequest(distributorId,
                        List.of(new DistributorOrderItemRequest(productId, 10))));

        assertThat(response.status()).isEqualTo(DistributorOrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("125.00");
        assertThat(response.orderNumber()).startsWith("DB2B-");
        assertThat(response.items()).hasSize(1);
        verify(distributorService).adjustStock(distributorId, productId, -10);
    }

    @Test
    void placeOrder_rejectsOwnDistributor() {
        DistributorProfile own = DistributorProfile.builder()
                .id(distributorId)
                .userId(buyerId)
                .active(true)
                .build();
        when(profileRepository.findById(distributorId)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> orderService.placeOrder(buyerId,
                new DistributorOrderRequest(distributorId,
                        List.of(new DistributorOrderItemRequest(productId, 1)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void placeOrder_rejectsInactiveDistributor() {
        DistributorProfile inactive = DistributorProfile.builder()
                .id(distributorId).userId(UUID.randomUUID()).active(false).build();
        when(profileRepository.findById(distributorId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.placeOrder(buyerId,
                new DistributorOrderRequest(distributorId,
                        List.of(new DistributorOrderItemRequest(productId, 1)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void placeOrder_rejectsInsufficientStock() {
        CatalogItem lowStock = CatalogItem.builder()
                .distributorId(distributorId).productId(productId).productName("P")
                .unitPrice(new BigDecimal("5.00")).packSize(1).stockQty(2).build();
        when(profileRepository.findById(distributorId)).thenReturn(Optional.of(distributor));
        when(catalogRepository.findByDistributorIdAndProductId(distributorId, productId))
                .thenReturn(Optional.of(lowStock));

        assertThatThrownBy(() -> orderService.placeOrder(buyerId,
                new DistributorOrderRequest(distributorId,
                        List.of(new DistributorOrderItemRequest(productId, 10)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_confirmedByDistributor() {
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.PENDING).totalAmount(new BigDecimal("50.00")).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(profileRepository.findByUserId(distributor.getUserId()))
                .thenReturn(Optional.of(distributor));
        when(orderRepository.save(any(DistributorOrder.class))).thenAnswer(i -> i.getArgument(0));

        DistributorOrderResponse response = orderService.updateStatus(
                distributor.getUserId(), order.getId(), DistributorOrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(DistributorOrderStatus.CONFIRMED);
    }

    @Test
    void updateStatus_rejectsBuyerConfirming() {
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.PENDING).totalAmount(new BigDecimal("50.00")).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(profileRepository.findByUserId(buyerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(buyerId, order.getId(), DistributorOrderStatus.CONFIRMED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.CONFIRMED).totalAmount(new BigDecimal("50.00")).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(profileRepository.findByUserId(distributor.getUserId()))
                .thenReturn(Optional.of(distributor));

        assertThatThrownBy(() -> orderService.updateStatus(
                distributor.getUserId(), order.getId(), DistributorOrderStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_cancelRestoresStock() {
        UUID itemId = UUID.randomUUID();
        DistributorOrderItem item = DistributorOrderItem.builder()
                .id(itemId).productId(productId).productName("P").quantity(4)
                .unitPrice(new BigDecimal("5.00")).subtotal(new BigDecimal("20.00")).build();
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.PENDING).totalAmount(new BigDecimal("20.00")).build();
        order.addItem(item);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(profileRepository.findByUserId(buyerId)).thenReturn(Optional.empty());
        when(orderRepository.save(any(DistributorOrder.class))).thenAnswer(i -> i.getArgument(0));

        DistributorOrderResponse response = orderService.updateStatus(
                buyerId, order.getId(), DistributorOrderStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(DistributorOrderStatus.CANCELLED);
        verify(distributorService).adjustStock(distributorId, productId, 4);
    }

    @Test
    void updateStatus_rejectsCancelAfterDelivered() {
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.DELIVERED).totalAmount(new BigDecimal("50.00")).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(buyerId, order.getId(), DistributorOrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class);
        verify(distributorService, never()).adjustStock(any(), any(), anyInt());
    }

    @Test
    void getOrder_hidesFromUnauthorizedUser() {
        DistributorOrder order = DistributorOrder.builder()
                .id(UUID.randomUUID()).buyerUserId(buyerId).distributorId(distributorId)
                .status(DistributorOrderStatus.PENDING).totalAmount(new BigDecimal("50.00")).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(profileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(UUID.randomUUID(), order.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
