package com.zhongruan.edu.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.common.security.JwtTokenService;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.AiPaperContextResponse;
import com.zhongruan.edu.feign.ai.AiQuestionRef;
import com.zhongruan.edu.feign.ai.AiSubmissionContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningEvidenceRef;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "edu.ai.jwt.secret=test-only-jwt-secret-with-at-least-32-bytes",
        "edu.ai.jwt.ttl=PT15M",
        "edu.ai.jwt.issuer=edu-biz-service-test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureWebTestClient
class AiServiceApplicationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private BizAiContextFeignClient contextClient;

    @Test
    void actuatorHealthIsAvailableWithoutAiProviderConfiguration() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void fallbackModeProvidesAuthorizedQaSummaryAndAdminStatus() {
        when(contextClient.getCourseContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiCourseContextResponse(
                        21001L,
                        "COURSE-PUBLISHED-001",
                        "已发布测试课程",
                        "PUBLISHED",
                        "APPROVED",
                        1002L,
                        true,
                        true,
                        List.of(new AiLessonRef(
                                23001L, 22001L, "公开课时", "PUBLISHED", "TEXT", "依赖注入用于降低组件耦合。", 30)),
                        List.of(new AiMaterialRef(
                                24001L,
                                22001L,
                                23001L,
                                "公开课时资料",
                                "DOCUMENT",
                                "course/material.pdf",
                                null,
                                "ENROLLED",
                                "PUBLISHED"))),
                "ai-test-trace"));
        when(contextClient.getSubmissionContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiSubmissionContextResponse(
                        21001L,
                        31001L,
                        "第一章课后练习",
                        "解释依赖注入并给出示例",
                        new BigDecimal("100"),
                        32001L,
                        "学生提交的第一章练习内容。",
                        new BigDecimal("88.5")),
                "ai-test-trace"));
        when(contextClient.getWarningContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiWarningContextResponse(
                        21001L,
                        36001L,
                        "PROGRESS_LAG",
                        "MEDIUM",
                        "学习进度低于课程节奏",
                        "建议完成第一章补学。",
                        List.of(new AiWarningEvidenceRef(
                                36101L,
                                "LESSON_PROGRESS",
                                23001L,
                                "completedLessonRate",
                                "0.25",
                                "课程学习完成率低于 50%。"))),
                "ai-test-trace"));
        when(contextClient.getPaperContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiPaperContextResponse(
                        21001L,
                        "COURSE-PUBLISHED-001",
                        "已发布测试课程",
                        List.of(new AiQuestionRef(
                                37101L,
                                37001L,
                                "SINGLE_CHOICE",
                                "下列哪一项最符合课程中的核心概念？",
                                "EASY",
                                new BigDecimal("5")))),
                "ai-test-trace"));

        String teacherToken = token(1002L, "teacher", "TEACHER", "teacher:access");
        webTestClient.post()
                .uri("/api/v1/ai/lessons/23001/summary-draft")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"courseId\":\"21001\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("FRAMEWORK_ONLY")
                .jsonPath("$.data.provider").isEqualTo("fallback")
                .jsonPath("$.data.citations[0].resourceType").isEqualTo("LESSON");

        String studentToken = token(1001L, "student", "STUDENT", "student:access");
        webTestClient.post()
                .uri("/api/v1/ai/courses/21001/qa/stream")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"这一课讲了什么？\",\"lessonId\":\"23001\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:meta");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:delta");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:citation");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:done");
                });

        String adminToken = token(1003L, "admin", "SUPER_ADMIN", "admin:access");
        webTestClient.get()
                .uri("/api/v1/ai/admin/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.framework").isEqualTo("Spring AI")
                .jsonPath("$.data.frameworkVersion").isEqualTo("1.1.8")
                .jsonPath("$.data.modelConfigured").isEqualTo(false);

        webTestClient.post()
                .uri("/api/v1/ai/submissions/32001/comment-draft")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"instruction\":\"语气积极且指出改进方向\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.draftType").isEqualTo("GRADING_COMMENT")
                .jsonPath("$.data.businessId").isEqualTo("32001")
                .jsonPath("$.data.status").isEqualTo("FRAMEWORK_ONLY");

        webTestClient.post()
                .uri("/api/v1/ai/warnings/36001/explanation")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.draftType").isEqualTo("RISK_EXPLANATION")
                .jsonPath("$.data.citations[0].resourceType").isEqualTo("WARNING_EVIDENCE");

        webTestClient.post()
                .uri("/api/v1/ai/exams/paper-suggestions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"courseId\":\"21001\",\"questionCount\":1,\"totalScore\":5}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.draftType").isEqualTo("PAPER_SUGGESTION")
                .jsonPath("$.data.citations[0].resourceType").isEqualTo("QUESTION");
    }

    @Test
    void authorizationFailureKeepsUnifiedErrorCodeForSseAndDraftEndpoints() {
        when(contextClient.getCourseContext(anyString(), any()))
                .thenThrow(new BusinessException(CommonErrorCode.FORBIDDEN));

        String studentToken = token(1001L, "student", "STUDENT", "student:access");
        webTestClient.post()
                .uri("/api/v1/ai/courses/21001/qa/stream")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"未授权问题\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:error");
                    org.assertj.core.api.Assertions.assertThat(body).contains("FORBIDDEN");
                    org.assertj.core.api.Assertions.assertThat(body).doesNotContain("AI_SERVICE_UNAVAILABLE");
                });

        String teacherToken = token(1002L, "teacher", "TEACHER", "teacher:access");
        webTestClient.post()
                .uri("/api/v1/ai/lessons/23001/summary-draft")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"courseId\":\"21001\"}")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FORBIDDEN");
    }

    private String token(Long userId, String username, String role, String permission) {
        return jwtTokenService.issue(userId, username, role, Set.of(role), Set.of(permission)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
