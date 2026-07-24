package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

class RoleScopedPlatformToolsTest {
    private final AiAssistantContextResponse context = new AiAssistantContextResponse(
            1L, "user", "STUDENT", OffsetDateTime.now(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of());

    @Test
    void studentReceivesOnlyOwnReadTools() {
        assertThat(toolNames("STUDENT"))
                .contains("getMyCourses", "getMyLearningProgress", "getMyPublishedGrades", "getMyAnnouncements")
                .doesNotContain("getPlatformMetrics", "getPlatformUsers", "getPendingTeacherRegistrations",
                        "getCourseSubmissions");
    }

    @Test
    void ordinaryAdminDoesNotReceiveSuperAdminTools() {
        assertThat(toolNames("ADMIN"))
                .contains("getPlatformMetrics", "getPlatformAnnouncements")
                .doesNotContain("getPlatformUsers", "getPendingTeacherRegistrations", "getMyPublishedGrades");
        assertThat(toolNames("SUPER_ADMIN"))
                .contains("getPlatformUsers", "getPendingTeacherRegistrations");
    }

    private List<String> toolNames(String role) {
        return Arrays.stream(RoleScopedPlatformTools.forRole(context, role))
                .flatMap(tool -> Arrays.stream(tool.getClass().getMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(annotation -> annotation != null)
                .map(Tool::name)
                .toList();
    }
}
