package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record EnrollmentVO(
        String enrollmentId,
        String courseId,
        String studentId,
        CodeLabelVO status,
        OffsetDateTime enrolledAt,
        OffsetDateTime withdrawnAt,
        Integer version) {}
