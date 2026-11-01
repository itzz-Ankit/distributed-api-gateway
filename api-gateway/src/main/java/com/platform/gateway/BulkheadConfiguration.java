package com.platform.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

@Component
public class BulkheadConfiguration implements GlobalFilter, Ordered {

    private static final int MAX_CONCURRENT_REQUESTS = 20;

    private final Semaphore userServiceSemaphore =
            new Semaphore(MAX_CONCURRENT_REQUESTS, true);

    private final Semaphore orderServiceSemaphore =
            new Semaphore(MAX_CONCURRENT_REQUESTS, true);

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        Route route = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
        );

        if (route == null) {
            return chain.filter(exchange);
        }

        Semaphore semaphore;
        String serviceName;

        if ("user-service".equals(route.getId())) {
            semaphore = userServiceSemaphore;
            serviceName = "User Service";

        } else if ("order-service".equals(route.getId())) {
            semaphore = orderServiceSemaphore;
            serviceName = "Order Service";

        } else {
            return chain.filter(exchange);
        }

        if (!semaphore.tryAcquire()) {
            return rejectRequest(exchange, serviceName);
        }

        Mono<Void> response;

        try {
            response = chain.filter(exchange);
        } catch (Throwable exception) {
            semaphore.release();
            return Mono.error(exception);
        }

        return response.doFinally(signal -> semaphore.release());
    }

    private Mono<Void> rejectRequest(
            ServerWebExchange exchange,
            String serviceName) {

        exchange.getResponse().setStatusCode(
                HttpStatus.SERVICE_UNAVAILABLE
        );

        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body =
                "{\"success\":false," +
                        "\"status\":503," +
                        "\"error\":\"Bulkhead capacity exceeded\"," +
                        "\"service\":\"" + serviceName + "\"," +
                        "\"message\":\"Maximum concurrent requests reached. Please retry later.\"}";

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}