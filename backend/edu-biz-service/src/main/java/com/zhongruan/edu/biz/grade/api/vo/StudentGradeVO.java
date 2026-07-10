package com.zhongruan.edu.biz.grade.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentGradeVO(
        String gradeId,
        String courseId,
        String assignmentId,
        String assignmentTitle,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal scoreRate,
        String teacherComment,
        OffsetDateTime publishedAt) {}
