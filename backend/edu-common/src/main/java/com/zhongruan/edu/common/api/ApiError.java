package com.zhongruan.edu.common.api;

public record ApiError(String field, String reason, Object rejectedValue) {
    public static ApiError of(String field, String reason) {
        return new ApiError(field, reason, null);
    }
}

