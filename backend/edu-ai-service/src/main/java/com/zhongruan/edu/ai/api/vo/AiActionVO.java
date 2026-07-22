package com.zhongruan.edu.ai.api.vo;

import com.zhongruan.edu.feign.ai.AiActionResponse;
import java.time.OffsetDateTime;
import java.util.Map;

/** 工具执行后的结构化业务结果，供助手直接渲染结果卡而不是解析模型文本。 */
public record AiActionVO(
        String actionId,
        String capabilityId,
        String status,
        String resourceType,
        String resourceId,
        String title,
        String summary,
        String href,
        boolean requiresConfirmation,
        String riskLevel,
        String confirmationPolicy,
        Map<String, String> preview,
        String errorCode,
        String errorMessage,
        OffsetDateTime expiresAt) {

    public AiActionVO(
            String actionId,
            String capabilityId,
            String status,
            String resourceType,
            String resourceId,
            String title,
            String summary,
            String href,
            boolean requiresConfirmation) {
        this(actionId, capabilityId, status, resourceType, resourceId, title, summary, href,
                requiresConfirmation, null, null, Map.of(), null, null, null);
    }

    public static AiActionVO from(AiActionResponse action) {
        return new AiActionVO(
                action.actionId(), action.capabilityId(), action.status(), action.resourceType(),
                action.resourceId(), action.title(), action.summary(), action.href(),
                action.requiresConfirmation(), action.riskLevel(), action.confirmationPolicy(),
                action.preview(), action.errorCode(), action.errorMessage(), action.expiresAt());
    }
}
