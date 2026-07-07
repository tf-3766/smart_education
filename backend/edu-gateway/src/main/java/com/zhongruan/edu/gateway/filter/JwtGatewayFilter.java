package com.zhongruan.edu.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.security.JwtClaims;
import com.zhongruan.edu.common.security.JwtTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of("/api/v1/auth/login", "/actuator/health");
    private static final Set<String> INTERNAL_HEADERS =
            Set.of("X-User-Id", "X-User-Role", "X-Request-Source", "X-Internal-Token");

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public JwtGatewayFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        ServerHttpRequest.Builder sanitizedBuilder = exchange.getRequest().mutate();
        sanitizedBuilder.headers(headers -> INTERNAL_HEADERS.forEach(headers::remove));
        if (PUBLIC_PATHS.contains(path) || "OPTIONS".equals(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange.mutate().request(sanitizedBuilder.build()).build());
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return writeError(exchange, CommonErrorCode.UNAUTHORIZED);
        }

        try {
            JwtClaims claims = jwtTokenService.parse(authorization.substring(BEARER_PREFIX.length()));
            sanitizedBuilder.headers(headers -> {
                headers.set("X-User-Id", String.valueOf(claims.userId()));
                headers.set("X-User-Role", claims.activeRole());
                headers.set("X-Request-Source", "GATEWAY");
            });
            return chain.filter(exchange.mutate().request(sanitizedBuilder.build()).build());
        } catch (ExpiredJwtException exception) {
            return writeError(exchange, CommonErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            return writeError(exchange, CommonErrorCode.UNAUTHORIZED);
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, CommonErrorCode errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorCode.httpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getAttributeOrDefault(
                TraceIdGlobalFilter.ATTRIBUTE, TraceIdsFallback.create());
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(ApiResponse.failure(errorCode, traceId));
        } catch (JsonProcessingException exception) {
            bytes = "{\"code\":\"INTERNAL_ERROR\",\"message\":\"系统内部错误\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -150;
    }

    private static final class TraceIdsFallback {
        private TraceIdsFallback() {}

        static String create() {
            return com.zhongruan.edu.common.context.TraceIds.normalizeOrCreate(null);
        }
    }
}
