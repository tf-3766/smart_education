package com.zhongruan.edu.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zhongruan.edu.common.error.CommonErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ApiResponseTest {
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-06T12:30:00.123Z"), ZoneOffset.UTC);

    @Test
    void successResponseContainsStableEnvelope() {
        ApiResponse<String> response = ApiResponse.success("ready", "trace-12345678", FIXED_CLOCK);

        assertEquals("SUCCESS", response.code());
        assertEquals("OK", response.message());
        assertEquals("ready", response.data());
        assertEquals("trace-12345678", response.traceId());
        assertEquals("2026-07-06T12:30:00.123Z", response.timestamp().toString());
    }

    @Test
    void failureResponseContainsSafeErrorWithoutThrowableDetails() {
        ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR, "trace-87654321");

        assertEquals("INTERNAL_ERROR", response.code());
        assertEquals("系统内部错误，请稍后重试", response.message());
        assertNull(response.data());
        assertEquals("trace-87654321", response.traceId());
    }
}
