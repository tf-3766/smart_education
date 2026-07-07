package com.zhongruan.edu.biz.shared.web;

import com.zhongruan.edu.common.context.TraceIds;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestTrace {
    private RequestTrace() {}

    public static String from(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE);
        return value instanceof String traceId ? traceId : TraceIds.normalizeOrCreate(null);
    }
}

