package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AiAssistantContextRequest(
        @NotNull Long userId,
        @NotBlank String roleCode,
        @Size(max = 128) String traceId,
        Set<String> domains) {
    public AiAssistantContextRequest {
        domains = domains == null ? Set.of() : Set.copyOf(domains);
    }

    public AiAssistantContextRequest(Long userId, String roleCode, String traceId) {
        this(userId, roleCode, traceId, Set.of("ALL"));
    }
}
