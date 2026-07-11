package com.zhongruan.edu.ai.api.vo;

import java.time.OffsetDateTime;

public record AiStreamEvent(
        String type,
        String requestId,
        Object data,
        OffsetDateTime timestamp) {}
