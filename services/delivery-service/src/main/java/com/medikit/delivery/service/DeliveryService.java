package com.medikit.delivery.service;

import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.ForbiddenException;
import com.medikit.common.web.NotFoundException;
import com.medikit.delivery.dto.DeliveryResponse;
import com.medikit.delivery.dto.DeliveryStatusRequest;
import com.medikit.delivery.entity.Delivery;
import com.medikit.delivery.entity.DeliveryStatus;
import com.medikit.delivery.repository.DeliveryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final String PARTNER_KEY_PREFIX = "medikit:delivery:partner:";

    private final DeliveryRepository deliveryRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventPublisher eventPublisher;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           StringRedisTemplate redisTemplate,
                           EventPublisher eventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DeliveryResponse createDelivery(Delivery delivery) {
        return toResponse(deliveryRepository.save(delivery));
    }

    @Transactional
    public DeliveryResponse assignPartner(UUID orderId) {
        Delivery delivery = findDelivery(orderId);
        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new ConflictException("Cannot assign partner to cancelled delivery");
        }
        UUID partnerId = pickPartner();
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        redisTemplate.opsForValue().set(PARTNER_KEY_PREFIX + partnerId, "ONLINE");
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publish(Topics.DELIVERY_ASSIGNED, orderId.toString(), saved);
        return toResponse(saved);
    }

    @Transactional
    public DeliveryResponse claim(UUID orderId, UUID partnerId) {
        Delivery delivery = findDelivery(orderId);
        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new ConflictException("Cannot claim cancelled delivery");
        }
        if (delivery.getPartnerId() != null) {
            throw new ConflictException("Delivery already assigned to a partner");
        }
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        redisTemplate.opsForValue().set(PARTNER_KEY_PREFIX + partnerId, "ONLINE");
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publish(Topics.DELIVERY_ASSIGNED, orderId.toString(), saved);
        return toResponse(saved);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID orderId, DeliveryStatus status,
                                         DeliveryStatusRequest.Coordinates coordinates, UUID callerId) {
        Delivery delivery = findDelivery(orderId);
        if (delivery.getPartnerId() == null) {
            throw new BadRequestException("Delivery has no assigned partner yet");
        }
        if (status != DeliveryStatus.CANCELLED) {
            validatePartner(delivery.getPartnerId(), callerId);
        }
        delivery.setStatus(status);
        if (coordinates != null) {
            delivery.setPartnerLatitude(coordinates.latitude());
            delivery.setPartnerLongitude(coordinates.longitude());
        }
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(Instant.now());
        }
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publish(Topics.DELIVERY_UPDATED, orderId.toString(), saved);
        return toResponse(saved);
    }

    public List<DeliveryResponse> myDeliveries(UUID partnerId, DeliveryStatus status) {
        List<Delivery> deliveries = status == null
                ? deliveryRepository.findByPartnerId(partnerId)
                : deliveryRepository.findByPartnerIdAndStatusIn(partnerId, List.of(status));
        return deliveries.stream().map(this::toResponse).toList();
    }

    public List<DeliveryResponse> availableDeliveries() {
        return deliveryRepository.findByPartnerIdIsNullAndStatus(DeliveryStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    public DeliveryResponse track(UUID orderId) {
        return toResponse(findDelivery(orderId));
    }

    private void validatePartner(UUID partnerId, UUID callerId) {
        if (callerId == null) {
            throw new ForbiddenException("Missing X-User-Id header");
        }
        if (!callerId.equals(partnerId)) {
            throw new ForbiddenException("Only the assigned delivery partner can update this delivery");
        }
    }

    private Delivery findDelivery(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Delivery not found for order " + orderId));
    }

    private UUID pickPartner() {
        return UUID.randomUUID();
    }

    private DeliveryResponse toResponse(Delivery d) {
        return new DeliveryResponse(
                d.getId(),
                d.getOrderId(),
                d.getUserId(),
                d.getPharmacyId(),
                d.getSlotId(),
                d.getStatus().name(),
                d.getPartnerId(),
                d.getCustomerLatitude(),
                d.getCustomerLongitude(),
                d.getPartnerLatitude(),
                d.getPartnerLongitude(),
                d.getEstimatedMinutes(),
                d.getDeliveredAt(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
