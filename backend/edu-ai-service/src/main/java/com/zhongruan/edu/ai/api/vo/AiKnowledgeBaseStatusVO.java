package com.zhongruan.edu.ai.api.vo;

import java.time.OffsetDateTime;

public record AiKnowledgeBaseStatusVO(
        String courseId,
        boolean vectorStoreConfigured,
        int indexedChunks,
        OffsetDateTime lastSyncedAt) {}
