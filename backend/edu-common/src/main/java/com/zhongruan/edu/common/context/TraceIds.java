package com.zhongruan.edu.common.context;

import java.util.UUID;
import java.util.regex.Pattern;

public final class TraceIds {
    public static final String HEADER = "X-Trace-Id";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{8,128}");

    private TraceIds() {}

    public static String normalizeOrCreate(String candidate) {
        if (candidate != null && SAFE_TRACE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
