package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** AI 服务提交给 Biz 的待确认动作计划；参数由 capabilityId 对应的领域处理器解析。 */
public record AiActionPlanRequest(
        @NotNull Long userId,
        @NotBlank String roleCode,
        @NotBlank @Size(max = 128) String capabilityId,
        @NotBlank @Size(max = 160) String idempotencyKey,
        @NotBlank @Size(max = 16000) String parametersJson) {}
