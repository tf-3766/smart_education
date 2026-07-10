package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateExamRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 10000) String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        @Min(1) Integer durationMinutes,
        @NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal totalScore,
        @NotNull @Min(0) Integer version) {}
