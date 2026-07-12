package com.zhongruan.edu.feign.ai;

import java.util.List;

public record AiWarningContextResponse(
        Long courseId,
        Long warningId,
        String warningType,
        String warningLevel,
        String summary,
        String suggestion,
        List<AiWarningEvidenceRef> evidences) {
    public AiWarningContextResponse {
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
    }
}
