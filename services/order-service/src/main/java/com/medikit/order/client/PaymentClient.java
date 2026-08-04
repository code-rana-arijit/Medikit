package com.medikit.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentClient {

    @PostMapping("/initiate")
    Map<String, Object> initiatePayment(@RequestBody Map<String, Object> request);

    @PostMapping("/capture")
    Map<String, Object> capturePayment(@RequestBody Map<String, Object> request);

    @PostMapping("/refund")
    Map<String, Object> refundPayment(@RequestBody Map<String, Object> request);
}
