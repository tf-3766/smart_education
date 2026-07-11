package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GradeExamAnswerRequest(
        @NotNull @Positive Long questionId,
        @NotNull @DecimalMin("0.00") BigDecimal score,
        @Size(max = 1000) String teacherComment) {}
