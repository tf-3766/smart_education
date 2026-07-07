package com.zhongruan.edu.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edu.ai.jwt")
public record AiJwtProperties(String secret, Duration ttl, String issuer) {
    public AiJwtProperties {
        ttl = ttl == null ? Duration.ofHours(2) : ttl;
        issuer = issuer == null || issuer.isBlank() ? "edu-biz-service" : issuer;
    }
}

