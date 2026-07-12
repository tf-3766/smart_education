package com.zhongruan.edu.feign.ai;

public record AiWarningEvidenceRef(
        Long evidenceId,
        String evidenceType,
        Long sourceId,
        String metricCode,
        String metricValue,
        String description) {}
