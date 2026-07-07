package com.zhongruan.edu.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommonErrorCodeTest {
    @Test
    void exposesTheSharedErrorContractNames() {
        Set<String> codes = Stream.of(CommonErrorCode.values())
                .map(CommonErrorCode::code)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "PARAM_VALIDATION_ERROR",
                        "UNAUTHORIZED",
                        "TOKEN_EXPIRED",
                        "FORBIDDEN",
                        "INVALID_CREDENTIALS",
                        "RESOURCE_NOT_FOUND",
                        "RESOURCE_CONFLICT",
                        "OPERATION_NOT_ALLOWED",
                        "FILE_UPLOAD_FAILED",
                        "AI_SERVICE_UNAVAILABLE",
                        "AI_NO_RELIABLE_CONTEXT",
                        "SSE_STREAM_INTERRUPTED",
                        "INTERNAL_ERROR"),
                codes);
    }
}
