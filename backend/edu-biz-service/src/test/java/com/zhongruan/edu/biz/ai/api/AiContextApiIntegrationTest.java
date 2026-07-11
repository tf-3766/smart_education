package com.zhongruan.edu.biz.ai.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
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
class AiContextApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Test
    void studentContextContainsOnlyActuallyAccessibleLessonsAndMaterials() throws Exception {
        String token = login("student", "123456");

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1001, "STUDENT", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessons[*].title", hasItem("公开课时")))
                .andExpect(jsonPath("$.data.lessons[*].title", not(hasItem("未解锁课时"))))
                .andExpect(jsonPath("$.data.lessons[*].title", not(hasItem("草稿章节中的课时"))))
                .andExpect(jsonPath("$.data.materials[*].name", hasItem("公开课时资料")))
                .andExpect(jsonPath("$.data.materials[*].name", not(hasItem("草稿章节资料"))));
    }

    @Test
    void internalContextRejectsForgedIdentityAndLockedLesson() throws Exception {
        String token = login("student", "123456");

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1002, "TEACHER", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1001, "STUDENT", 23002)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void courseTeacherCanReceiveDraftContextForSummaryGeneration() throws Exception {
        String token = login("teacher", "t123456");

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1002, "TEACHER", 23003)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherMember").value(true))
                .andExpect(jsonPath("$.data.lessons[*].title", hasItem("草稿章节中的课时")))
                .andExpect(jsonPath("$.data.materials[*].name", hasItem("草稿章节资料")));
    }

    @Test
    void studentContextUsesTheSameOngoingCoursePermissionAsLearningApis() throws Exception {
        String token = login("student", "123456");
        CourseEntity course = courseMapper.selectById(21001L);
        course.setStatus(CourseStatus.ONGOING.name());
        courseMapper.updateById(course);

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1001, "STUDENT", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.enrolled").value(true));
    }

    private String contextRequest(long userId, String roleCode, Integer lessonId) throws Exception {
        return objectMapper.writeValueAsString(new ContextRequest(
                userId, roleCode, 21001, lessonId, null, "COURSE_QA", "ai-context-test-trace"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}

    private record ContextRequest(
            long userId,
            String roleCode,
            long courseId,
            Integer lessonId,
            Long materialId,
            String purpose,
            String traceId) {}
}
