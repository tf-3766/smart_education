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
        List<String> platformMetrics) {
    public AiAssistantContextResponse {
        enrollmentWindows = safe(enrollmentWindows);
        courses = safe(courses);
        warnings = safe(warnings);
        assignments = safe(assignments);
        exams = safe(exams);
        platformMetrics = safe(platformMetrics);
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}