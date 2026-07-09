package com.zhongruan.edu.feign.ai;

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
        List<AiMaterialRef> materials) {
    public AiCourseContextResponse {
        lessons = lessons == null ? List.of() : List.copyOf(lessons);
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}
