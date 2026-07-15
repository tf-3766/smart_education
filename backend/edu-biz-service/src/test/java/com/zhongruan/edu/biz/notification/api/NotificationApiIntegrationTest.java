package com.zhongruan.edu.biz.notification.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.notification.application.service.NotificationApplicationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;
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
class NotificationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private NotificationApplicationService notificationService;

    @Test
    void isolatesMessagesForStudentTeacherAndAdministrator() throws Exception {
        String student = login("student", "123456");
        String teacher = login("teacher", "t123456");
        String administrator = login("admin", "admin123");

        JsonNode studentMessages = list(student, null);
        assertTrue(hasTitle(studentMessages, "第一章学习提醒"));
        assertTrue(hasTitle(studentMessages, "系统联调公告"));
        assertTrue(hasTitle(studentMessages, "作业已发布：第一章课后练习"));
        assertTrue(hasTitle(studentMessages, "考试安排已发布：第一章随堂测验"));
        assertTrue(hasTitle(studentMessages, "学习预警：学习进度低于课程节奏"));

        JsonNode teacherMessages = list(teacher, null);
        assertFalse(hasTitle(teacherMessages, "第一章学习提醒"));
        assertTrue(hasTitle(teacherMessages, "系统联调公告"));
        assertTrue(hasTitle(teacherMessages, "收到作业提交：第一章课后练习"));
        assertTrue(hasTitle(teacherMessages, "学生学习预警：学习进度低于课程节奏"));

        JsonNode administratorMessages = list(administrator, null);
        assertFalse(hasTitle(administratorMessages, "第一章学习提醒"));
        assertTrue(hasTitle(administratorMessages, "系统联调公告"));

        String studentNotificationId = findByTitle(studentMessages, "第一章学习提醒").path("notificationId").asText();
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", studentNotificationId)
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void persistsReadStateAndMarksAllMessagesReadIdempotently() throws Exception {
        String student = login("student", "123456");
        JsonNode messages = list(student, null);
        String notificationId = findByTitle(messages, "第一章学习提醒").path("notificationId").asText();
        long initialUnread = unreadCount(student);

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());

        assertEquals(initialUnread - 1, unreadCount(student));
        JsonNode afterRead = list(student, null);
        assertTrue(findByTitle(afterRead, "第一章学习提醒").path("read").asBoolean());
        assertNotNull(findByTitle(afterRead, "第一章学习提醒").path("readAt").textValue());
        assertFalse(hasTitle(list(student, "unread=true"), "第一章学习提醒"));

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        assertEquals(0, unreadCount(student));
    }

    @Test
    void hidesAnnouncementNotificationAfterWithdrawal() throws Exception {
        String student = login("student", "123456");
        String teacher = login("teacher", "t123456");

        MvcResult created = mockMvc.perform(post("/api/v1/teacher/courses/21001/announcements")
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"待撤回站内信\",\"content\":\"撤回后不应展示。\",\"audience\":\"STUDENT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String announcementId = body(created).path("data").path("announcementId").asText();
        assertTrue(hasTitle(list(student, null), "待撤回站内信"));

        mockMvc.perform(post("/api/v1/teacher/announcements/{announcementId}/withdrawal", announcementId)
                        .header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        assertFalse(hasTitle(list(student, null), "待撤回站内信"));
    }

    @Test
    void persistsPreferencesAppliesThemToNewEventsAndArchivesMessages() throws Exception {
        String student = login("student", "123456");

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabledCategories.length()").value(5));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabledCategories\":[\"COURSE\",\"EXAM\",\"WARNING\",\"SYSTEM\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabledCategories.length()").value(4));

        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setCourseId(21001L);
        assignment.setTitle("偏好过滤测试作业");
        assignment.setDescription("不应生成学生消息");
        assignment.setMaxScore(BigDecimal.valueOf(100));
        assignment.setStatus(AssignmentStatus.PUBLISHED.name());
        assignment.setDueAt(LocalDateTime.of(2099, 1, 1, 0, 0));
        assignment.setPublishedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);
        notificationService.publishAssignment(assignment);
        assertFalse(hasTitle(list(student, null), "作业已发布：偏好过滤测试作业"));

        JsonNode messages = list(student, null);
        String notificationId = findByTitle(messages, "第一章学习提醒").path("notificationId").asText();
        long initialUnread = unreadCount(student);
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/archive", notificationId)
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());

        assertFalse(hasTitle(list(student, null), "第一章学习提醒"));
        assertEquals(initialUnread - 1, unreadCount(student));
    }

    @Test
    void opensAuthenticatedRealtimeStreamAndRejectsAnonymousRequests() throws Exception {
        String student = login("student", "123456");

        MvcResult stream = mockMvc.perform(get("/api/v1/notifications/stream")
                        .header("Authorization", bearer(student))
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();
        assertTrue(stream.getResponse().getContentAsString().contains("connected"));

        mockMvc.perform(get("/api/v1/notifications/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode list(String token, String query) throws Exception {
        String path = query == null ? "/api/v1/notifications" : "/api/v1/notifications?" + query;
        MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("records");
    }

    private long unreadCount(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").asLong();
    }

    private boolean hasTitle(JsonNode records, String title) {
        return StreamSupport.stream(records.spliterator(), false)
                .anyMatch(item -> title.equals(item.path("title").asText()));
    }

    private JsonNode findByTitle(JsonNode records, String title) {
        return StreamSupport.stream(records.spliterator(), false)
                .filter(item -> title.equals(item.path("title").asText()))
                .findFirst()
                .orElseThrow();
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
