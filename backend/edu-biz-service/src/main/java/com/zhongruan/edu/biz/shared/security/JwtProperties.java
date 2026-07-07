package com.zhongruan.edu.biz.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edu.security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("edu.security.jwt.secret is required");
        }
        ttl = ttl == null ? Duration.ofHours(2) : ttl;
        issuer = issuer == null || issuer.isBlank() ? "edu-biz-service" : issuer;
    }
}

