package com.zhongruan.edu.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edu.gateway.jwt")
public record GatewaySecurityProperties(String secret, Duration ttl, String issuer) {
    public GatewaySecurityProperties {
        ttl = ttl == null ? Duration.ofHours(2) : ttl;
        issuer = issuer == null || issuer.isBlank() ? "edu-biz-service" : issuer;
    }
}

