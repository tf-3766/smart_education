package com.zhongruan.edu.feign.ai;

import java.time.OffsetDateTime;
import java.util.Map;

/** 持久化 AI 动作的跨服务响应，也是助手结构化确认卡的数据来源。 */
public record AiActionResponse(
        String actionId,
        String capabilityId,
        String status,
        String riskLevel,
        String confirmationPolicy,
        String targetType,
        String targetId,
        Integer targetVersion,
        String title,
        String summary,
        Map<String, String> preview,
        String resourceType,
        String resourceId,
        String href,
        boolean requiresConfirmation,
        String errorCode,
        String errorMessage,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime executedAt,
        OffsetDateTime createdAt) {}
