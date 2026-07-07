package com.zhongruan.edu.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.ExpiredJwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    private static final String SECRET = "test-only-jwt-secret-with-at-least-32-bytes";

    @Test
    void issuedTokenRoundTripsStableIdentityClaims() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        JwtTokenService service = new JwtTokenService(SECRET, Duration.ofMinutes(15), "test-issuer", clock);

        String token = service.issue(
                        1002L,
                        "teacher",
                        "TEACHER",
                        Set.of("TEACHER"),
                        Set.of("teacher:access"))
                .value();
        JwtClaims claims = service.parse(token);

        assertEquals(1002L, claims.userId());
        assertEquals("TEACHER", claims.activeRole());
        assertEquals(Set.of("teacher:access"), claims.permissions());
    }

    @Test
    void expiredTokenIsRejectedByParser() {
        Clock issueClock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        JwtTokenService issuer = new JwtTokenService(SECRET, Duration.ofMinutes(1), "test-issuer", issueClock);
        String token = issuer.issue(1001L, "student", "STUDENT", Set.of("STUDENT"), Set.of())
                .value();

        Clock laterClock = Clock.fixed(Instant.parse("2026-07-06T12:02:00Z"), ZoneOffset.UTC);
        JwtTokenService verifier = new JwtTokenService(SECRET, Duration.ofMinutes(1), "test-issuer", laterClock);

        assertThrows(ExpiredJwtException.class, () -> verifier.parse(token));
    }
}
