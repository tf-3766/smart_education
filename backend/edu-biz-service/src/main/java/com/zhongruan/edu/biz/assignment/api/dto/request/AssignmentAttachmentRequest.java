package com.zhongruan.edu.biz.assignment.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record AssignmentAttachmentRequest(
        @NotBlank @Size(max = 160) String name,
        @Positive Long fileId,
        @Size(max = 512) String fileKey,
        @Size(max = 1024) String fileUrl,
        @Min(0) Long fileSize,
        @Size(max = 128) String mimeType,
        @NotNull Integer sortOrder) {}
