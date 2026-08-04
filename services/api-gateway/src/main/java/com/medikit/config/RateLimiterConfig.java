package com.medikit.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> resolveKey(exchange);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(getRemoteIp(exchange));
    }

    private Mono<String> resolveKey(ServerWebExchange exchange) {
        Mono<String> principalKey = exchange.getPrincipal()
                .map(Principal::getName)
                .defaultIfEmpty(getRemoteIp(exchange));
        return principalKey;
    }

    private String getRemoteIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
