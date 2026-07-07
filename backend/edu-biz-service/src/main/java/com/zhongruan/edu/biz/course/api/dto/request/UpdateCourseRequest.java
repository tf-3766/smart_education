package com.zhongruan.edu.biz.course.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateCourseRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String summary,
        @Size(max = 1024) String coverUrl,
        Long categoryId,
        @Size(max = 32) String term,
        @Size(max = 128) String department,
        @DecimalMin("0.0") @DecimalMax("99.99") BigDecimal credit,
        OffsetDateTime enrollmentOpenAt,
        OffsetDateTime enrollmentCloseAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        @NotNull @Min(0) Integer version) {}
