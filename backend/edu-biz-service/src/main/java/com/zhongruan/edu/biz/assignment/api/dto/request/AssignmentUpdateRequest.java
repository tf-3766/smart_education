package com.zhongruan.edu.biz.assignment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AssignmentUpdateRequest(
        Long lessonId,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxScore,
        OffsetDateTime openAt,
        @NotNull OffsetDateTime dueAt,
        @Valid List<AssignmentAttachmentRequest> attachments,
        @NotNull Integer version) {}
