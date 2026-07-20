package com.zhongruan.edu.biz.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BootstrapInitializationTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bootstrapScriptCreatesSchemaAndLoadsDemoAccounts() {

        Set<String> tables = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public'",
                String.class));
        assertEquals(37, tables.size());
        assertTrue(tables.containsAll(Set.of(
                "sys_user", "sys_role", "sys_permission", "sys_user_role", "sys_role_permission")));
        assertTrue(tables.containsAll(Set.of(
                "edu_course_category",
                "edu_term_enrollment_window",
                "edu_assignment",
                "edu_assignment_submission",
                "edu_grade_record",
                "edu_forum_topic",
                "edu_learning_warning",
                "edu_exam",
                "edu_question_bank",
                "edu_question",
                "edu_exam_paper",
                "edu_exam_attempt",
                "edu_announcement",
                "edu_notification",
                "edu_notification_read",
                "edu_notification_preference",
                "edu_ai_generation_record")));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class));
        assertEquals(
                4,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_user WHERE username IN ('student','teacher','teacher2','admin')",
                        Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class));
        assertEquals(5, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class));
        assertPasswordMatches("student", "123456");
        assertPasswordMatches("teacher", "t123456");
        assertPasswordMatches("teacher2", "t123456");
        assertPasswordMatches("admin", "admin123");
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_permission WHERE permission_code = 'auth:profile:read'",
                        Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id "
                                + "WHERE ur.user_id = 1003 AND r.role_code = 'SUPER_ADMIN' AND ur.deleted = 0",
                        Integer.class));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                                + "WHERE CONSTRAINT_SCHEMA = 'public' AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                        Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_assignment", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_assignment_submission", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_grade_record", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_exam_attempt", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_course_category", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_term_enrollment_window", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_announcement", Integer.class));
        assertEquals(11, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_notification", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_notification_read", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_notification_preference", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_ai_generation_record", Integer.class));
    }

    private void assertPasswordMatches(String username, String rawPassword) {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username = ?", String.class, username);
        assertTrue(passwordHash != null && passwordHash.startsWith("$2"));
        assertTrue(passwordEncoder.matches(rawPassword, passwordHash));
    }
}
