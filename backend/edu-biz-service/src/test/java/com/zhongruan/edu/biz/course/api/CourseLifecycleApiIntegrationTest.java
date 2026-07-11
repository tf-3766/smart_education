package com.zhongruan.edu.biz.course.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class CourseLifecycleApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedCourseEndpointsRejectAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/student/courses"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/course-reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateCourseAndTeacherCannotReviewCourse() throws Exception {
        String studentToken = login("student", "123456");
        String teacherToken = login("teacher", "t123456");

        mockMvc.perform(post("/api/v1/teacher/courses")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCourseJson("STUDENT-FORBIDDEN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/teacher/courses/21001")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCourseJson("学生越权修改", 0)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/teacher/courses/21001/publish")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/course-reviews/21001/approve")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"not allowed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotReadOrEditAnotherTeachersCourse() throws Exception {
        String teacherToken = login("teacher", "t123456");

        mockMvc.perform(get("/api/v1/teacher/courses/21003")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/teacher/courses/21003")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCourseJson("越权修改", 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateCourseAndIsTheOnlyOwnerRelationship() throws Exception {
        String token = login("teacher", "t123456");
        String courseId = createCourse(token, "CREATE-OWNER-001");

        mockMvc.perform(get("/api/v1/teacher/courses/{courseId}/teachers", courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].teacherId").value("1002"))
                .andExpect(jsonPath("$.data[0].role.code").value("OWNER"));
    }

    @Test
    void ownerCanAddCollaboratorButDuplicateRelationshipIsRejected() throws Exception {
        String token = login("teacher", "t123456");
        String courseId = createCourse(token, "ADD-COLLAB-001");

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/teachers", courseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":\"1004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role.code").value("COLLABORATOR"));

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/teachers", courseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":\"1004\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void reviewApprovalAndPublicationFormAnExplicitLifecycle() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String adminToken = login("admin", "admin123");
        String courseId = createCourse(teacherToken, "LIFECYCLE-001");

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/publish", courseId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/submit-review", courseId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.reviewStatus.code").value("PENDING"));

        mockMvc.perform(post("/api/v1/admin/course-reviews/{courseId}/approve", courseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"资料完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus.code").value("APPROVED"));

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/publish", courseId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("PUBLISHED"));
    }

    @Test
    void rejectRequiresReasonAndReturnsCourseToEditableDraft() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String adminToken = login("admin", "admin123");
        String courseId = createCourse(teacherToken, "REJECT-001");
        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/submit-review", courseId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/course-reviews/{courseId}/reject", courseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/admin/course-reviews/{courseId}/reject", courseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"课程简介不完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus.code").value("REJECTED"));

        mockMvc.perform(put("/api/v1/teacher/courses/{courseId}", courseId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCourseJson("修改后的课程", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("修改后的课程"));
    }

    @Test
    void studentEnrollmentIsStateCheckedAndIdempotent() throws Exception {
        String token = login("student", "123456");

        mockMvc.perform(post("/api/v1/student/courses/21002/enroll")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/student/courses/21004/enroll")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        MvcResult first = mockMvc.perform(post("/api/v1/student/courses/21003/enroll")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("ENROLLED"))
                .andReturn();
        String enrollmentId = body(first).path("data").path("enrollmentId").asText();

        mockMvc.perform(post("/api/v1/student/courses/21003/enroll")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollmentId").value(enrollmentId));

        mockMvc.perform(get("/api/v1/student/courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].courseId", hasItem("21003")));
    }

    private String createCourse(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/teacher/courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCourseJson(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status.code").value("DRAFT"))
                .andExpect(jsonPath("$.data.reviewStatus.code").value("NOT_SUBMITTED"))
                .andReturn();
        return body(result).path("data").path("courseId").asText();
    }

    private String createCourseJson(String code) {
        return """
                {
                  "courseCode":"%s",
                  "name":"课程 %s",
                  "summary":"用于集成测试",
                  "term":"2026-FALL",
                  "department":"计算机学院",
                  "credit":3.0,
                  "enrollmentOpenAt":"2020-01-01T00:00:00+08:00",
                  "enrollmentCloseAt":"2099-12-31T23:59:59+08:00",
                  "startAt":"2020-01-01T00:00:00+08:00",
                  "endAt":"2099-12-31T23:59:59+08:00"
                }
                """.formatted(code, code);
    }

    private String updateCourseJson(String name, int version) {
        return """
                {
                  "name":"%s",
                  "summary":"更新后的简介",
                  "term":"2026-FALL",
                  "department":"计算机学院",
                  "credit":3.0,
                  "enrollmentOpenAt":"2020-01-01T00:00:00+08:00",
                  "enrollmentCloseAt":"2099-12-31T23:59:59+08:00",
                  "startAt":"2020-01-01T00:00:00+08:00",
                  "endAt":"2099-12-31T23:59:59+08:00",
                  "version":%d
                }
                """.formatted(name, version);
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
