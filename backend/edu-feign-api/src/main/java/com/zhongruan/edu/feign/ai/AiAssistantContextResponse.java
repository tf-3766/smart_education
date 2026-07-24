package com.zhongruan.edu.feign.ai;

import java.time.OffsetDateTime;
import java.util.List;

public record AiAssistantContextResponse(
        Long userId,
        String username,
        String roleCode,
        OffsetDateTime generatedAt,
        List<String> enrollmentWindows,
        List<String> courses,
        List<String> warnings,
        List<String> assignments,
        List<String> exams,
        List<String> platformMetrics,
        List<String> pendingTeacherRegistrations,
        List<String> submissions,
        List<AiTeacherRegistrationCandidate> pendingTeacherCandidates,
        List<String> learningProgress,
        List<String> grades,
        List<String> announcements,
        List<String> forumActivity,
        List<String> notifications,
        List<String> users) {
    public AiAssistantContextResponse {
        enrollmentWindows = safe(enrollmentWindows);
        courses = safe(courses);
        warnings = safe(warnings);
        assignments = safe(assignments);
        exams = safe(exams);
        platformMetrics = safe(platformMetrics);
        pendingTeacherRegistrations = safe(pendingTeacherRegistrations);
        submissions = safe(submissions);
        pendingTeacherCandidates = pendingTeacherCandidates == null
                ? List.of() : List.copyOf(pendingTeacherCandidates);
        learningProgress = safe(learningProgress);
        grades = safe(grades);
        announcements = safe(announcements);
        forumActivity = safe(forumActivity);
        notifications = safe(notifications);
        users = safe(users);
    }

    public AiAssistantContextResponse(
            Long userId,
            String username,
            String roleCode,
            OffsetDateTime generatedAt,
            List<String> enrollmentWindows,
            List<String> courses,
            List<String> warnings,
            List<String> assignments,
            List<String> exams,
            List<String> platformMetrics,
            List<String> pendingTeacherRegistrations,
            List<String> submissions) {
        this(userId, username, roleCode, generatedAt, enrollmentWindows, courses, warnings, assignments,
                exams, platformMetrics, pendingTeacherRegistrations, submissions, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public AiAssistantContextResponse(
            Long userId,
            String username,
            String roleCode,
            OffsetDateTime generatedAt,
            List<String> enrollmentWindows,
            List<String> courses,
            List<String> warnings,
            List<String> assignments,
            List<String> exams,
            List<String> platformMetrics,
            List<String> pendingTeacherRegistrations,
            List<String> submissions,
            List<AiTeacherRegistrationCandidate> pendingTeacherCandidates) {
        this(userId, username, roleCode, generatedAt, enrollmentWindows, courses, warnings, assignments,
                exams, platformMetrics, pendingTeacherRegistrations, submissions, pendingTeacherCandidates,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
