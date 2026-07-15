package com.zhongruan.edu.gateway.config;

import com.zhongruan.edu.common.security.JwtTokenService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties({GatewaySecurityProperties.class, AiRateLimitProperties.class})
public class GatewayConfig {
    @Bean
    JwtTokenService gatewayJwtTokenService(GatewaySecurityProperties properties) {
        return new JwtTokenService(properties.secret(), properties.ttl(), properties.issuer());
    }

    @Bean
    RedisRateLimiter aiRedisRateLimiter(AiRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.replenishRate(), properties.burstCapacity(), properties.requestedTokens());
    }

    @Bean
    KeyResolver aiRateLimitKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String hostAddress = exchange.getRequest().getRemoteAddress() == null
                    ? "unknown"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just("ip:" + hostAddress);
        };
    }

    @Bean
    RouteLocator routes(
            RouteLocatorBuilder builder, RedisRateLimiter aiRedisRateLimiter, KeyResolver aiRateLimitKeyResolver) {
        return builder.routes()
                .route("edu-ai-service", route -> route.order(-10)
                        .path("/api/v1/ai/**")
                        .filters(filters -> filters.requestRateLimiter(config -> {
                            config.setRateLimiter(aiRedisRateLimiter);
                            config.setKeyResolver(aiRateLimitKeyResolver);
                        }))
                        .uri("lb://edu-ai-service"))
                .route("edu-biz-service", route -> route.order(0)
                        .path("/api/v1/**")
                        .uri("lb://edu-biz-service"))
                .build();
    }

    @Bean
    CorsWebFilter corsWebFilter(
            @Value("${edu.gateway.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
                    List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                "X-Trace-Id",
                "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("X-Trace-Id", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
