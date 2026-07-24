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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .andExpect(jsonPath("$.data.materials[*].name", not(hasItem("草稿章节资料"))))
                .andExpect(jsonPath("$.data.chapters[*].title", hasItem("第一章 已发布")))
                .andExpect(jsonPath("$.data.chapters[*].title", not(hasItem("第二章 草稿"))));
    }

    @Test
    void assistantContextGroundsStudentInOwnWarningsAndLearningTasks() throws Exception {
        String token = login("student", "123456");
        jdbcTemplate.update("""
                INSERT INTO edu_lesson_learning_record
                    (id, course_id, chapter_id, lesson_id, student_id, status, last_studied_at, study_seconds,
                     created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (99101, 21001, 22001, 23001, 1001, 'COMPLETED', CURRENT_TIMESTAMP, 1800,
                        CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO edu_grade_record
                    (id, course_id, student_id, source_type, source_id, score, max_score, weight, grade_status,
                     comment, created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (99102, 21001, 1001, 'EXAM', 9999, 20, 100, 10, 'DRAFT',
                        '尚未发布的内部评语', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
                """);

        mockMvc.perform(post("/_internal/v1/ai-context/assistant")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantContextRequest(1001, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("STUDENT"))
                .andExpect(jsonPath("$.data.courses[*]", hasItem(org.hamcrest.Matchers.containsString("已发布测试课程"))))
                .andExpect(jsonPath("$.data.warnings[*]", hasItem(org.hamcrest.Matchers.containsString("学习进度低于课程节奏"))))
                .andExpect(jsonPath("$.data.assignments[*]", hasItem(org.hamcrest.Matchers.containsString("第一章课后练习"))))
                .andExpect(jsonPath("$.data.learningProgress[*]", hasItem(org.hamcrest.Matchers.containsString("学习 1800 秒"))))
                .andExpect(jsonPath("$.data.grades[*]", hasItem(org.hamcrest.Matchers.containsString("88.50/100.00"))))
                .andExpect(jsonPath("$.data.grades[*]", not(hasItem(org.hamcrest.Matchers.containsString("尚未发布的内部评语")))))
                .andExpect(jsonPath("$.data.announcements[*]", hasItem(org.hamcrest.Matchers.containsString("第一章学习提醒"))))
                .andExpect(jsonPath("$.data.forumActivity[*]", hasItem(org.hamcrest.Matchers.containsString("第一章概念讨论"))))
                .andExpect(jsonPath("$.data.notifications[*]", hasItem(org.hamcrest.Matchers.containsString("系统联调公告"))))
                .andExpect(jsonPath("$.data.users").isEmpty())
                .andExpect(jsonPath("$.data.enrollmentWindows[*]", hasItem(org.hamcrest.Matchers.containsString("2026 秋季"))));
    }

    @Test
    void assistantAnnouncementsRespectAudienceForStudentAndTeacher() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO edu_announcement
                    (id, scope_type, course_id, title, content, audience, status, published_at,
                     created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (99103, 'SYSTEM', NULL, '教师内部通知', '仅教师可见', 'TEACHER', 'PUBLISHED', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0)
                """);

        mockMvc.perform(post("/_internal/v1/ai-context/assistant")
                        .header("Authorization", bearer(login("student", "123456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantContextRequest(1001, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcements[*]",
                        not(hasItem(org.hamcrest.Matchers.containsString("教师内部通知")))));
        mockMvc.perform(post("/_internal/v1/ai-context/assistant")
                        .header("Authorization", bearer(login("teacher", "t123456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantContextRequest(1002, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcements[*]",
                        hasItem(org.hamcrest.Matchers.containsString("教师内部通知"))));
    }

    @Test
    void assistantContextOnlyReturnsRequestedFactDomains() throws Exception {
        String token = login("student", "123456");
        mockMvc.perform(post("/_internal/v1/ai-context/assistant")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantContextRequest(1001, "STUDENT", Set.of("GRADES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[*]", hasItem(org.hamcrest.Matchers.containsString("已发布测试课程"))))
                .andExpect(jsonPath("$.data.grades[*]", hasItem(org.hamcrest.Matchers.containsString("88.50/100.00"))))
                .andExpect(jsonPath("$.data.warnings").isEmpty())
                .andExpect(jsonPath("$.data.assignments").isEmpty())
                .andExpect(jsonPath("$.data.announcements").isEmpty())
                .andExpect(jsonPath("$.data.notifications").isEmpty());
    }

    @Test
    void assistantContextLetsAdminReadTermWindowsWithoutStudentWarningDetails() throws Exception {
        String token = login("admin", "admin123");
        jdbcTemplate.update("""
                INSERT INTO sys_user
                    (id, username, password_hash, display_name, user_status, created_at, created_by,
                     updated_at, updated_by, deleted, version)
                VALUES (1099, 'pending.contract', 'unused', '待审核契约教师', 'PENDING',
                        CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 4)
                """);

        mockMvc.perform(post("/_internal/v1/ai-context/assistant")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantContextRequest(1003, "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollmentWindows[*]", hasItem(org.hamcrest.Matchers.containsString("2026 秋季"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString("课程总数"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString(
                        "明细未提供不代表底层表为空"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString(
                        "不得据此判断指标失真"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString(
                        "同一课程代码跨教师或学期多次开课属于正常业务"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString(
                        "DRAFT 未进入审核、OFFLINE 计入课程总数均不构成异常"))))
                .andExpect(jsonPath("$.data.platformMetrics[*]", hasItem(org.hamcrest.Matchers.containsString(
                        "未提供经规则引擎确认的异常信号"))))
                .andExpect(jsonPath("$.data.pendingTeacherCandidates[0].userId").value(1099))
                .andExpect(jsonPath("$.data.pendingTeacherCandidates[0].username").value("pending.contract"))
                .andExpect(jsonPath("$.data.pendingTeacherCandidates[0].displayName").value("待审核契约教师"))
                .andExpect(jsonPath("$.data.pendingTeacherCandidates[0].version").value(4))
                .andExpect(jsonPath("$.data.pendingTeacherCandidates[0].createdAt",
                        org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.data.users[*]", hasItem(org.hamcrest.Matchers.containsString("pending.contract"))))
                .andExpect(jsonPath("$.data.users[*]", hasItem(org.hamcrest.Matchers.containsString("角色 TEACHER"))))
                .andExpect(jsonPath("$.data.users[0]", org.hamcrest.Matchers.startsWith("用户角色汇总：")))
                .andExpect(jsonPath("$.data.warnings").isEmpty());
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
                .andExpect(jsonPath("$.data.summary").value("用于学生学习闭环测试"))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.term").value("2026-FALL"))
                .andExpect(jsonPath("$.data.department").value("计算机学院"))
                .andExpect(jsonPath("$.data.credit").value(3.0))
                .andExpect(jsonPath("$.data.enrollmentOpenAt").value("2020-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.enrollmentCloseAt").value("2099-12-31T23:59:59Z"))
                .andExpect(jsonPath("$.data.startAt").value("2020-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.endAt").value("2099-12-31T23:59:59Z"))
                .andExpect(jsonPath("$.data.version").isNumber())
                .andExpect(jsonPath("$.data.lessons[*].title", hasItem("草稿章节中的课时")))
                .andExpect(jsonPath("$.data.materials[*].name", hasItem("草稿章节资料")))
                .andExpect(jsonPath("$.data.chapters[*].title", hasItem("第二章 草稿")));
    }

    @Test
    void missingManagedFileDoesNotHideTheOtherwiseValidCourseContext() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_file
                    (id, owner_user_id, original_name, object_key, storage_provider, file_size, mime_type,
                     sha256, purpose, file_status, created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (99110, 1002, 'missing.md', 'course_material/missing.md', 'LOCAL', 12, 'text/markdown',
                        'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
                        'COURSE_MATERIAL', 'READY', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO edu_course_material
                    (id, course_id, chapter_id, lesson_id, name, material_type, file_id, file_key, file_url,
                     file_size, mime_type, visibility, status, sort_order,
                     created_at, created_by, updated_at, updated_by, deleted, version)
                VALUES (99111, 21001, 22001, 23001, '缺失文件资料', 'DOCUMENT', 99110, NULL, NULL,
                        12, 'text/markdown', 'LESSON', 'PUBLISHED', 99,
                        CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
                """);

        mockMvc.perform(post("/_internal/v1/ai-context/course")
                        .header("Authorization", bearer(login("teacher", "t123456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextRequest(1002, "TEACHER", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(21001))
                .andExpect(jsonPath("$.data.materials[?(@.name == '缺失文件资料')].extractionStatus",
                        hasItem("FILE_UNAVAILABLE")));
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

    @Test
    void courseTeacherReceivesPurposeSpecificAiContexts() throws Exception {
        String token = login("teacher", "t123456");

        mockMvc.perform(post("/_internal/v1/ai-context/submission")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceContextRequest(1002, "TEACHER", 32001, "GRADING_COMMENT_DRAFT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(21001))
                .andExpect(jsonPath("$.data.assignmentTitle").value("第一章课后练习"))
                .andExpect(jsonPath("$.data.submissionContent").value("学生提交的第一章练习内容。"));

        mockMvc.perform(post("/_internal/v1/ai-context/warning")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceContextRequest(1002, "TEACHER", 36001, "RISK_EXPLANATION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningType").value("PROGRESS_LAG"))
                .andExpect(jsonPath("$.data.evidences[0].metricCode").value("completedLessonRate"));

        mockMvc.perform(post("/_internal/v1/ai-context/paper")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paperContextRequest(1002, "TEACHER", 21001)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseName").value("已发布测试课程"))
                .andExpect(jsonPath("$.data.questions[0].questionId").value(37101))
                .andExpect(jsonPath("$.data.questions[0].stem").value("下列哪一项最符合课程中的核心概念？"));
    }

    @Test
    void purposeSpecificContextRejectsTeacherOutsideCourse() throws Exception {
        String token = login("teacher2", "t123456");

        mockMvc.perform(post("/_internal/v1/ai-context/submission")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceContextRequest(1004, "TEACHER", 32001, "GRADING_COMMENT_DRAFT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String assistantContextRequest(long userId, String roleCode) throws Exception {
        return objectMapper.writeValueAsString(
                new AssistantContextRequest(userId, roleCode, "ai-assistant-context-test-trace", Set.of("ALL")));
    }

    private String assistantContextRequest(long userId, String roleCode, Set<String> domains) throws Exception {
        return objectMapper.writeValueAsString(
                new AssistantContextRequest(userId, roleCode, "ai-assistant-context-test-trace", domains));
    }

    private String contextRequest(long userId, String roleCode, Integer lessonId) throws Exception {
        return objectMapper.writeValueAsString(new ContextRequest(
                userId, roleCode, 21001, lessonId, null, "COURSE_QA", "ai-context-test-trace"));
    }

    private String resourceContextRequest(long userId, String roleCode, long resourceId, String purpose)
            throws Exception {
        return objectMapper.writeValueAsString(
                new ResourceContextRequest(userId, roleCode, resourceId, purpose, "ai-context-test-trace"));
    }

    private String paperContextRequest(long userId, String roleCode, long courseId) throws Exception {
        return objectMapper.writeValueAsString(
                new PaperContextRequest(userId, roleCode, courseId, "PAPER_SUGGESTION", "ai-context-test-trace"));
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

    private record AssistantContextRequest(long userId, String roleCode, String traceId, Set<String> domains) {}

    private record ContextRequest(
            long userId,
            String roleCode,
            long courseId,
            Integer lessonId,
            Long materialId,
            String purpose,
            String traceId) {}

    private record ResourceContextRequest(
            long userId, String roleCode, long resourceId, String purpose, String traceId) {}

    private record PaperContextRequest(
            long userId, String roleCode, long courseId, String purpose, String traceId) {}
}
