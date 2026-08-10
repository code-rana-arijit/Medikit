package com.medikit.distributor.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.distributor.client.OrderClient;
import com.medikit.distributor.dto.FulfillmentResponse;
import com.medikit.distributor.entity.DistributorProfile;
import com.medikit.distributor.entity.FulfillmentStatus;
import com.medikit.distributor.entity.RetailFulfillment;
import com.medikit.distributor.repository.RetailFulfillmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    @Mock
    private RetailFulfillmentRepository fulfillmentRepository;

    @Mock
    private DistributorService distributorService;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private FulfillmentService fulfillmentService;

    private UUID distributorUserId;
    private UUID distributorId;
    private DistributorProfile distributor;

    @BeforeEach
    void setUp() {
        distributorUserId = UUID.randomUUID();
        distributorId = UUID.randomUUID();
        distributor = DistributorProfile.builder()
                .id(distributorId).userId(distributorUserId).shopName("Medi Wholesale").active(true).build();
    }

    @Test
    void claim_createsFulfillment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(orderClient.getOrder(orderId)).thenReturn(Map.of("userId", customerId.toString()));
        when(fulfillmentRepository.save(any(RetailFulfillment.class))).thenAnswer(i -> i.getArgument(0));

        FulfillmentResponse response = fulfillmentService.claim(distributorUserId, orderId);

        assertThat(response.status()).isEqualTo(FulfillmentStatus.CLAIMED);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.customerUserId()).isEqualTo(customerId);
        assertThat(response.distributorId()).isEqualTo(distributorId);
    }

    @Test
    void claim_throwsWhenAlreadyClaimed() {
        UUID orderId = UUID.randomUUID();
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.existsByOrderId(orderId)).thenReturn(true);

        assertThatThrownBy(() -> fulfillmentService.claim(distributorUserId, orderId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void claim_throwsWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(orderClient.getOrder(orderId)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> fulfillmentService.claim(distributorUserId, orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void claim_rejectsSelfOrder() {
        UUID orderId = UUID.randomUUID();
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(orderClient.getOrder(orderId)).thenReturn(Map.of("userId", distributorUserId.toString()));

        assertThatThrownBy(() -> fulfillmentService.claim(distributorUserId, orderId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_transitionsThroughLifecycle() {
        UUID fulfillmentId = UUID.randomUUID();
        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .id(fulfillmentId).orderId(UUID.randomUUID()).customerUserId(UUID.randomUUID())
                .distributorId(distributorId).status(FulfillmentStatus.CLAIMED).build();
        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.save(any(RetailFulfillment.class))).thenAnswer(i -> i.getArgument(0));

        FulfillmentResponse response = fulfillmentService.updateStatus(
                distributorUserId, fulfillmentId, FulfillmentStatus.PICKED_UP);

        assertThat(response.status()).isEqualTo(FulfillmentStatus.PICKED_UP);
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        UUID fulfillmentId = UUID.randomUUID();
        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .id(fulfillmentId).orderId(UUID.randomUUID()).customerUserId(UUID.randomUUID())
                .distributorId(distributorId).status(FulfillmentStatus.CLAIMED).build();
        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);

        assertThatThrownBy(() -> fulfillmentService.updateStatus(
                distributorUserId, fulfillmentId, FulfillmentStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_marksDeliveredAtOnDeliver() {
        UUID fulfillmentId = UUID.randomUUID();
        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .id(fulfillmentId).orderId(UUID.randomUUID()).customerUserId(UUID.randomUUID())
                .distributorId(distributorId).status(FulfillmentStatus.IN_TRANSIT).build();
        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.save(any(RetailFulfillment.class))).thenAnswer(i -> i.getArgument(0));

        FulfillmentResponse response = fulfillmentService.updateStatus(
                distributorUserId, fulfillmentId, FulfillmentStatus.DELIVERED);

        assertThat(response.status()).isEqualTo(FulfillmentStatus.DELIVERED);
        assertThat(response.deliveredAt()).isNotNull();
    }

    @Test
    void updateStatus_hidesOtherDistributorsFulfillment() {
        UUID fulfillmentId = UUID.randomUUID();
        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .id(fulfillmentId).orderId(UUID.randomUUID()).customerUserId(UUID.randomUUID())
                .distributorId(UUID.randomUUID()).status(FulfillmentStatus.CLAIMED).build();
        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);

        assertThatThrownBy(() -> fulfillmentService.updateStatus(
                distributorUserId, fulfillmentId, FulfillmentStatus.PICKED_UP))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void myFulfillments_listsOwn() {
        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .id(UUID.randomUUID()).orderId(UUID.randomUUID()).customerUserId(UUID.randomUUID())
                .distributorId(distributorId).status(FulfillmentStatus.CLAIMED).build();
        when(distributorService.findForUser(distributorUserId)).thenReturn(distributor);
        when(fulfillmentRepository.findByDistributorIdOrderByCreatedAtDesc(distributorId))
                .thenReturn(List.of(fulfillment));

        List<FulfillmentResponse> result = fulfillmentService.myFulfillments(distributorUserId);

        assertThat(result).hasSize(1);
        verify(fulfillmentRepository).findByDistributorIdOrderByCreatedAtDesc(distributorId);
    }
}
