package com.zhongruan.edu.biz.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "edu.security.jwt.secret=test-only-jwt-secret-with-at-least-32-bytes",
        "edu.security.jwt.ttl=PT15M",
        "edu.security.jwt.issuer=edu-biz-service-mysql-test"
})
@Testcontainers(disabledWithoutDocker = true)
class MySqlBootstrapInitializationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_education_test")
            .withUsername("edu_test")
            .withPassword("edu_test_password")
            .withInitScript("db/online_education_bootstrap.sql");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void bootstrapScriptRunsAgainstMySqlEight() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertTrue(version.startsWith("8."));
        assertEquals(
                38,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                        Integer.class));
        assertEquals(12, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class));
        assertEquals(5, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class));
        assertPasswordMatches("student", "123456");
        assertPasswordMatches("teacher", "t123456");
        assertPasswordMatches("teacher2", "t123456");
        assertPasswordMatches("admin", "admin123");
        assertPasswordMatches("admin_ops", "admin123");
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id "
                                + "WHERE ur.user_id = 1003 AND r.role_code = 'SUPER_ADMIN' AND ur.deleted = 0",
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
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.table_constraints "
                                + "WHERE table_schema = DATABASE() AND table_name = 'edu_course' "
                                + "AND constraint_name = 'uk_course_code' AND constraint_type = 'UNIQUE'",
                        Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                                + "WHERE table_schema = DATABASE() AND table_name = 'edu_course' "
                                + "AND index_name = 'idx_course_code'",
                        Integer.class));
        assertEquals(
                3,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_file WHERE id IN (22501, 22502, 22503) AND file_status = 'ACTIVE'",
                        Integer.class));
    }

    private void assertPasswordMatches(String username, String rawPassword) {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username = ?", String.class, username);
        assertTrue(passwordHash != null && passwordHash.startsWith("$2"));
        assertTrue(passwordEncoder.matches(rawPassword, passwordHash));
    }
}
