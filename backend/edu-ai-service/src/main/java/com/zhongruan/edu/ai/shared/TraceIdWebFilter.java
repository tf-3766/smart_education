package com.zhongruan.edu.ai.shared;

import com.zhongruan.edu.common.context.TraceIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFilter implements WebFilter {
    public static final String ATTRIBUTE = TraceIdWebFilter.class.getName() + ".traceId";
    private static final Logger log = LoggerFactory.getLogger(TraceIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = TraceIds.normalizeOrCreate(exchange.getRequest().getHeaders().getFirst(TraceIds.HEADER));
        exchange.getAttributes().put(ATTRIBUTE, traceId);
        exchange.getResponse().getHeaders().set(TraceIds.HEADER, traceId);
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(TraceIds.HEADER);
            headers.set(TraceIds.HEADER, traceId);
        }).build();
        return chain.filter(exchange.mutate().request(request).build())
                .doOnSuccess(ignored -> log.debug(
                        "AI service request completed traceId={} method={} path={}",
                        traceId,
                        request.getMethod(),
                        request.getPath()))
                .doOnError(error -> log.warn(
                        "AI service request failed traceId={} method={} path={}",
                        traceId,
                        request.getMethod(),
                        request.getPath(),
                        error));
    }
}
