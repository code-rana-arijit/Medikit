package com.medikit.loyalty.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "discount-service", path = "/api/v1/discounts")
public interface DiscountClient {

    @PostMapping("/issue")
    Map<String, Object> issue(@RequestBody Map<String, Object> request);
}
