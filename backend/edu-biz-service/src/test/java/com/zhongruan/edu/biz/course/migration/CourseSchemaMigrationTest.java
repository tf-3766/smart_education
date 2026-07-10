package com.zhongruan.edu.biz.course.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CourseSchemaMigrationTest {
    private static final Set<String> COURSE_TABLES = Set.of(
            "edu_course",
            "edu_course_teacher",
            "edu_course_enrollment",
            "edu_course_chapter",
            "edu_course_lesson",
            "edu_course_material",
            "edu_lesson_learning_record",
            "edu_course_review");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bootstrapScriptCreatesCourseSchemaWithoutPhysicalForeignKeys() {
        Set<String> tables = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public'",
                String.class));
        assertTrue(tables.containsAll(COURSE_TABLES));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                                + "WHERE CONSTRAINT_SCHEMA = 'public' AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                        Integer.class));
    }

    @Test
    void courseRelationshipsHaveRequiredUniqueConstraints() {
        assertUniqueConstraintExists("edu_course", "UK_COURSE_CODE");
        assertUniqueConstraintExists("edu_course_teacher", "UK_COURSE_TEACHER");
        assertUniqueConstraintExists("edu_course_enrollment", "UK_COURSE_ENROLLMENT");
        assertUniqueConstraintExists("edu_lesson_learning_record", "UK_LESSON_STUDENT");
    }

    @Test
    void allCourseTablesUseTheSharedAuditColumns() {
        for (String table : COURSE_TABLES) {
            Set<String> columns = Set.copyOf(jdbcTemplate.queryForList(
                    "SELECT LOWER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_SCHEMA = 'public' AND LOWER(TABLE_NAME) = ?",
                    String.class,
                    table));
            assertTrue(
                    columns.containsAll(Set.of(
                            "id", "created_at", "created_by", "updated_at", "updated_by", "deleted", "version")),
                    () -> table + " lacks shared audit columns");
        }
    }

    private void assertUniqueConstraintExists(String table, String constraint) {
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                                + "WHERE CONSTRAINT_SCHEMA = 'public' AND LOWER(TABLE_NAME) = ? "
                                + "AND UPPER(CONSTRAINT_NAME) = ? AND CONSTRAINT_TYPE = 'UNIQUE'",
                        Integer.class,
                        table,
                        constraint));
    }
}
