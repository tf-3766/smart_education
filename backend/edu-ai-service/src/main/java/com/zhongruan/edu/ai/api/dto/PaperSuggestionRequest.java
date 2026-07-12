package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaperSuggestionRequest(
        @NotNull @Positive Long courseId,
        @NotNull @Min(1) @Max(100) Integer questionCount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalScore,
        @Size(max = 1000) String requirements) {}
