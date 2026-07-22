package com.zhongruan.edu.ai.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BatchGradingDraftVO(
        String requestId,
        String rubric,
        double reviewThreshold,
        int totalCount,
        int reviewCount,
        String status,
        List<Item> items,
        OffsetDateTime createdAt) {
    public BatchGradingDraftVO {
        items = List.copyOf(items);
    }

    public record Item(
            String submissionId,
            String assignmentId,
            BigDecimal maxScore,
            BigDecimal suggestedScore,
            String comment,
            double confidence,
            boolean reviewRequired,
            List<String> anomalyCodes,
            List<String> reviewReasons,
            List<AiCitationVO> citations) {
        public Item {
            anomalyCodes = List.copyOf(anomalyCodes);
            reviewReasons = List.copyOf(reviewReasons);
            citations = List.copyOf(citations);
        }
    }
}
