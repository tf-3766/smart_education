package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record CourseReviewListItemVO(
        String courseId,
        String courseCode,
        String name,
        String ownerTeacherId,
        String ownerTeacherName,
        String term,
        CodeLabelVO courseStatus,
        CodeLabelVO reviewStatus,
        OffsetDateTime updatedAt) {}
