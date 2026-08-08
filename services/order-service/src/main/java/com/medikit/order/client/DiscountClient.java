package com.medikit.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "discount-service", path = "/api/v1/discounts")
public interface DiscountClient {

    @PostMapping("/validate")
    Map<String, Object> validate(@RequestBody Map<String, Object> request);

    @PostMapping("/redeem")
    Map<String, Object> redeem(@RequestBody Map<String, Object> request);
}
