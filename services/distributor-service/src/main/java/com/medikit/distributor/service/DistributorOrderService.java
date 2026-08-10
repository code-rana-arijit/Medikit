package com.medikit.distributor.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.common.web.PageResult;
import com.medikit.distributor.dto.DistributorOrderItemResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DistributorOrderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DistributorOrderRepository orderRepository;
    private final DistributorProfileRepository profileRepository;
    private final CatalogItemRepository catalogRepository;
    private final DistributorService distributorService;

    public DistributorOrderService(DistributorOrderRepository orderRepository,
                                   DistributorProfileRepository profileRepository,
                                   CatalogItemRepository catalogRepository,
                                   DistributorService distributorService) {
        this.orderRepository = orderRepository;
        this.profileRepository = profileRepository;
        this.catalogRepository = catalogRepository;
        this.distributorService = distributorService;
    }

    @Transactional
    public DistributorOrderResponse placeOrder(UUID buyerUserId, DistributorOrderRequest request) {
        DistributorProfile distributor = profileRepository.findById(request.distributorId())
                .orElseThrow(() -> new NotFoundException("Distributor not found"));
        if (!distributor.isActive()) {
            throw new BadRequestException("Distributor is not active");
        }
        if (distributor.getUserId().equals(buyerUserId)) {
            throw new BadRequestException("Cannot place a supply order with your own distributor profile");
        }

        DistributorOrder order = DistributorOrder.builder()
                .orderNumber(generateOrderNumber())
                .buyerUserId(buyerUserId)
                .distributorId(distributor.getId())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (var req : request.items()) {
            CatalogItem catalog = catalogRepository.findByDistributorIdAndProductId(distributor.getId(), req.productId())
                    .orElseThrow(() -> new BadRequestException(
                            "Product " + req.productId() + " not in distributor catalog"));
            if (catalog.getStockQty() < req.quantity()) {
                throw new BadRequestException("Insufficient stock for " + catalog.getProductName());
            }
            BigDecimal subtotal = catalog.getUnitPrice().multiply(BigDecimal.valueOf(req.quantity()));
            total = total.add(subtotal);
            order.addItem(DistributorOrderItem.builder()
                    .productId(catalog.getProductId())
                    .productName(catalog.getProductName())
                    .quantity(req.quantity())
                    .unitPrice(catalog.getUnitPrice())
                    .subtotal(subtotal)
                    .build());
        }

        order.setTotalAmount(total);
        DistributorOrder saved = orderRepository.save(order);
        for (var item : order.getItems()) {
            distributorService.adjustStock(distributor.getId(), item.getProductId(), -item.getQuantity());
        }
        return toResponse(saved);
    }

    @Transactional
    public DistributorOrderResponse updateStatus(UUID userId, UUID orderId, DistributorOrderStatus target) {
        DistributorOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        boolean isBuyer = order.getBuyerUserId().equals(userId);
        boolean isDistributor = profileRepository.findByUserId(userId)
                .map(p -> p.getId().equals(order.getDistributorId()))
                .orElse(false);

        switch (target) {
            case CONFIRMED, SHIPPED, DELIVERED -> {
                if (!isDistributor) {
                    throw new BadRequestException("Only the distributor can confirm, ship or deliver an order");
                }
                if (!canTransition(order.getStatus(), target)) {
                    throw new BadRequestException("Invalid status transition from " + order.getStatus() + " to " + target);
                }
            }
            case CANCELLED -> {
                if (!isBuyer && !isDistributor) {
                    throw new BadRequestException("Only buyer or distributor can cancel the order");
                }
                if (order.getStatus() == DistributorOrderStatus.DELIVERED) {
                    throw new BadRequestException("Cannot cancel a delivered order");
                }
            }
            default -> throw new BadRequestException("Unsupported status " + target);
        }

        order.setStatus(target);
        if (target == DistributorOrderStatus.CANCELLED) {
            for (var item : order.getItems()) {
                distributorService.adjustStock(order.getDistributorId(), item.getProductId(), item.getQuantity());
            }
        }
        return toResponse(orderRepository.save(order));
    }

    public DistributorOrderResponse getOrder(UUID userId, UUID orderId) {
        DistributorOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        boolean isBuyer = order.getBuyerUserId().equals(userId);
        boolean isDistributor = profileRepository.findByUserId(userId)
                .map(p -> p.getId().equals(order.getDistributorId()))
                .orElse(false);
        if (!isBuyer && !isDistributor) {
            throw new NotFoundException("Order not found");
        }
        return toResponse(order);
    }

    public PageResult<DistributorOrderResponse> myOrders(UUID userId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<DistributorOrder> orders;
        DistributorOrderStatus filter = parseStatus(status);
        if (filter != null) {
            orders = orderRepository.findByBuyerUserIdAndStatus(userId, filter, pageable);
        } else {
            orders = orderRepository.findByBuyerUserId(userId, pageable);
        }
        return PageResult.from(orders.map(this::toResponse));
    }

    public PageResult<DistributorOrderResponse> distributorOrders(UUID userId, int page, int size, String status) {
        DistributorProfile profile = distributorService.findForUser(userId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<DistributorOrder> orders;
        DistributorOrderStatus filter = parseStatus(status);
        if (filter != null) {
            orders = orderRepository.findByDistributorIdAndStatus(profile.getId(), filter, pageable);
        } else {
            orders = orderRepository.findByDistributorId(profile.getId(), pageable);
        }
        return PageResult.from(orders.map(this::toResponse));
    }

    private boolean canTransition(DistributorOrderStatus from, DistributorOrderStatus to) {
        return switch (from) {
            case PENDING -> to == DistributorOrderStatus.CONFIRMED || to == DistributorOrderStatus.CANCELLED;
            case CONFIRMED -> to == DistributorOrderStatus.SHIPPED;
            case SHIPPED -> to == DistributorOrderStatus.DELIVERED;
            default -> false;
        };
    }

    private DistributorOrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return DistributorOrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status filter");
        }
    }

    private String generateOrderNumber() {
        return "DB2B-" + LocalDate.now().format(DATE) + "-" + ThreadLocalRandom.current().nextInt(10000, 100000);
    }

    private DistributorOrderResponse toResponse(DistributorOrder order) {
        List<DistributorOrderItemResponse> items = order.getItems().stream()
                .map(i -> new DistributorOrderItemResponse(
                        i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();
        return new DistributorOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerUserId(),
                order.getDistributorId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
