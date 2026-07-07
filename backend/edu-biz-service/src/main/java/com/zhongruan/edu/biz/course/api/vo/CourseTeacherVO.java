package com.zhongruan.edu.biz.course.api.vo;

public record CourseTeacherVO(
        String relationId,
        String courseId,
        String teacherId,
        String teacherName,
        CodeLabelVO role,
        Integer version) {}
