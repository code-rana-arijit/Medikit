package com.medikit.delivery.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import com.medikit.delivery.dto.OrderConfirmedEvent;
import com.medikit.delivery.entity.Delivery;
import com.medikit.delivery.service.DeliveryService;
import com.medikit.delivery.service.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private final DeliveryService deliveryService;
    private final SlotService slotService;
    private final ObjectMapper objectMapper;

    public DeliveryEventConsumer(DeliveryService deliveryService,
                                 SlotService slotService,
                                 ObjectMapper objectMapper) {
        this.deliveryService = deliveryService;
        this.slotService = slotService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderConfirmed(String message) {
        try {
            OrderConfirmedEvent event = objectMapper.readValue(message, OrderConfirmedEvent.class);
            slotService.bookSlot(event.slotId());
            Delivery delivery = Delivery.builder()
                    .orderId(event.orderId())
                    .userId(event.userId())
                    .pharmacyId(event.pharmacyId())
                    .slotId(event.slotId())
                    .customerLatitude(event.customerLatitude())
                    .customerLongitude(event.customerLongitude())
                    .build();
            deliveryService.createDelivery(delivery);
        } catch (Exception e) {
            log.error("Failed to process ORDER_CONFIRMED event: {}", message, e);
        }
    }

    @KafkaListener(topics = Topics.DELIVERY_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onDeliveryUpdated(String message) {
        log.debug("Received DELIVERY_UPDATED event (no-op): {}", message);
    }
}
