package com.zhongruan.edu.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongruan.edu.gateway.filter.JwtGatewayFilter;
import com.zhongruan.edu.gateway.filter.TraceIdGlobalFilter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@SpringBootTest(properties = {
        "edu.gateway.jwt.secret=test-only-jwt-secret-with-at-least-32-bytes",
        "edu.gateway.jwt.ttl=PT15M",
        "edu.gateway.jwt.issuer=edu-biz-service-test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
class GatewayApplicationTest {
    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private TraceIdGlobalFilter traceIdGlobalFilter;

    @Autowired
    private JwtGatewayFilter jwtGatewayFilter;

    @Test
    void exposesOnlyExplicitBizAndAiRoutes() {
        List<String> routeIds = routeLocator.getRoutes()
                .map(Route::getId)
                .sort()
                .collectList()
                .block(Duration.ofSeconds(5));

        assertEquals(List.of("edu-ai-service", "edu-biz-service"), routeIds);
    }

    @Test
    void gatewayPreservesXTraceIdForDownstreamServices() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/me")
                        .header("X-Trace-Id", "gateway-trace-12345678")
                        .build());

        traceIdGlobalFilter
                .filter(exchange, forwarded -> {
                    assertEquals(
                            "gateway-trace-12345678",
                            forwarded.getRequest().getHeaders().getFirst("X-Trace-Id"));
                    return Mono.empty();
                })
                .block(Duration.ofSeconds(2));

        assertEquals("gateway-trace-12345678", exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void gatewayGeneratesXTraceIdWhenCallerDoesNotProvideOne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/me"));

        traceIdGlobalFilter
                .filter(exchange, forwarded -> {
                    assertNotNull(forwarded.getRequest().getHeaders().getFirst("X-Trace-Id"));
                    return Mono.empty();
                })
                .block(Duration.ofSeconds(2));

        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void gatewayAllowsAnonymousRegistrationRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/register").build());
        AtomicBoolean forwarded = new AtomicBoolean(false);

        jwtGatewayFilter
                .filter(exchange, forwardedExchange -> {
                    forwarded.set(true);
                    return Mono.empty();
                })
                .block(Duration.ofSeconds(2));

        assertTrue(forwarded.get());
    }
}
