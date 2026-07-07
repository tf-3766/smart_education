package com.zhongruan.edu.biz.course.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentCourseListItemVO(
        String courseId,
        String courseCode,
        String name,
        String summary,
        String coverUrl,
        String term,
        BigDecimal credit,
        String ownerTeacherName,
        CodeLabelVO status,
        CodeLabelVO enrollmentStatus,
        boolean enrollable,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {}
