package com.zhongruan.edu.biz.assignment.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AssignmentQuestionRequest(
        @NotBlank @Size(max = 64) String questionId,
        @NotBlank
        @Pattern(regexp = "SINGLE_CHOICE|MULTI_CHOICE|TRUE_FALSE|FILL_BLANK|SHORT_ANSWER")
        String questionType,
        @NotBlank @Size(max = 2000) String stem,
        @Size(max = 10) List<@Size(max = 500) String> options,
        @NotNull @DecimalMin(value = "0.01") BigDecimal score,
        @Size(max = 10) List<@Size(max = 500) String> correctAnswers) {

    public AssignmentQuestionRequest withoutCorrectAnswers() {
        return new AssignmentQuestionRequest(questionId, questionType, stem, options, score, List.of());
    }
}