package com.platform.gateway;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayMetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        Timer.Sample sample = Timer.start(meterRegistry);

        String method = exchange.getRequest()
                .getMethodValue();

        return chain.filter(exchange)
                .doFinally(signalType -> {

                    Route route = exchange.getAttribute(
                            ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
                    );

                    String routeName = route != null
                            ? route.getId()
                            : "unmatched";

                    int statusCode = exchange.getResponse()
                            .getStatusCode() != null
                            ? exchange.getResponse()
                            .getStatusCode().value()
                            : 500;

                    String status = String.valueOf(statusCode);

                    Counter.builder("gateway.requests")
                            .description("Total requests processed by API Gateway")
                            .tag("route", routeName)
                            .tag("method", method)
                            .tag("status", status)
                            .register(meterRegistry)
                            .increment();

                    sample.stop(
                            Timer.builder("gateway.request.duration")
                                    .description("API Gateway request processing time")
                                    .publishPercentileHistogram()
                                    .tag("route", routeName)
                                    .tag("method", method)
                                    .tag("status", status)
                                    .register(meterRegistry)
                    );
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}