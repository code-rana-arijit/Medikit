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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FulfillmentService {

    private final RetailFulfillmentRepository fulfillmentRepository;
    private final DistributorService distributorService;
    private final OrderClient orderClient;

    public FulfillmentService(RetailFulfillmentRepository fulfillmentRepository,
                              DistributorService distributorService,
                              OrderClient orderClient) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.distributorService = distributorService;
        this.orderClient = orderClient;
    }

    @Transactional
    public FulfillmentResponse claim(UUID userId, UUID orderId) {
        DistributorProfile distributor = distributorService.findForUser(userId);
        if (!distributor.isActive()) {
            throw new BadRequestException("Distributor is not active");
        }
        if (fulfillmentRepository.existsByOrderId(orderId)) {
            throw new ConflictException("Order is already claimed for fulfillment");
        }

        Map<String, Object> order = fetchOrder(orderId);
        Object rawUserId = order.get("userId");
        UUID customerUserId;
        if (rawUserId instanceof String s) {
            customerUserId = UUID.fromString(s);
        } else if (rawUserId instanceof Map<?, ?> m) {
            customerUserId = UUID.fromString(String.valueOf(m.get("id")));
        } else {
            customerUserId = UUID.fromString(String.valueOf(rawUserId));
        }
        if (customerUserId.equals(userId)) {
            throw new BadRequestException("Cannot fulfill your own retail order");
        }

        RetailFulfillment fulfillment = RetailFulfillment.builder()
                .orderId(orderId)
                .customerUserId(customerUserId)
                .distributorId(distributor.getId())
                .build();
        return toResponse(fulfillmentRepository.save(fulfillment));
    }

    @Transactional
    public FulfillmentResponse updateStatus(UUID userId, UUID fulfillmentId, FulfillmentStatus target) {
        RetailFulfillment fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new NotFoundException("Fulfillment not found"));
        DistributorProfile distributor = distributorService.findForUser(userId);
        if (!distributor.getId().equals(fulfillment.getDistributorId())) {
            throw new NotFoundException("Fulfillment not found");
        }
        if (!canTransition(fulfillment.getStatus(), target)) {
            throw new BadRequestException("Invalid fulfillment transition from " + fulfillment.getStatus() + " to " + target);
        }
        fulfillment.setStatus(target);
        if (target == FulfillmentStatus.DELIVERED) {
            fulfillment.setDeliveredAt(Instant.now());
        }
        return toResponse(fulfillmentRepository.save(fulfillment));
    }

    public List<FulfillmentResponse> myFulfillments(UUID userId) {
        DistributorProfile distributor = distributorService.findForUser(userId);
        return fulfillmentRepository.findByDistributorIdOrderByCreatedAtDesc(distributor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FulfillmentResponse getFulfillment(UUID userId, UUID fulfillmentId) {
        RetailFulfillment fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new NotFoundException("Fulfillment not found"));
        DistributorProfile distributor = distributorService.findForUser(userId);
        if (!distributor.getId().equals(fulfillment.getDistributorId())) {
            throw new NotFoundException("Fulfillment not found");
        }
        return toResponse(fulfillment);
    }

    private Map<String, Object> fetchOrder(UUID orderId) {
        try {
            return orderClient.getOrder(orderId);
        } catch (Exception e) {
            throw new NotFoundException("Retail order not found");
        }
    }

    private boolean canTransition(FulfillmentStatus from, FulfillmentStatus to) {
        return switch (from) {
            case CLAIMED -> to == FulfillmentStatus.PICKED_UP || to == FulfillmentStatus.CANCELLED;
            case PICKED_UP -> to == FulfillmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> to == FulfillmentStatus.DELIVERED;
            default -> false;
        };
    }

    private FulfillmentResponse toResponse(RetailFulfillment f) {
        return new FulfillmentResponse(
                f.getId(),
                f.getOrderId(),
                f.getCustomerUserId(),
                f.getDistributorId(),
                f.getStatus(),
                f.getCreatedAt(),
                f.getDeliveredAt());
    }
}
