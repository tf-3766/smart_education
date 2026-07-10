package com.zhongruan.edu.biz.assignment.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TeacherAssignmentListItemVO(
        String assignmentId,
        String courseId,
        String lessonId,
        String title,
        BigDecimal maxScore,
        CodeLabelVO assignmentStatus,
        CodeLabelVO availabilityStatus,
        OffsetDateTime dueAt,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt,
        Integer version) {}
