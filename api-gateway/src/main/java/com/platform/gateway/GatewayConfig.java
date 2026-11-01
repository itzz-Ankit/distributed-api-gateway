package com.platform.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            RedisRateLimiter rateLimiter,
            KeyResolver ipKeyResolver) {

        return builder.routes()

                // User Service Swagger Documentation
                .route("user-api-docs", route -> route
                        .path("/v3/api-docs/user")
                        .filters(filters -> filters.rewritePath(
                                "/v3/api-docs/user",
                                "/v3/api-docs"
                        ))
                        .uri("lb://user-service"))

                // Order Service Swagger Documentation
                .route("order-api-docs", route -> route
                        .path("/v3/api-docs/order")
                        .filters(filters -> filters.rewritePath(
                                "/v3/api-docs/order",
                                "/v3/api-docs"
                        ))
                        .uri("lb://order-service"))

                // User Service Route
                .route("user-service", route -> route
                        .path("/user", "/user/**")
                        .filters(filters -> filters

                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(ipKeyResolver))

                                .circuitBreaker(config -> config
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")
                                        .addStatusCode("INTERNAL_SERVER_ERROR")
                                        .addStatusCode("BAD_GATEWAY")
                                        .addStatusCode("SERVICE_UNAVAILABLE")
                                        .addStatusCode("GATEWAY_TIMEOUT"))

                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT
                                        )
                                        .setBackoff(
                                                Duration.ofMillis(200),
                                                Duration.ofSeconds(1),
                                                2,
                                                false
                                        ))
                        )
                        .uri("lb://user-service"))

                // Order Service Route
                .route("order-service", route -> route
                        .path("/orders", "/orders/**")
                        .filters(filters -> filters

                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiter)
                                        .setKeyResolver(ipKeyResolver))

                                .circuitBreaker(config -> config
                                        .setName("orderServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/order")
                                        .addStatusCode("INTERNAL_SERVER_ERROR")
                                        .addStatusCode("BAD_GATEWAY")
                                        .addStatusCode("SERVICE_UNAVAILABLE")
                                        .addStatusCode("GATEWAY_TIMEOUT"))

                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT
                                        )
                                        .setBackoff(
                                                Duration.ofMillis(200),
                                                Duration.ofSeconds(1),
                                                2,
                                                false
                                        ))
                        )
                        .uri("lb://order-service"))

                .build();
    }
}