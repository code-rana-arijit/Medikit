package com.medikit.payment.controller;

import com.medikit.common.web.BadRequestException;
import com.medikit.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawBody,
                                              @RequestHeader Map<String, String> headers) {
        try {
            paymentService.handleWebhook(rawBody, headers);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Rejected webhook: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
}
