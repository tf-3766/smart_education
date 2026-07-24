package com.zhongruan.edu.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.common.security.JwtTokenService;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.AiPaperContextResponse;
import com.zhongruan.edu.feign.ai.AiQuestionRef;
import com.zhongruan.edu.feign.ai.AiSubmissionContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningEvidenceRef;
import com.zhongruan.edu.feign.ai.AiTeacherRegistrationCandidate;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    @MockitoBean
    private com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient authoringClient;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.zhongruan.edu.feign.ai.BizAiActionFeignClient actionClient;

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
                                "PUBLISHED", "依赖注入课件正文", "EXTRACTED", "测试正文"))),
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
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:tool");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:delta");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:citation");
                    org.assertj.core.api.Assertions.assertThat(body).contains("event:done");
                });

        webTestClient.get()
                .uri("/api/v1/ai/courses/21001/knowledge-base/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.courseId").isEqualTo("21001")
                .jsonPath("$.data.vectorStoreConfigured").isEqualTo(false)
                .jsonPath("$.data.indexedChunks").isEqualTo(0);
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
                .uri("/api/v1/ai/submissions/batch-grading-draft")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"submissionIds":["32001"],"rubric":"概念准确 60 分，示例完整 40 分","reviewThreshold":0.75}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.totalCount").isEqualTo(1)
                .jsonPath("$.data.reviewCount").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("FRAMEWORK_ONLY")
                .jsonPath("$.data.items[0].submissionId").isEqualTo("32001")
                .jsonPath("$.data.items[0].reviewRequired").isEqualTo(true)
                .jsonPath("$.data.items[0].anomalyCodes[0]").exists();

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

    @Test
    void adminGovernanceDraftPrechecksTeachersAndCoursesWithoutWritingBusinessData() {
        when(contextClient.getAssistantContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiAssistantContextResponse(
                        1003L, "admin", "SUPER_ADMIN", OffsetDateTime.now(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of("张老师（用户名 teacher_zhang，用户ID 1002，版本 3）"), List.of(),
                        List.of(new AiTeacherRegistrationCandidate(
                                1002L, "teacher_zhang", "张老师", 3, OffsetDateTime.parse("2026-07-20T08:00:00Z")))),
                "ai-test-trace"));
        when(contextClient.getCourseContext(anyString(), any())).thenReturn(ApiResponse.success(
                new AiCourseContextResponse(
                        21001L, "JAVA-101", "Java 程序设计", "DRAFT", "PENDING", 1002L,
                        false, false,
                        List.of(new AiLessonRef(23001L, 22001L, "第一章", "PUBLISHED", "TEXT", "正文", 1)),
                        List.of(),
                        "面向本科生的 Java 基础课程", 11L, "2026 秋季", "计算机学院", new BigDecimal("3.0"),
                        OffsetDateTime.parse("2026-08-20T00:00:00Z"),
                        OffsetDateTime.parse("2026-09-10T00:00:00Z"),
                        OffsetDateTime.parse("2026-09-15T00:00:00Z"),
                        OffsetDateTime.parse("2027-01-10T00:00:00Z"), 7),
                "ai-test-trace"));

        String adminToken = token(1003L, "admin", "SUPER_ADMIN", "admin:access");
        webTestClient.post()
                .uri("/api/v1/ai/admin/governance-review-draft")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"teacherUserIds\":[\"1002\"],\"courseIds\":[\"21001\"]}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.totalCount").isEqualTo(2)
                .jsonPath("$.data.status").isEqualTo("FRAMEWORK_ONLY")
                .jsonPath("$.data.teacherReviews[0].userId").isEqualTo("1002")
                .jsonPath("$.data.teacherReviews[0].targetVersion").isEqualTo(3)
                .jsonPath("$.data.teacherReviews[0].username").isEqualTo("teacher_zhang")
                .jsonPath("$.data.teacherReviews[0].reviewRequired").isEqualTo(true)
                  .jsonPath("$.data.courseCompliance[0].courseId").isEqualTo("21001")
                  .jsonPath("$.data.courseCompliance[0].targetVersion").isEqualTo(7)
                  .jsonPath("$.data.courseCompliance[0].summary").isEqualTo("面向本科生的 Java 基础课程")
                  .jsonPath("$.data.courseCompliance[0].categoryId").isEqualTo("11")
                  .jsonPath("$.data.courseCompliance[0].term").isEqualTo("2026 秋季")
                  .jsonPath("$.data.courseCompliance[0].department").isEqualTo("计算机学院")
                  .jsonPath("$.data.courseCompliance[0].credit").isEqualTo(3.0)
                  .jsonPath("$.data.courseCompliance[0].enrollmentOpenAt").isEqualTo("2026-08-20T00:00:00Z")
                  .jsonPath("$.data.courseCompliance[0].enrollmentCloseAt").isEqualTo("2026-09-10T00:00:00Z")
                  .jsonPath("$.data.courseCompliance[0].startAt").isEqualTo("2026-09-15T00:00:00Z")
                  .jsonPath("$.data.courseCompliance[0].endAt").isEqualTo("2027-01-10T00:00:00Z")
                  .jsonPath("$.data.courseCompliance[0].issueCodes[0]").isEqualTo("NO_MATERIALS")
                  .jsonPath("$.data.courseCompliance[0].reviewRequired").isEqualTo(true);
      }

      @Test
      void adminGovernanceDraftReportsMissingAndInvalidCourseMetadata() {
          when(contextClient.getAssistantContext(anyString(), any())).thenReturn(ApiResponse.success(
                  new AiAssistantContextResponse(
                          1003L, "admin", "SUPER_ADMIN", OffsetDateTime.now(),
                          List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                  "ai-test-trace"));
          when(contextClient.getCourseContext(anyString(), any())).thenReturn(ApiResponse.success(
                  new AiCourseContextResponse(
                          21002L, "META-INVALID", "元数据不完整课程", "PUBLISHED", "APPROVED", 1002L,
                          false, false,
                          List.of(new AiLessonRef(23002L, 22002L, "有效课时", "PUBLISHED", "TEXT", "正文", 1)),
                          List.of(new AiMaterialRef(
                                  24002L, 22002L, 23002L, "有效资料", "DOCUMENT", "material.pdf", null,
                                  "ENROLLED", "PUBLISHED", "资料正文", "EXTRACTED", "正文")),
                          null, null, "", "", BigDecimal.ZERO,
                          OffsetDateTime.parse("2026-09-01T00:00:00Z"), null,
                          OffsetDateTime.parse("2026-10-01T00:00:00Z"),
                          OffsetDateTime.parse("2026-09-30T00:00:00Z"), 2),
                  "ai-test-trace"));

          String adminToken = token(1003L, "admin", "SUPER_ADMIN", "admin:access");
          webTestClient.post()
                  .uri("/api/v1/ai/admin/governance-review-draft")
                  .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue("{\"courseIds\":[\"21002\"]}")
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody()
                  .jsonPath("$.data.courseCompliance[0].readinessScore").isEqualTo(25)
                  .jsonPath("$.data.courseCompliance[0].issueCodes").value(org.hamcrest.Matchers.hasItems(
                          "MISSING_SUMMARY", "MISSING_CATEGORY", "MISSING_TERM", "MISSING_DEPARTMENT",
                          "INVALID_CREDIT", "INVALID_COURSE_WINDOW", "MISSING_ENROLLMENT_WINDOW"))
                  .jsonPath("$.data.courseCompliance[0].categoryId").doesNotExist()
                  .jsonPath("$.data.courseCompliance[0].enrollmentCloseAt").doesNotExist();
      }

    private String token(Long userId, String username, String role, String permission) {
        return jwtTokenService.issue(userId, username, role, Set.of(role), Set.of(permission)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
