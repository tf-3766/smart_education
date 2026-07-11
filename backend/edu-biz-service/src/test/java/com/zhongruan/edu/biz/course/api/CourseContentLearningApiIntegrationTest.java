package com.zhongruan.edu.biz.course.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseContentLearningApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherCannotEditAnotherTeachersChapterAndAdminCannotEditLesson() throws Exception {
        String teacher2 = login("teacher2", "t123456");
        String admin = login("admin", "admin123");

        mockMvc.perform(put("/api/v1/teacher/chapters/22001")
                        .header("Authorization", bearer(teacher2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"越权\",\"sortOrder\":10,\"version\":0}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/teacher/lessons/23001")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonUpdateJson(21001, 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lessonCourseMustMatchItsChapterCourse() throws Exception {
        String teacher = login("teacher", "t123456");

        mockMvc.perform(post("/api/v1/teacher/chapters/22001/lessons")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonCreateJson(21003)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void materialCourseChapterAndLessonMustBelongToOneHierarchy() throws Exception {
        String teacher = login("teacher", "t123456");

        mockMvc.perform(post("/api/v1/teacher/courses/21001/materials")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chapterId":"22003",
                                  "lessonId":"23004",
                                  "name":"错误归属资料",
                                  "materialType":"DOCUMENT",
                                  "fileKey":"mock/mismatch.pdf",
                                  "fileSize":1024,
                                  "mimeType":"application/pdf",
                                  "visibility":"LESSON",
                                  "status":"PUBLISHED",
                                  "sortOrder":10
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void studentCannotAccessUnselectedUnpublishedLockedOrForeignContent() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/student/lessons/23004")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/student/lessons/23003")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/student/lessons/23002")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
        mockMvc.perform(get("/api/v1/student/materials/24002")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/student/materials/24003")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void outlineContainsOnlyPublishedAndUnlockedContent() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/student/courses/21001/outline")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapters[*].chapterId", contains("22001")))
                .andExpect(jsonPath("$.data.chapters[*].chapterId", not(contains("22002"))))
                .andExpect(jsonPath("$.data.chapters[0].lessons[*].lessonId", contains("23001")));
    }

    @Test
    void completingLessonIsIdempotentAndProgressIsAggregated() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/student/courses/21001/progress")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLessons").value(3))
                .andExpect(jsonPath("$.data.availableLessons").value(1))
                .andExpect(jsonPath("$.data.completedLessons").value(0));

        MvcResult first = mockMvc.perform(post("/api/v1/student/lessons/23001/complete")
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"1004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value("1001"))
                .andExpect(jsonPath("$.data.status.code").value("COMPLETED"))
                .andReturn();
        String recordId = body(first).path("data").path("recordId").asText();

        mockMvc.perform(post("/api/v1/student/lessons/23001/complete")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId));

        mockMvc.perform(get("/api/v1/student/courses/21001/progress")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedLessons").value(1))
                .andExpect(jsonPath("$.data.progressPercent").value(100.0));
    }

    @Test
    void materialAccessReturnsAuthorizedMetadataWithoutStoragePath() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/student/materials/24001")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialId").value("24001"))
                .andExpect(jsonPath("$.data.accessMode").value("MOCK_METADATA_ONLY"))
                .andExpect(jsonPath("$.data.fileKey").doesNotExist())
                .andExpect(jsonPath("$.data.internalPath").doesNotExist());
    }

    @Test
    void deletingChapterDoesNotDeleteHistoricalLearningRecord() throws Exception {
        String student = login("student", "123456");
        String teacher = login("teacher", "t123456");
        mockMvc.perform(post("/api/v1/student/lessons/23001/complete")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teacher/chapters/22001")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM edu_lesson_learning_record WHERE lesson_id = 23001 AND student_id = 1001",
                Integer.class));
    }

    @Test
    void teacherContentCrudAndStudentStartFormAWorkingVerticalSlice() throws Exception {
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");

        MvcResult chapterResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/chapters")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新增章节\",\"description\":\"集成测试\",\"sortOrder\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status.code").value("DRAFT"))
                .andReturn();
        String chapterId = body(chapterResult).path("data").path("chapterId").asText();

        mockMvc.perform(put("/api/v1/teacher/chapters/{chapterId}", chapterId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新增章节（已更新）\",\"description\":\"集成测试\",\"sortOrder\":30,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1));

        mockMvc.perform(post("/api/v1/teacher/chapters/{chapterId}/publish", chapterId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("PUBLISHED"));

        MvcResult lessonResult = mockMvc.perform(post("/api/v1/teacher/chapters/{chapterId}/lessons", chapterId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonCreateJson(21001)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status.code").value("DRAFT"))
                .andReturn();
        String lessonId = body(lessonResult).path("data").path("lessonId").asText();

        mockMvc.perform(put("/api/v1/teacher/lessons/{lessonId}", lessonId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonUpdateJson(21001, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1));

        mockMvc.perform(post("/api/v1/teacher/lessons/{lessonId}/publish", lessonId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("PUBLISHED"));

        MvcResult materialResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/materials")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chapterId":"%s",
                                  "lessonId":"%s",
                                  "name":"新增资料",
                                  "materialType":"DOCUMENT",
                                  "fileKey":"mock/integration.pdf",
                                  "fileSize":2048,
                                  "mimeType":"application/pdf",
                                  "visibility":"LESSON",
                                  "status":"PUBLISHED",
                                  "sortOrder":20
                                }
                                """.formatted(chapterId, lessonId)))
                .andExpect(status().isCreated())
                .andReturn();
        String materialId = body(materialResult).path("data").path("materialId").asText();

        mockMvc.perform(get("/api/v1/teacher/courses/21001/materials")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].materialId", hasItem(materialId)));

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/start", lessonId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value("1001"))
                .andExpect(jsonPath("$.data.status.code").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/student/materials/{materialId}", materialId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialId").value(materialId));

        mockMvc.perform(delete("/api/v1/teacher/materials/{materialId}", materialId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/teacher/lessons/{lessonId}", lessonId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/teacher/chapters/{chapterId}", chapterId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());
    }

    private String validLessonCreateJson(long courseId) {
        return """
                {
                  "courseId":"%d",
                  "title":"新课时",
                  "contentType":"RICH_TEXT",
                  "content":"# Markdown 内容",
                  "estimatedMinutes":20,
                  "sortOrder":30,
                  "unlockType":"IMMEDIATE"
                }
                """.formatted(courseId);
    }

    private String validLessonUpdateJson(long courseId, int version) {
        return """
                {
                  "courseId":"%d",
                  "title":"更新课时",
                  "contentType":"RICH_TEXT",
                  "content":"# Markdown 内容",
                  "estimatedMinutes":20,
                  "sortOrder":10,
                  "unlockType":"IMMEDIATE",
                  "version":%d
                }
                """.formatted(courseId, version);
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
