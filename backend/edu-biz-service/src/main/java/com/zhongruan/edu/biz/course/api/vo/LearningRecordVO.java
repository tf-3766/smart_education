package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record LearningRecordVO(
        String recordId,
        String courseId,
        String chapterId,
        String lessonId,
        String studentId,
        CodeLabelVO status,
        Long studySeconds,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime lastStudiedAt) {}
