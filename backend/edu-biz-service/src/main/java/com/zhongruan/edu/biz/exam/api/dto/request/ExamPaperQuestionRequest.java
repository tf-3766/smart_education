package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExamPaperQuestionRequest(
        @NotNull @Positive Long questionId,
        @NotNull @Min(1) Integer questionOrder,
        @NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal score) {}
