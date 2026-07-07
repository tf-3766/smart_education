package com.zhongruan.edu.gateway.filter;

import com.zhongruan.edu.common.context.TraceIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {
    public static final String ATTRIBUTE = TraceIdGlobalFilter.class.getName() + ".traceId";
    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = TraceIds.normalizeOrCreate(exchange.getRequest().getHeaders().getFirst(TraceIds.HEADER));
        exchange.getAttributes().put(ATTRIBUTE, traceId);
        exchange.getResponse().getHeaders().set(TraceIds.HEADER, traceId);
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(TraceIds.HEADER);
            headers.set(TraceIds.HEADER, traceId);
        }).build();
        return chain.filter(exchange.mutate().request(request).build())
                .doOnSuccess(ignored -> log.debug(
                        "Gateway request completed traceId={} method={} path={}",
                        traceId,
                        request.getMethod(),
                        request.getPath()))
                .doOnError(error -> log.warn(
                        "Gateway request failed traceId={} method={} path={}",
                        traceId,
                        request.getMethod(),
                        request.getPath(),
                        error));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
