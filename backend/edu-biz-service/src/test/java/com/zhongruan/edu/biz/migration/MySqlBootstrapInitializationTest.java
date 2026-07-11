package com.zhongruan.edu.biz.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
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

    @Test
    void bootstrapScriptRunsAgainstMySqlEight() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertTrue(version.startsWith("8."));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class));
        assertEquals(5, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id "
                                + "WHERE ur.user_id = 1003 AND r.role_code = 'SUPER_ADMIN' AND ur.deleted = 0",
                        Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_assignment", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_exam_attempt", Integer.class));
    }
}
