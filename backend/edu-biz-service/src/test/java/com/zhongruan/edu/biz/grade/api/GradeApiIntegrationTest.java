package com.zhongruan.edu.biz.grade.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GradeApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherGradesPublishesAndStudentSeesPublishedGrade() throws Exception {
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");

        String assignmentId = createPublishedAssignment(teacher, "A2 grading assignment");
        JsonNode submission = submitAssignment(student, assignmentId, "A2 submitted answer");
        String submissionId = submission.path("submissionId").asText();
        int submissionVersion = submission.path("version").asInt();

        MvcResult gradeResult = mockMvc.perform(post("/api/v1/teacher/submissions/{submissionId}/grade", submissionId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score":88.5,
                                  "maxScore":100,
                                  "teacherComment":"Clear structure and correct main points.",
                                  "publishNow":false,
                                  "version":%d
                                }
                                """.formatted(submissionVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus.code").value("GRADED"))
                .andExpect(jsonPath("$.data.gradeStatus.code").value("DRAFT"))
                .andExpect(jsonPath("$.data.gradeId").exists())
                .andReturn();
        JsonNode grade = body(gradeResult).path("data");
        String gradeId = grade.path("gradeId").asText();
        int gradeVersion = grade.path("gradeVersion").asInt();

        mockMvc.perform(get("/api/v1/student/grades")
                        .header("Authorization", bearer(student))
                        .param("courseId", "21001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].gradeId", not(hasItem(gradeId))));

        mockMvc.perform(post("/api/v1/teacher/grades/{gradeId}/publication", gradeId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(gradeVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeStatus.code").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").exists());

        mockMvc.perform(get("/api/v1/student/grades")
                        .header("Authorization", bearer(student))
                        .param("courseId", "21001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].gradeId", hasItem(gradeId)))
                .andExpect(jsonPath("$.data.records[?(@.gradeId == '" + gradeId + "')].score", hasItem(88.50)))
                .andExpect(jsonPath("$.data.records[?(@.gradeId == '" + gradeId + "')].teacherComment",
                        hasItem("Clear structure and correct main points.")));

        mockMvc.perform(get("/api/v1/teacher/assignments/{assignmentId}/statistics", assignmentId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudentCount").value(1))
                .andExpect(jsonPath("$.data.submittedCount").value(1))
                .andExpect(jsonPath("$.data.gradedCount").value(1))
                .andExpect(jsonPath("$.data.publishedGradeCount").value(1))
                .andExpect(jsonPath("$.data.lowScoreCount").value(0));

        mockMvc.perform(get("/api/v1/teacher/courses/21001/grade-statistics")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value("21001"))
                .andExpect(jsonPath("$.data.assignmentCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.gradedRecordCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.averageScoreRate").isNumber())
                .andExpect(jsonPath("$.data.passRate").isNumber());
    }

    @Test
    void gradingRejectsUnrelatedTeacherAndOutOfRangeScore() throws Exception {
        String teacher = login("teacher", "t123456");
        String otherTeacher = login("teacher2", "t123456");
        String student = login("student", "123456");

        String assignmentId = createPublishedAssignment(teacher, "A2 grading guard assignment");
        JsonNode submission = submitAssignment(student, assignmentId, "A2 guard answer");
        String submissionId = submission.path("submissionId").asText();
        int submissionVersion = submission.path("version").asInt();

        mockMvc.perform(post("/api/v1/teacher/submissions/{submissionId}/grade", submissionId)
                        .header("Authorization", bearer(otherTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score":80,
                                  "maxScore":100,
                                  "teacherComment":"Should be forbidden.",
                                  "version":%d
                                }
                                """.formatted(submissionVersion)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/teacher/submissions/{submissionId}/grade", submissionId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score":120,
                                  "maxScore":100,
                                  "teacherComment":"Out of range.",
                                  "version":%d
                                }
                                """.formatted(submissionVersion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GRADE_SCORE_OUT_OF_RANGE"));
    }

    private String createPublishedAssignment(String teacherToken, String title) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId":"23001",
                                  "title":"%s",
                                  "description":"A2 grading workflow",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2099-12-31T23:59:59+08:00"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();
        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());
        return assignmentId;
    }

    private JsonNode submitAssignment(String studentToken, String assignmentId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/student/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"%s",
                                  "fileKey":"mock/a2-submission.docx"
                                }
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).path("data");
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
