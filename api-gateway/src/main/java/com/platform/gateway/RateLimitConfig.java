package com.platform.gateway;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter rateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress address = exchange.getRequest().getRemoteAddress();
            String ip = address == null || address.getAddress() == null
                    ? "unknown"
                    : address.getAddress().getHostAddress();

            return Mono.just(ip);
        };
    }
}
