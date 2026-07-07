package com.zhongruan.edu.biz.course.api.dto.request;

import com.zhongruan.edu.biz.course.domain.enums.LessonContentType;
import com.zhongruan.edu.biz.course.domain.enums.LessonUnlockType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpdateLessonRequest(
        @Positive Long courseId,
        @NotBlank @Size(max = 160) String title,
        @NotNull LessonContentType contentType,
        @Size(max = 60000) String content,
        @Size(max = 1024) String videoUrl,
        @Min(1) @Max(10000) Integer estimatedMinutes,
        @NotNull @Min(0) @Max(100000) Integer sortOrder,
        @NotNull LessonUnlockType unlockType,
        OffsetDateTime unlockAt,
        @NotNull @Min(0) Integer version) {}
