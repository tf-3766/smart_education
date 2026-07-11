package com.zhongruan.edu.biz.platform.api;

import static org.hamcrest.Matchers.hasItem;
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
class PlatformApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void administratorManagesCategoriesAnnouncementsAndStatistics() throws Exception {
        String admin = login("admin", "admin123");
        String teacher = login("teacher", "t123456");
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/course-categories").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", hasItem("1")));

        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/course-categories")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"平台测试分类\",\"sortOrder\":20,\"enabled\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn();
        String categoryId = body(categoryResult).path("data").path("categoryId").asText();
        mockMvc.perform(put("/api/v1/admin/course-categories/{categoryId}", categoryId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"平台测试分类已更新\",\"sortOrder\":30,\"enabled\":true,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("平台测试分类已更新"));

        mockMvc.perform(post("/api/v1/teacher/courses")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseCode":"PLATFORM-CATEGORY-001",
                                  "name":"分类关联测试课程",
                                  "categoryId":"%s"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(categoryId));
        mockMvc.perform(delete("/api/v1/admin/course-categories/{categoryId}", categoryId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        MvcResult courseAnnouncement = mockMvc.perform(post("/api/v1/teacher/courses/21001/announcements")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"课程接口公告\",\"content\":\"请按时学习。\",\"audience\":\"STUDENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scopeType").value("COURSE"))
                .andReturn();
        String courseAnnouncementId = body(courseAnnouncement).path("data").path("announcementId").asText();

        mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"系统接口公告\",\"content\":\"系统公告正文。\",\"audience\":\"ALL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scopeType").value("SYSTEM"));

        mockMvc.perform(get("/api/v1/student/announcements").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].title", hasItem("课程接口公告")))
                .andExpect(jsonPath("$.data.records[*].title", hasItem("系统接口公告")));

        mockMvc.perform(get("/api/v1/teacher/announcements").header("Authorization", bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].title", hasItem("系统接口公告")));

        mockMvc.perform(post("/api/v1/teacher/announcements/{announcementId}/withdrawal", courseAnnouncementId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        mockMvc.perform(get("/api/v1/admin/statistics").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalCourses").isNumber())
                .andExpect(jsonPath("$.data.publishedAnnouncements").isNumber());
    }

    @Test
    void platformGovernanceEnforcesRoleAndCourseOwnership() throws Exception {
        String student = login("student", "123456");
        String otherTeacher = login("teacher2", "t123456");

        mockMvc.perform(get("/api/v1/admin/statistics").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/teacher/courses/21001/announcements")
                        .header("Authorization", bearer(otherTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"越权公告\",\"content\":\"不应创建。\",\"audience\":\"STUDENT\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
