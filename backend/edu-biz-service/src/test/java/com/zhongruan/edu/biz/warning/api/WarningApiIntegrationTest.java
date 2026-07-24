package com.zhongruan.edu.biz.warning.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WarningApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherGeneratesWarningsAndHandlesCourseStudentWarning() throws Exception {
        String teacher = login("teacher", "t123456");
        String otherTeacher = login("teacher2", "t123456");
        String student = login("student", "123456");

        createOverdueMissingAssignment(teacher);
        createLowScorePublishedGrade(teacher, student);

        MvcResult generationResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/warnings/generation")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warningTypes":["MISSING_ASSIGNMENT","LOW_SCORE"],
                                  "dryRun":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCount", greaterThanOrEqualTo(2)))
                .andReturn();
        JsonNode warnings = body(generationResult).path("data").path("warnings");
        String warningId = firstWarningId(warnings, "LOW_SCORE");
        int warningVersion = firstWarningVersion(warnings, "LOW_SCORE");

        mockMvc.perform(get("/api/v1/student/warnings")
                        .header("Authorization", bearer(student))
                        .param("courseId", "21001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].warningType.code", hasItem("LOW_SCORE")))
                .andExpect(jsonPath("$.data.records[*].warningType.code", hasItem("MISSING_ASSIGNMENT")));

        mockMvc.perform(get("/api/v1/teacher/courses/21001/warnings")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].warningId", hasItem(warningId)));

        mockMvc.perform(get("/api/v1/notifications?category=WARNING")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].sourceType", hasItem("WARNING_CREATED")));
        mockMvc.perform(get("/api/v1/notifications?category=WARNING")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].sourceType", hasItem("WARNING_CREATED")));

        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].sourceType", hasItem("GRADE_PUBLISHED")));

        mockMvc.perform(get("/api/v1/teacher/courses/21001/warnings")
                        .header("Authorization", bearer(otherTeacher)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/student/warnings/{warningId}", warningId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningId").value(warningId));

        mockMvc.perform(get("/api/v1/teacher/warnings/{warningId}", warningId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningId").value(warningId));

        mockMvc.perform(get("/api/v1/teacher/warnings/{warningId}", warningId)
                        .header("Authorization", bearer(otherTeacher)))
                .andExpect(status().isForbidden());

        String aiInterventionRemark = "AI干预计划".repeat(100);
        mockMvc.perform(post("/api/v1/teacher/warnings/{warningId}/handle", warningId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "action", "HANDLED",
                                "remark", aiInterventionRemark,
                                "version", warningVersion))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningStatus.code").value("HANDLED"))
                .andExpect(jsonPath("$.data.handledBy").value("1002"))
                .andExpect(jsonPath("$.data.handleRemark").value(aiInterventionRemark))
                .andExpect(jsonPath("$.data.handledAt").exists());
    }

    @Test
    void progressWarningIgnoresDraftAndLockedLessons() throws Exception {
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");
        jdbcTemplate.update("UPDATE edu_course_lesson SET estimated_minutes = 0 WHERE id = 23001");

        mockMvc.perform(post("/api/v1/student/lessons/23001/complete")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/teacher/courses/21001/warnings/generation")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warningTypes":["PROGRESS_LAG"],
                                  "studentId":"1001",
                                  "dryRun":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCount").value(0))
                .andExpect(jsonPath("$.data.warnings").isEmpty());
    }

    private void createOverdueMissingAssignment(String teacherToken) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId":"23001",
                                  "title":"A3 overdue warning assignment",
                                  "description":"Should create missing assignment warning",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2020-01-02T00:00:00+08:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();
        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());
    }

    private void createLowScorePublishedGrade(String teacherToken, String studentToken) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId":"23001",
                                  "title":"A3 low score warning assignment",
                                  "description":"Should create low score warning",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2099-12-31T23:59:59+08:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();
        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());
        MvcResult submissionResult = mockMvc.perform(post("/api/v1/student/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"low score submission\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode submission = body(submissionResult).path("data");
        mockMvc.perform(post("/api/v1/teacher/submissions/{submissionId}/grade", submission.path("submissionId").asText())
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score":50,
                                  "maxScore":100,
                                  "teacherComment":"Needs more work.",
                                  "publishNow":true,
                                  "version":%d
                                }
                                """.formatted(submission.path("version").asInt())))
                .andExpect(status().isOk());
    }

    private String firstWarningId(JsonNode warnings, String type) {
        for (JsonNode warning : warnings) {
            if (type.equals(warning.path("warningType").path("code").asText())) {
                return warning.path("warningId").asText();
            }
        }
        throw new IllegalStateException("warning not found: " + type);
    }

    private int firstWarningVersion(JsonNode warnings, String type) {
        for (JsonNode warning : warnings) {
            if (type.equals(warning.path("warningType").path("code").asText())) {
                return warning.path("version").asInt();
            }
        }
        throw new IllegalStateException("warning not found: " + type);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}
}
