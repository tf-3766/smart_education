package com.zhongruan.edu.biz.grade.api.vo;

import java.math.BigDecimal;

public record AssignmentStatisticsVO(
        String assignmentId,
        String courseId,
        long totalStudentCount,
        long submittedCount,
        long missingCount,
        long gradedCount,
        long publishedGradeCount,
        BigDecimal averageScore,
        long lowScoreCount) {}
