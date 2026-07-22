package com.zhongruan.edu.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiCapabilityRegistryTest {
    private final AiCapabilityRegistry registry = new AiCapabilityRegistry();

    @Test
    void exposesRoleSpecificCapabilitiesFromOneRegistry() {
        assertThat(registry.available("STUDENT", null))
                .extracting(item -> item.capabilityId())
                .contains("student.learning-overview.query", "student.study-plan.generate")
                .doesNotContain("admin.teacher-registration.review", "course.submission.grade");
        assertThat(registry.available("TEACHER", null))
                .extracting(item -> item.capabilityId())
                .contains("course.submission.grade", "course.submission.batch-grade-assist", "course.assignment.publish", "course.teaching-package.plan", "course.risk-intervention.plan")
                .doesNotContain("platform.term-enrollment-window.upsert");
        assertThat(registry.available("SUPER_ADMIN", null))
                .extracting(item -> item.capabilityId())
                .contains("admin.teacher-registration.review", "platform.term-enrollment-window.upsert", "admin.operations-brief.generate")
                .doesNotContain("course.assignment.create");
    }

    @Test
    void courseCapabilitiesRemainDiscoverableButDisabledWithoutCourseContext() {
        var withoutCourse = registry.available("TEACHER", null).stream()
                .filter(item -> item.capabilityId().equals("course.question-bank.create"))
                .findFirst().orElseThrow();
        var withCourse = registry.available("TEACHER", 21001L).stream()
                .filter(item -> item.capabilityId().equals("course.question-bank.create"))
                .findFirst().orElseThrow();

        assertThat(withoutCourse.enabled()).isFalse();
        assertThat(withoutCourse.unavailableReason()).isEqualTo("进入具体课程后可用");
        assertThat(withCourse.enabled()).isTrue();
        assertThat(withCourse.confirmationPolicy()).isEqualTo("DRAFT_REVIEW");
        assertThat(withCourse.requiredContext()).contains("courseId");
    }
}
