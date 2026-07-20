package com.zhongruan.edu.biz.platform.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpsertTermEnrollmentWindowRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^\\d{4} (春季|秋季)$", message = "学期格式应为 YYYY 春季/秋季")
        String term,
        OffsetDateTime enrollmentOpenAt,
        OffsetDateTime enrollmentCloseAt) {}
