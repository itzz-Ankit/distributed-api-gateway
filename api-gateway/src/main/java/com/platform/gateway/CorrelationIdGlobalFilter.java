package com.platform.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID = "X-Correlation-ID";

    private final ApplicationEventPublisher eventPublisher;
    private final Service observabilityService;

    public CorrelationIdGlobalFilter(
            ApplicationEventPublisher eventPublisher,
            Service observabilityService) {
        this.eventPublisher = eventPublisher;
        this.observabilityService = observabilityService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();

        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String requestId = correlationId;

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(CORRELATION_ID, requestId)))
                .build();
        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID, requestId);

        return chain.filter(mutatedExchange).doFinally(signalType -> {
            long latencyMs = (System.nanoTime() - startTime) / 1_000_000;
            HttpStatus status = mutatedExchange.getResponse().getStatusCode();
            int statusCode = status != null ? status.value() : 500;

            Route route = mutatedExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String serviceName = route != null ? route.getId() : "api-gateway";

            observabilityService.recordRequest(serviceName, statusCode, latencyMs);

            GatewayRequestEvent event = new GatewayRequestEvent(
                    requestId,
                    serviceName,
                    mutatedExchange.getRequest().getMethodValue(),
                    mutatedExchange.getRequest().getURI().getPath(),
                    statusCode,
                    latencyMs,
                    Instant.now()
            );

            try {
                eventPublisher.publishEvent(event);
                log.info("Request completed | CorrelationId: {} | Service: {} | Status: {} | Latency: {} ms",
                        requestId, serviceName, statusCode, latencyMs);
            } catch (Exception exception) {
                log.error("Failed to publish gateway request event: {}", requestId, exception);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
