package com.zhongruan.edu.biz.course.api.dto.request;

import com.zhongruan.edu.biz.course.domain.enums.MaterialStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialType;
import com.zhongruan.edu.biz.course.domain.enums.MaterialVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCourseMaterialRequest(
        Long chapterId,
        Long lessonId,
        @NotBlank @Size(max = 160) String name,
        @NotNull MaterialType materialType,
        @Positive Long fileId,
        @Size(max = 512) String fileKey,
        @Size(max = 1024) String fileUrl,
        @PositiveOrZero Long fileSize,
        @Size(max = 128) String mimeType,
        @NotNull MaterialVisibility visibility,
        @NotNull MaterialStatus status,
        @NotNull @Min(0) @Max(100000) Integer sortOrder,
        @NotNull @Min(0) Integer version) {}
