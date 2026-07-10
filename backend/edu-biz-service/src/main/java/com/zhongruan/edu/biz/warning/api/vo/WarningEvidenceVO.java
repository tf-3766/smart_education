package com.zhongruan.edu.biz.warning.api.vo;

public record WarningEvidenceVO(
        String evidenceId,
        String evidenceType,
        String sourceId,
        String metricCode,
        String metricValue,
        String description) {}
