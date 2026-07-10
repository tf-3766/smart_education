package com.zhongruan.edu.biz.warning.api.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record LearningWarningVO(
        String warningId,
        String courseId,
        String studentId,
        String studentName,
        CodeLabelVO warningType,
        CodeLabelVO warningLevel,
        CodeLabelVO warningStatus,
        String summary,
        String suggestion,
        String aiExplanationDraftId,
        OffsetDateTime generatedAt,
        String handledBy,
        String handleRemark,
        OffsetDateTime handledAt,
        List<WarningEvidenceVO> evidences,
        Integer version) {}
