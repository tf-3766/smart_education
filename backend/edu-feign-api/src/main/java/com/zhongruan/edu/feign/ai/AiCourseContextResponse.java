package com.zhongruan.edu.feign.ai;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AiCourseContextResponse(
        Long courseId,
        String courseCode,
        String courseName,
        String courseStatus,
        String reviewStatus,
        Long ownerTeacherId,
        boolean teacherMember,
        boolean enrolled,
        List<AiLessonRef> lessons,
        List<AiMaterialRef> materials,
        String summary,
        Long categoryId,
        String term,
        String department,
        BigDecimal credit,
        OffsetDateTime enrollmentOpenAt,
        OffsetDateTime enrollmentCloseAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer version,
        List<AiChapterRef> chapters) {
    public AiCourseContextResponse {
        lessons = lessons == null ? List.of() : List.copyOf(lessons);
        materials = materials == null ? List.of() : List.copyOf(materials);
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }

    public AiCourseContextResponse(
            Long courseId,
            String courseCode,
            String courseName,
            String courseStatus,
            String reviewStatus,
            Long ownerTeacherId,
            boolean teacherMember,
            boolean enrolled,
            List<AiLessonRef> lessons,
            List<AiMaterialRef> materials) {
        this(courseId, courseCode, courseName, courseStatus, reviewStatus, ownerTeacherId,
                teacherMember, enrolled, lessons, materials, null, null, null, null, null,
                null, null, null, null, null, List.of());
    }

    public AiCourseContextResponse(
            Long courseId,
            String courseCode,
            String courseName,
            String courseStatus,
            String reviewStatus,
            Long ownerTeacherId,
            boolean teacherMember,
            boolean enrolled,
            List<AiLessonRef> lessons,
            List<AiMaterialRef> materials,
            String summary,
            Long categoryId,
            String term,
            String department,
            BigDecimal credit,
            OffsetDateTime enrollmentOpenAt,
            OffsetDateTime enrollmentCloseAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            Integer version) {
        this(courseId, courseCode, courseName, courseStatus, reviewStatus, ownerTeacherId,
                teacherMember, enrolled, lessons, materials, summary, categoryId, term, department, credit,
                enrollmentOpenAt, enrollmentCloseAt, startAt, endAt, version, List.of());
    }
}
