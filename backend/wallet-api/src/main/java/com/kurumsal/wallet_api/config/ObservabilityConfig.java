package com.kurumsal.wallet_api.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the aspect that turns {@code @Observed} into spans (exported via the OTLP pipeline
 * already configured in application.yml) — Spring Boot auto-instruments HTTP/JDBC, but does not
 * register this aspect by default, so {@code @Observed} annotations are otherwise inert.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
