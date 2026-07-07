package com.zhongruan.edu.common.context;

public record RequestContext(String traceId, Long userId, String currentRole, RequestSource source) {
    public static RequestContext anonymous(String traceId, RequestSource source) {
        return new RequestContext(traceId, null, null, source);
    }
}

