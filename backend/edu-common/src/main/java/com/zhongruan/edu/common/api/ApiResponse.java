package com.zhongruan.edu.common.api;

import com.zhongruan.edu.common.error.ErrorCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        List<ApiError> errors,
        String traceId,
        OffsetDateTime timestamp) {

    private static final Clock SYSTEM_CLOCK = Clock.systemUTC();

    public ApiResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return success(data, traceId, SYSTEM_CLOCK);
    }

    public static ApiResponse<Void> success(String traceId) {
        return success(null, traceId, SYSTEM_CLOCK);
    }

    static <T> ApiResponse<T> success(T data, String traceId, Clock clock) {
        return new ApiResponse<>(
                "SUCCESS", "OK", data, List.of(), traceId, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String traceId) {
        return failure(errorCode, errorCode.message(), List.of(), traceId);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String traceId) {
        return failure(errorCode, traceId);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, String traceId) {
        return failure(errorCode, message, List.of(), traceId);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, String traceId) {
        return failure(errorCode, message, traceId);
    }

    public static <T> ApiResponse<T> failure(
            ErrorCode errorCode, String message, List<ApiError> errors, String traceId) {
        return new ApiResponse<>(
                errorCode.code(),
                message,
                null,
                errors,
                traceId,
                OffsetDateTime.now(SYSTEM_CLOCK).withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static <T> ApiResponse<T> fail(
            ErrorCode errorCode, String message, List<ApiError> errors, String traceId) {
        return failure(errorCode, message, errors, traceId);
    }
}
