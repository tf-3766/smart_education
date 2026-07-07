package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record TeacherCourseListItemVO(
        String courseId,
        String courseCode,
        String name,
        String term,
        String ownerTeacherId,
        String ownerTeacherName,
        CodeLabelVO status,
        CodeLabelVO reviewStatus,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime updatedAt) {}
