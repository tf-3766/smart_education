package com.zhongruan.edu.ai.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminGovernanceDraftVO(
        String requestId,
        String status,
        int totalCount,
        int successCount,
        int failureCount,
        int reviewCount,
        List<TeacherReviewItem> teacherReviews,
        List<CourseComplianceItem> courseCompliance,
        OffsetDateTime createdAt) {
    public AdminGovernanceDraftVO {
        teacherReviews = List.copyOf(teacherReviews);
        courseCompliance = List.copyOf(courseCompliance);
    }

    public record TeacherReviewItem(
            String userId,
            Integer targetVersion,
            String username,
            String displayName,
            OffsetDateTime registeredAt,
            String candidate,
            String recommendation,
            double confidence,
            boolean reviewRequired,
            List<String> riskCodes,
            List<String> reasons,
            List<String> evidence) {
        public TeacherReviewItem {
            riskCodes = List.copyOf(riskCodes);
            reasons = List.copyOf(reasons);
            evidence = List.copyOf(evidence);
        }
    }

    public record CourseComplianceItem(
            String courseId,
            Integer targetVersion,
            String courseCode,
            String courseName,
            String courseStatus,
            String reviewStatus,
            String summary,
            String categoryId,
            String term,
            String department,
            BigDecimal credit,
            OffsetDateTime enrollmentOpenAt,
            OffsetDateTime enrollmentCloseAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int lessonCount,
            int materialCount,
            int readinessScore,
            String recommendation,
            boolean failed,
            boolean reviewRequired,
            List<String> issueCodes,
            List<String> reasons,
            List<String> evidence) {
        public CourseComplianceItem {
            issueCodes = List.copyOf(issueCodes);
            reasons = List.copyOf(reasons);
            evidence = List.copyOf(evidence);
        }

        public static CourseComplianceItem unavailable(String courseId, String reason) {
            return new CourseComplianceItem(
                    courseId, null, null, "课程上下文读取失败", null, null,
                    null, null, null, null, null, null, null, null, null,
                    0, 0, 0, "UNAVAILABLE", true, true,
                    List.of("CONTEXT_UNAVAILABLE"), List.of(reason), List.of());
        }
    }
}
