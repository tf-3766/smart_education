package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchGradingDraftRequest(
        @NotEmpty @Size(max = 50) List<Long> submissionIds,
        @NotBlank @Size(max = 4000) String rubric,
        @DecimalMin("0.50") @DecimalMax("0.95") Double reviewThreshold,
        @Size(max = 1000) String instruction) {
    public BatchGradingDraftRequest {
        submissionIds = submissionIds == null ? List.of() : List.copyOf(submissionIds);
        reviewThreshold = reviewThreshold == null ? 0.75D : reviewThreshold;
    }
}
