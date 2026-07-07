package com.zhongruan.edu.ai.shared;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAiWebFilter implements WebFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of("/actuator/health", "/actuator/info");
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public JwtAiWebFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (PUBLIC_PATHS.contains(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return writeError(exchange, CommonErrorCode.UNAUTHORIZED);
        }
        try {
            JwtClaims claims = jwtTokenService.parse(authorization.substring(BEARER_PREFIX.length()));
            ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                headers.remove("X-User-Id");
                headers.remove("X-User-Role");
                headers.set("X-User-Id", String.valueOf(claims.userId()));
                headers.set("X-User-Role", claims.activeRole());
            }).build();
            return chain.filter(exchange.mutate().request(request).build());
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
                TraceIdWebFilter.ATTRIBUTE,
                com.zhongruan.edu.common.context.TraceIds.normalizeOrCreate(null));
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
}
