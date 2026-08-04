package com.medikit.payment.controller;

import com.medikit.payment.dto.PaymentRequest;
import com.medikit.payment.dto.PaymentResponse;
import com.medikit.payment.dto.RefundRequest;
import com.medikit.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiate(request));
    }

    @PostMapping("/capture")
    public ResponseEntity<PaymentResponse> capture(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(paymentService.capture(request.getOrDefault("orderId", request.get("order_id"))));
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refund(@RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.refund(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getByOrder(orderId));
    }
}
