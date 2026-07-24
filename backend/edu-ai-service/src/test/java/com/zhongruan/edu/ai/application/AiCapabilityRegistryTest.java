package com.zhongruan.edu.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiCapabilityRegistryTest {
    private final AiCapabilityRegistry registry = new AiCapabilityRegistry();

    @Test
    void exposesRoleSpecificCapabilitiesFromOneRegistry() {
        assertThat(registry.available("STUDENT", null))
                .extracting(item -> item.capabilityId())
                .contains("student.learning-overview.query", "student.grade.query", "student.communication.query")
                .doesNotContain("student.study-plan.generate")
                .doesNotContain("admin.teacher-registration.review", "course.submission.grade");
        assertThat(registry.available("STUDENT", null))
                .allMatch(item -> item.mode().equals("ANSWER"));
        assertThat(registry.available("TEACHER", null))
                .extracting(item -> item.capabilityId())
                .contains("course.submission.grade", "course.submission.batch-grade-assist", "course.assignment.publish",
                        "course.teaching-package.plan", "course.risk-intervention.plan", "course.paper-suggestion.generate")
                .doesNotContain("platform.term-enrollment-window.upsert");
        assertThat(registry.available("TEACHER", null)).extracting(item -> item.mode())
                .contains("ANSWER", "DRAFT", "ACTION");
        assertThat(registry.available("ADMIN", null)).extracting(item -> item.capabilityId())
                .contains("admin.course-governance.query", "admin.ai-service.query", "platform.term-enrollment-window.upsert")
                .doesNotContain("admin.user-governance.query", "admin.teacher-registration.batch-precheck",
                        "admin.teacher-registration.review");
        assertThat(registry.available("SUPER_ADMIN", null))
                .extracting(item -> item.capabilityId())
                .contains("admin.teacher-registration.review", "platform.term-enrollment-window.upsert", "admin.operations-brief.generate")
                .doesNotContain("course.assignment.create");
    }

    @Test
    void teacherCanStartCrossCourseAuthoringFromAnyPageWhileCourseQaNeedsCourseContext() {
        var withoutCourse = registry.available("TEACHER", null).stream()
                .filter(item -> item.capabilityId().equals("course.question-bank.create"))
                .findFirst().orElseThrow();
        var courseQa = registry.available("TEACHER", null).stream()
                .filter(item -> item.capabilityId().equals("course.knowledge.qa"))
                .findFirst().orElseThrow();

        assertThat(withoutCourse.enabled()).isTrue();
        assertThat(withoutCourse.confirmationPolicy()).isEqualTo("DRAFT_REVIEW");
        assertThat(courseQa.enabled()).isFalse();
        assertThat(courseQa.unavailableReason()).isEqualTo("进入具体课程后可用");
    }

    @Test
    void policySummaryReflectsHardRoleBoundary() {
        assertThat(registry.policySummary("STUDENT", null))
                .contains("仅允许模式：ANSWER", "student.grade.query")
                .doesNotContain("DRAFT", "ACTION");
    }
}
