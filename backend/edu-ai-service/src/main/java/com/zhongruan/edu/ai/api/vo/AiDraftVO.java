package com.zhongruan.edu.ai.api.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AiDraftVO(
        String requestId,
        String draftType,
        String businessId,
        String content,
        String provider,
        String model,
        String status,
        List<AiCitationVO> citations,
        OffsetDateTime createdAt) {
    public AiDraftVO {
        citations = List.copyOf(citations);
    }
}
