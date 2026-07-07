# Flyway uses second-level timestamp versions

We decided that all future production schema changes in `edu-biz-service` use Flyway files named `VyyyyMMddHHmmss__description.sql`, such as `V20260707143025__create_assignment_grade_tables.sql`. Existing shared migrations `V1__init_auth_tables.sql`, `V2__create_course_tables.sql`, and `V3__create_course_review_table.sql` are immutable: do not edit, rename, delete, reorder, or rewrite them after they have been shared.

Second-level timestamps remove the need for two backend members to reserve sequential numbers while working in parallel. Every schema change must be registered in `docs/migration-register.md`, created from the latest `dev`, reviewed by the owning member, and merged back through PR; conflicts are solved by creating a later migration, never by changing history.

