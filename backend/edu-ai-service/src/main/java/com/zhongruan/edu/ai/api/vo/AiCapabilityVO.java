package com.zhongruan.edu.ai.api.vo;

import java.util.Set;

/** 当前用户实际可用的 AI 能力元数据；前端入口和模型工具必须使用同一 capabilityId。 */
public record AiCapabilityVO(
        String capabilityId,
        String name,
        String description,
        Set<String> roles,
        String mode,
        String riskLevel,
        Set<String> requiredContext,
        String confirmationPolicy,
        String deepLinkTemplate,
        boolean enabled,
        String unavailableReason) {

    public AiCapabilityVO {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        requiredContext = requiredContext == null ? Set.of() : Set.copyOf(requiredContext);
    }

    public boolean requiresCourseContext() {
        return requiredContext.contains("courseId");
    }
}
