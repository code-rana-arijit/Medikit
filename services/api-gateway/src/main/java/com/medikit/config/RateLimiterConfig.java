package com.medikit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Rate limiting key resolvers.
 * <p>
 * The primary resolver keys the token bucket on the authenticated user when
 * present, otherwise on the client IP, so a logged-in user is not throttled by
 * a shared per-IP bucket while anonymous traffic still gets per-IP limits.
 * A dedicated IP resolver is also exposed for a stricter per-IP burst cap.
 * </p>
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    @Primary
    public KeyResolver compositeKeyResolver() {
        return exchange -> resolveCompositeKey(exchange);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> resolveKey(exchange);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(ipKey(getRemoteIp(exchange)));
    }

    private Mono<String> resolveCompositeKey(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .defaultIfEmpty(ipKey(getRemoteIp(exchange)));
    }

    private Mono<String> resolveKey(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(p -> "user:" + p.getName())
                .defaultIfEmpty(ipKey(getRemoteIp(exchange)));
    }

    private String ipKey(String ip) {
        return "ip:" + ip;
    }

    private String getRemoteIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
