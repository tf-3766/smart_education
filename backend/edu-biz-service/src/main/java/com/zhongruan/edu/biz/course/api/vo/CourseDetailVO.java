package com.zhongruan.edu.biz.course.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CourseDetailVO(
        String courseId,
        String courseCode,
        String name,
        String summary,
        String coverUrl,
        String categoryId,
        String term,
        String department,
        BigDecimal credit,
        String ownerTeacherId,
        String ownerTeacherName,
        CodeLabelVO status,
        CodeLabelVO reviewStatus,
        OffsetDateTime enrollmentOpenAt,
        OffsetDateTime enrollmentCloseAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String latestReviewReason,
        Integer version) {}
