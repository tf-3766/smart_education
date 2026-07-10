package com.zhongruan.edu.biz.grade.api.vo;

import java.math.BigDecimal;

public record CourseGradeStatisticsVO(
        String courseId,
        long assignmentCount,
        long publishedAssignmentCount,
        long enrolledStudentCount,
        long gradedRecordCount,
        long publishedGradeCount,
        BigDecimal averageScoreRate,
        BigDecimal passRate,
        long lowScoreCount) {}
