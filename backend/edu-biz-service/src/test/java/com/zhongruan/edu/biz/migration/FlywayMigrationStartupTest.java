package com.zhongruan.edu.biz.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationStartupTest {
    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesAuthSchemaAndLoadsLocalAccounts() {
        assertTrue(flyway.info().applied().length >= 2);

        Set<String> tables = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public'",
                String.class));
        assertTrue(tables.containsAll(Set.of(
                "sys_user", "sys_role", "sys_permission", "sys_user_role", "sys_role_permission")));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class));
        assertEquals(
                4,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_user WHERE username IN ('student','teacher','teacher2','admin')",
                        Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_permission WHERE permission_code = 'auth:profile:read'",
                        Integer.class));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                                + "WHERE CONSTRAINT_SCHEMA = 'public' AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                        Integer.class));
        assertEquals("1", flyway.info().applied()[0].getVersion().getVersion());
    }
}
