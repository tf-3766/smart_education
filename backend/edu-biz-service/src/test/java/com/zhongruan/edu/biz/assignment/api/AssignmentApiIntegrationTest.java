package com.zhongruan.edu.biz.assignment.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.notification.application.service.NotificationDeadlineScheduler;
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
class AssignmentApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationDeadlineScheduler deadlineScheduler;

    @Test
    void teacherPublishesAssignmentAndEnrolledStudentSubmitsIt() throws Exception {
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId":"23001",
                                  "title":"A1 集成测试作业",
                                  "description":"完成 A1 作业提交闭环",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2099-12-31T23:59:59+08:00",
                                  "attachments":[
                                    {
                                      "name":"说明.pdf",
                                      "fileKey":"mock/a1-guide.pdf",
                                      "fileSize":2048,
                                      "mimeType":"application/pdf",
                                      "sortOrder":10
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentStatus.code").value("DRAFT"))
                .andExpect(jsonPath("$.data.availabilityStatus.code").value("CLOSED"))
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();

        MvcResult publishResult = mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus.code").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.availabilityStatus.code").value("OPEN"))
                .andReturn();
        int publishedVersion = body(publishResult).path("data").path("version").asInt();

        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].title", hasItem("作业已发布：A1 集成测试作业")));

        mockMvc.perform(get("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].assignmentId", hasItem(assignmentId)));

        mockMvc.perform(get("/api/v1/student/courses/21001/assignments")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].assignmentId", hasItem(assignmentId)))
                .andExpect(jsonPath("$.data.records[?(@.assignmentId == '" + assignmentId + "')].graded", hasItem(false)));

        MvcResult draftResult = mockMvc.perform(put("/api/v1/student/assignments/{assignmentId}/submission-draft", assignmentId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"草稿内容",
                                  "fileKey":"mock/submission-draft.docx"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus.code").value("DRAFT"))
                .andReturn();

        int draftVersion = body(draftResult).path("data").path("version").asInt();
        mockMvc.perform(post("/api/v1/student/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", bearer(student))
                        .header("Idempotency-Key", "a1-assignment-submit-" + assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"正式提交内容",
                                  "fileKey":"mock/submission-final.docx",
                                  "version":%d
                                }
                                """.formatted(draftVersion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.submissionStatus.code").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.studentId").value("1001"))
                .andExpect(jsonPath("$.data.submittedAt").exists());

        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].title", hasItem("收到作业提交：A1 集成测试作业")));

        mockMvc.perform(get("/api/v1/student/assignments/{assignmentId}", assignmentId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignment.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.data.submission.submissionStatus.code").value("SUBMITTED"));

        mockMvc.perform(put("/api/v1/teacher/assignments/{assignmentId}", assignmentId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId":"23001",
                                  "title":"A1 updated title",
                                  "description":"Must not change grading basis after a submission",
                                  "maxScore":120,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2099-12-31T23:59:59+08:00",
                                  "version":%d
                                }
                                """.formatted(publishedVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/student/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"重复提交\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void assignmentSubmissionChecksResourceScopeAndDeadline() throws Exception {
        String teacher = login("teacher", "t123456");
        String otherTeacher = login("teacher2", "t123456");
        String student = login("student", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"已截止作业",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2020-01-02T00:00:00+08:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();

        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(otherTeacher)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availabilityStatus.code").value("OVERDUE"));

        deadlineScheduler.publishDueAssignmentNotifications();

        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].sourceType", hasItem("ASSIGNMENT_DEADLINE")))
                .andExpect(jsonPath("$.data.records[*].assignmentId", hasItem(assignmentId)));
        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].sourceType", hasItem("ASSIGNMENT_DEADLINE")))
                .andExpect(jsonPath("$.data.records[*].assignmentId", hasItem(assignmentId)));

        mockMvc.perform(post("/api/v1/student/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"逾期提交\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        mockMvc.perform(get("/api/v1/student/courses/21003/assignments")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void manuallyClosingAssignmentNotifiesRecipientsWithoutDuplicateDeadlineEvents() throws Exception {
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/assignments")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"手动截止通知测试",
                                  "maxScore":100,
                                  "openAt":"2020-01-01T00:00:00+08:00",
                                  "dueAt":"2099-12-31T23:59:59+08:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = body(createResult).path("data").path("assignmentId").asText();

        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/teacher/assignments/{assignmentId}/close", assignmentId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus.code").value("CLOSED"));

        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.assignmentId == '" + assignmentId
                        + "' && @.sourceType == 'ASSIGNMENT_DEADLINE')]").value(hasSize(1)));
        mockMvc.perform(get("/api/v1/notifications?category=ASSIGNMENT")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.assignmentId == '" + assignmentId
                        + "' && @.sourceType == 'ASSIGNMENT_DEADLINE')]").value(hasSize(1)));
    }

    @Test
    void validationDoesNotEchoSubmissionContent() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(post("/api/v1/student/assignments/31001/submissions")
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "x".repeat(20001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'content')].rejectedValue")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
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
