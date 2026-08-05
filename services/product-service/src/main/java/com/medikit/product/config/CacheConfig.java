package com.medikit.product.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Hot-product local caching.
 * <p>
 * Product catalog reads are served from an in-process Caffeine cache (L1) to
 * absorb repeated reads of popular products, avoiding DB round-trips at 50K
 * concurrent users. Entries expire after a short TTL to bound staleness. The
 * Redis-backed cache remains the shared L2 across service instances.
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .initialCapacity(1_024)
                .recordStats());
        manager.setCacheNames(List.of("product", "category", "trending"));
        return manager;
    }
}
