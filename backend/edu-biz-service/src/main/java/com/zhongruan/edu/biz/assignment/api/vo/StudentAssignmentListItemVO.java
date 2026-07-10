package com.zhongruan.edu.biz.assignment.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentAssignmentListItemVO(
        String assignmentId,
        String courseId,
        String lessonId,
        String title,
        BigDecimal maxScore,
        CodeLabelVO availabilityStatus,
        OffsetDateTime dueAt,
        CodeLabelVO submissionStatus,
        OffsetDateTime submittedAt,
        boolean graded) {}
