package com.zhongruan.edu.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edu.gateway.ai-rate-limit")
public record AiRateLimitProperties(int replenishRate, int burstCapacity, int requestedTokens) {
    public AiRateLimitProperties {
        replenishRate = replenishRate <= 0 ? 5 : replenishRate;
        burstCapacity = burstCapacity <= 0 ? 10 : burstCapacity;
        requestedTokens = requestedTokens <= 0 ? 1 : requestedTokens;
    }
}

