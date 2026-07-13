package com.medreminder.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

@Configuration
public class RateLimitingConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                    "unknown";
            return Mono.just(ip);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1000, 1000, 1);
    }
}

// Configuration for applying rate limiter to specific routes
// To use: Add to GatewayConfig routes:
// .filters(f -> f.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter)))
