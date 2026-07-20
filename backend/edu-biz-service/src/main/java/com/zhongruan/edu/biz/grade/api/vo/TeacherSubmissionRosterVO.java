package com.zhongruan.edu.biz.grade.api.vo;

public record TeacherSubmissionRosterVO(
        String studentId,
        String studentName,
        boolean submitted,
        TeacherSubmissionGradeVO submission) {}