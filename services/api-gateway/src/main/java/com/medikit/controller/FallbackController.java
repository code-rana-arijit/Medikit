package com.medikit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{status}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable(required = false) Integer status) {
        HttpStatus httpStatus = status != null && HttpStatus.resolve(status) != null
                ? HttpStatus.resolve(status)
                : HttpStatus.SERVICE_UNAVAILABLE;

        return Mono.just(ResponseEntity.status(httpStatus).body(Map.of(
                "code", httpStatus.value(),
                "message", "Service is temporarily unavailable. Please try again later.",
                "service", "medikit-gateway-fallback"
        )));
    }
}
