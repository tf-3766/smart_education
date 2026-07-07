package com.zhongruan.edu.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;

public final class JwtTokenService {
    private static final String USERNAME = "username";
    private static final String ACTIVE_ROLE = "activeRole";
    private static final String ROLES = "roles";
    private static final String PERMISSIONS = "permissions";

    private final SecretKey key;
    private final Duration ttl;
    private final String issuer;
    private final Clock clock;

    public JwtTokenService(String secret, Duration ttl, String issuer) {
        this(secret, ttl, issuer, Clock.systemUTC());
    }

    JwtTokenService(String secret, Duration ttl, String issuer, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.issuer = issuer;
        this.clock = clock;
    }

    public IssuedToken issue(
            Long userId,
            String username,
            String activeRole,
            Collection<String> roles,
            Collection<String> permissions) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ttl);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(USERNAME, username)
                .claim(ACTIVE_ROLE, activeRole)
                .claim(ROLES, List.copyOf(roles))
                .claim(PERMISSIONS, List.copyOf(permissions))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaims(
                Long.valueOf(claims.getSubject()),
                claims.get(USERNAME, String.class),
                claims.get(ACTIVE_ROLE, String.class),
                stringSet(claims.get(ROLES, List.class)),
                stringSet(claims.get(PERMISSIONS, List.class)),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant());
    }

    private static Set<String> stringSet(List<?> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof String text) {
                result.add(text);
            }
        }
        return Set.copyOf(result);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}

