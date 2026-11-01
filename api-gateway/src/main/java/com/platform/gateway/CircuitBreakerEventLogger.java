package com.platform.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CircuitBreakerEventLogger {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory>
    circuitBreakerEventCustomizer() {

        return factory -> factory.addCircuitBreakerCustomizer(
                circuitBreaker -> circuitBreaker.getEventPublisher()

                        .onStateTransition(event ->
                                log.warn(
                                        "Circuit Breaker State Changed: {}",
                                        event
                                )
                        )

                        .onError(event ->
                                log.error(
                                        "Circuit Breaker Error: {}",
                                        event
                                )
                        ),

                "userServiceCircuitBreaker",
                "orderServiceCircuitBreaker"
        );
    }
}