package com.medikit.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized observability configuration.
 * <p>
 * Tracing: Micrometer Tracing bridge to OpenTelemetry. Spring Boot auto-configures
 * the OTLP exporter from `management.otlp.tracing.endpoint` (defaults to Tempo when
 * the OTLP endpoint is reachable). Each service exposes Prometheus metrics at
 * `/actuator/prometheus` and distributed traces via Micrometer Tracing.
 * </p>
 * <p>
 * Configure via environment variables:
 *   management.otlp.tracing.endpoint  -> OTLP collector (Tempo) URL
 *   management.tracing.sampling.probability -> 0.0 - 1.0 sample rate
 * </p>
 */
@Configuration
public class ObservabilityConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityConfig.class);

    public ObservabilityConfig() {
        log.info("Medikit observability configuration loaded (Micrometer Tracing + Prometheus)");
    }
}
