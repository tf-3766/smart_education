package com.zhongruan.edu.biz.warning.api.vo;

import java.util.List;

public record WarningGenerationResultVO(
        int createdCount,
        int skippedCount,
        List<LearningWarningVO> warnings) {}
