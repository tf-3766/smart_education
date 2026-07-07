package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record LessonDetailVO(
        String lessonId,
        String courseId,
        String chapterId,
        String title,
        CodeLabelVO contentType,
        String content,
        String videoUrl,
        Integer estimatedMinutes,
        Integer sortOrder,
        CodeLabelVO status,
        CodeLabelVO unlockType,
        OffsetDateTime unlockAt,
        OffsetDateTime publishedAt,
        Integer version) {}
