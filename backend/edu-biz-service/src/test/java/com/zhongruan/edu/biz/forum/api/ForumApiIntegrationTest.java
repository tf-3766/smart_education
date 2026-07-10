package com.zhongruan.edu.biz.forum.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ForumApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void courseMemberPostsRepliesAndTeacherAdminModerateContent() throws Exception {
        String student = login("student", "Student@123");
        String teacher = login("teacher", "Teacher@123");
        String admin = login("admin", "Admin@123");

        MvcResult topicResult = mockMvc.perform(post("/api/v1/student/courses/21001/forum/topics")
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"A3 forum topic",
                                  "content":"Question about lesson one."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status.code").value("VISIBLE"))
                .andReturn();
        JsonNode topic = body(topicResult).path("data");
        String topicId = topic.path("topicId").asText();
        int topicVersion = topic.path("version").asInt();

        MvcResult replyResult = mockMvc.perform(post("/api/v1/student/forum/topics/{topicId}/replies", topicId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"I have the same question.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status.code").value("VISIBLE"))
                .andReturn();
        String replyId = body(replyResult).path("data").path("replyId").asText();
        int replyVersion = body(replyResult).path("data").path("version").asInt();

        mockMvc.perform(get("/api/v1/student/courses/21001/forum/topics")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].topicId", hasItem(topicId)));

        mockMvc.perform(get("/api/v1/student/courses/21003/forum/topics")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());

        MvcResult hiddenReply = mockMvc.perform(patch("/api/v1/teacher/forum/replies/{replyId}/visibility", replyId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false,\"reason\":\"off topic\",\"version\":%d}".formatted(replyVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("HIDDEN"))
                .andExpect(jsonPath("$.data.moderationReason").value("off topic"))
                .andExpect(jsonPath("$.data.moderatedBy").value("1002"))
                .andExpect(jsonPath("$.data.moderatedAt").exists())
                .andReturn();

        mockMvc.perform(post("/api/v1/student/forum/topics/{topicId}/replies", topicId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentReplyId\":\"%s\",\"content\":\"child of hidden reply\"}".formatted(replyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORUM_PARENT_REPLY_INVALID"));

        mockMvc.perform(get("/api/v1/student/forum/topics/{topicId}/replies", topicId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].replyId", not(hasItem(replyId))));

        MvcResult hiddenTopic = mockMvc.perform(patch("/api/v1/teacher/forum/topics/{topicId}/visibility", topicId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false,\"reason\":\"off topic\",\"version\":%d}".formatted(topicVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("HIDDEN"))
                .andExpect(jsonPath("$.data.moderationReason").value("off topic"))
                .andExpect(jsonPath("$.data.moderatedBy").value("1002"))
                .andReturn();

        mockMvc.perform(get("/api/v1/student/forum/topics/{topicId}", topicId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/student/forum/topics/{topicId}/replies", topicId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"reply after hidden\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORUM_TOPIC_HIDDEN"));

        int hiddenTopicVersion = body(hiddenTopic).path("data").path("version").asInt();
        mockMvc.perform(patch("/api/v1/admin/forum/topics/{topicId}/visibility", topicId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":true,\"reason\":\"restored\",\"version\":%d}".formatted(hiddenTopicVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("VISIBLE"))
                .andExpect(jsonPath("$.data.moderationReason").value("restored"))
                .andExpect(jsonPath("$.data.moderatedBy").value("1003"));

        int hiddenReplyVersion = body(hiddenReply).path("data").path("version").asInt();
        mockMvc.perform(patch("/api/v1/admin/forum/replies/{replyId}/visibility", replyId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":true,\"reason\":\"restored\",\"version\":%d}".formatted(hiddenReplyVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value("VISIBLE"))
                .andExpect(jsonPath("$.data.moderationReason").value("restored"))
                .andExpect(jsonPath("$.data.moderatedBy").value("1003"));
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
