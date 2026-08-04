package com.medikit.order.saga;

import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderStatus;
import com.medikit.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class SagaTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final int sagaTimeoutMinutes;

    public SagaTimeoutScheduler(OrderRepository orderRepository,
                                OrderSagaOrchestrator sagaOrchestrator,
                                @Value("${medikit.order.saga-timeout-minutes:10}") int sagaTimeoutMinutes) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.sagaTimeoutMinutes = sagaTimeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${medikit.order.grace-period-minutes:5}m", initialDelay = 60000)
    @Transactional
    public void expireStuckOrders() {
        Instant cutoff = Instant.now().minusSeconds(sagaTimeoutMinutes * 60L);
        List<Order> stuck = orderRepository.findStaleOrders(
                List.of(OrderStatus.CREATED, OrderStatus.PENDING_PAYMENT), cutoff);

        for (Order order : stuck) {
            log.warn("Compensating stale order {} (status {})", order.getId(), order.getStatus());
            sagaOrchestrator.compensate(order, "Saga timeout - order not confirmed in time");
        }
    }
}
