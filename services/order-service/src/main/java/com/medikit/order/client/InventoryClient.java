package com.medikit.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "inventory-service", path = "/api/v1/inventory")
public interface InventoryClient {

    @PostMapping("/reserve")
    Map<String, Object> reserveStock(@RequestBody Map<String, Object> request);

    @PostMapping("/confirm")
    Map<String, Object> confirmReservation(@RequestBody Map<String, Object> request);

    @PostMapping("/release")
    Map<String, Object> releaseReservation(@RequestBody Map<String, Object> request);

    @PostMapping("/stock/bulk")
    List<Map<String, Object>> getBulkStock(@RequestBody Map<String, Object> request);
}
