package com.zhongruan.edu.ai.api.vo;

import java.time.OffsetDateTime;

public record AiServiceStatusVO(
        String serviceStatus,
        String framework,
        String frameworkVersion,
        String provider,
        String model,
        boolean modelConfigured,
        boolean vectorStoreConfigured,
        OffsetDateTime checkedAt) {}
