package com.zhongruan.edu.biz.ai.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiActionApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void administratorPlansConfirmsAndIdempotentlyReplaysTermWindowAction() throws Exception {
        Session admin = login("admin", "admin123");
        Session student = login("student", "123456");
        String parameters = objectMapper.writeValueAsString(Map.of(
                "term", "2031 春季",
                "enrollmentOpenAt", "2031-02-20T09:00:00+08:00",
                "enrollmentCloseAt", "2031-03-05T17:00:00+08:00"));
        String request = actionRequest(
                admin, "platform.term-enrollment-window.upsert", "test-admin-term-2031", parameters);

        MvcResult planned = mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.confirmationPolicy").value("STRONG_CONFIRM"))
                .andExpect(jsonPath("$.data.preview.学期").value("2031 春季"))
                .andReturn();
        String actionId = body(planned).path("data").path("actionId").asText();

        mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionId").value(actionId));

        mockMvc.perform(get("/api/v1/assistant-actions/{actionId}", actionId)
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationText\":\"确认执行\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resourceType").value("TERM_ENROLLMENT_WINDOW"))
                .andExpect(jsonPath("$.data.href").value("/admin/term-enrollment"));

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/v1/admin/term-enrollment-windows")
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].term", hasItem("2031 春季")));
    }

    @Test
    void teacherPlansAndConfirmsAssignmentPublicationThroughSameExecutor() throws Exception {
        Session teacher = login("teacher", "t123456");
        MvcResult draft = mockMvc.perform(post("/_internal/v1/ai-authoring/assignments")
                        .header("Authorization", bearer(teacher.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", teacher.userId(),
                                "roleCode", teacher.activeRole(),
                                "courseId", 21001,
                                "title", "通用动作执行器测试作业",
                                "description", "确认后发布",
                                "maxScore", 100,
                                "dueInDays", 7))))
                .andExpect(status().isOk())
                .andReturn();
        String assignmentId = body(draft).path("data").path("resourceId").asText();
        String parameters = objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId));

        MvcResult planned = mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(teacher.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionRequest(
                                teacher, "course.assignment.publish", "test-publish-" + assignmentId, parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.targetId").value(assignmentId))
                .andReturn();
        String actionId = body(planned).path("data").path("actionId").asText();

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(teacher.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resourceType").value("ASSIGNMENT"))
                .andExpect(jsonPath("$.data.resourceId").value(assignmentId));

        mockMvc.perform(get("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacher.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.assignmentId == '%s')].assignmentStatus.code"
                        .formatted(assignmentId), hasItem("PUBLISHED")))
                .andExpect(jsonPath("$.data.records[?(@.assignmentId == '%s')].source"
                        .formatted(assignmentId), hasItem("AI")));
    }

    @Test
    void cancelledActionCannotExecute() throws Exception {
        Session admin = login("admin", "admin123");
        String parameters = objectMapper.writeValueAsString(Map.of("term", "2031 秋季"));
        MvcResult planned = mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionRequest(
                                admin, "platform.term-enrollment-window.upsert", "test-cancel-term", parameters)))
                .andExpect(status().isOk())
                .andReturn();
        String actionId = body(planned).path("data").path("actionId").asText();

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/cancel", actionId)
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isConflict());
    }

    @Test
    void administratorReviewsTeacherRegistrationWithStrongConfirmation() throws Exception {
        Session admin = login("admin", "admin123");
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ai.review.teacher","password":"teacher123",
                                 "displayName":"AI 审核测试教师","role":"TEACHER"}
                                """))
                .andExpect(status().isAccepted())
                .andReturn();
        String userId = body(registered).path("data").path("userId").asText();
        String parameters = objectMapper.writeValueAsString(Map.of(
                "userId", userId, "decision", "APPROVE"));

        MvcResult planned = mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionRequest(
                                admin, "admin.teacher-registration.review", "test-review-" + userId, parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.confirmationPolicy").value("STRONG_CONFIRM"))
                .andExpect(jsonPath("$.data.preview.审核决定").value("通过教师注册"))
                .andReturn();
        String actionId = body(planned).path("data").path("actionId").asText();

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(admin.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationText\":\"确认执行\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resourceType").value("USER"))
                .andExpect(jsonPath("$.data.resourceId").value(userId));

        login("ai.review.teacher", "teacher123");
    }

    @Test
    void teacherSavesSubmissionGradeOnlyAfterExplicitConfirmation() throws Exception {
        Session teacher = login("teacher", "t123456");
        long assignmentId = 931001L;
        long submissionId = 932001L;
        jdbcTemplate.update("""
                INSERT INTO edu_assignment
                    (id, course_id, lesson_id, title, description, response_mode, max_score, status, source,
                     open_at, due_at, published_at, created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (?, 21001, 23001, 'AI 评分执行器测试作业', '隔离测试', 'TEXT', 100.00,
                        'PUBLISHED', 'AI', CURRENT_TIMESTAMP, DATEADD('DAY', 7, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
                """, assignmentId);
        jdbcTemplate.update("""
                INSERT INTO edu_assignment_submission
                    (id, assignment_id, course_id, student_id, attempt_no, content, status, submitted_at,
                     created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (?, ?, 21001, 1001, 1, 'AI 评分执行器测试提交', 'SUBMITTED', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0, 0)
                """, submissionId, assignmentId);
        String parameters = objectMapper.writeValueAsString(Map.of(
                "submissionId", submissionId,
                "score", 86.5,
                "teacherComment", "概念清楚，建议补充边界条件说明。",
                "publishNow", false));

        MvcResult planned = mockMvc.perform(post("/_internal/v1/ai-actions")
                        .header("Authorization", bearer(teacher.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionRequest(
                                teacher, "course.submission.grade", "test-grade-" + submissionId, parameters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.preview.分数").value("86.5 / 100"))
                .andReturn();
        String actionId = body(planned).path("data").path("actionId").asText();

        org.junit.jupiter.api.Assertions.assertNull(jdbcTemplate.queryForObject(
                "SELECT score FROM edu_assignment_submission WHERE id = ?", java.math.BigDecimal.class, submissionId));

        mockMvc.perform(post("/api/v1/assistant-actions/{actionId}/confirm", actionId)
                        .header("Authorization", bearer(teacher.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resourceType").value("ASSIGNMENT_SUBMISSION"));

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT score FROM edu_assignment_submission WHERE id = ?",
                        java.math.BigDecimal.class,
                        submissionId).compareTo(new java.math.BigDecimal("86.50")));
        org.junit.jupiter.api.Assertions.assertEquals(
                "DRAFT",
                jdbcTemplate.queryForObject(
                        "SELECT grade_status FROM edu_grade_record WHERE source_type='ASSIGNMENT'"
                                + " AND source_id=? AND student_id=1001",
                        String.class,
                        assignmentId));
    }

    private String actionRequest(
            Session session, String capabilityId, String idempotencyKey, String parametersJson) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "userId", session.userId(),
                "roleCode", session.activeRole(),
                "capabilityId", capabilityId,
                "idempotencyKey", idempotencyKey,
                "parametersJson", parametersJson));
    }

    private Session login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = body(result).path("data");
        return new Session(
                data.path("accessToken").asText(),
                data.path("user").path("userId").asLong(data.path("userId").asLong()),
                data.path("user").path("activeRole").asText(data.path("activeRole").asText()));
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(String token, long userId, String activeRole) {}
}
