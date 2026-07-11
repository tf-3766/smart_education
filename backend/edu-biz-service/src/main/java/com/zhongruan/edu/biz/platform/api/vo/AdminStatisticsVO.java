package com.zhongruan.edu.biz.platform.api.vo;

public record AdminStatisticsVO(
        long totalUsers,
        long enabledUsers,
        long students,
        long teachers,
        long administrators,
        long totalCourses,
        long publishedCourses,
        long pendingCourseReviews,
        long activeEnrollments,
        long publishedAssignments,
        long submittedAssignments,
        long publishedExams,
        long openWarnings,
        long publishedAnnouncements) {}
