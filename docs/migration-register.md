# Flyway Migration Register

> Purpose: prevent two backend members from creating conflicting schema migrations. This register documents ownership and intent; it does not replace the actual SQL migration files.

## Rules

- Production migrations live in `backend/edu-biz-service/src/main/resources/db/migration/`.
- New production migrations must use second-level timestamp versions:

```text
VyyyyMMddHHmmss__short_description.sql
```

- Example:

```text
V20260707143025__create_assignment_grade_tables.sql
```

- Existing `V1` to `V3` migrations are immutable. Do not edit, rename, delete, squash, or reorder them.
- Create migrations only from the latest `dev`.
- Register the intended migration before opening the PR.
- If two migrations collide or overlap, the later PR creates a newer timestamp migration.
- Local demo data belongs in `backend/edu-biz-service/src/main/resources/db/localmigration/`, not in production migration files.
- AI vector collections, Redis namespaces, prompts, and model configuration are owned by `edu-ai-service`; they are not Biz Flyway migrations.

## Existing Shared Migrations

| Version | File | Owner | Scope | Status |
|---|---|---|---|---|
| `V1` | `V1__init_auth_tables.sql` | Member A | `sys_user`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission` | Shared, immutable |
| `V2` | `V2__create_course_tables.sql` | Member A | `edu_course`, `edu_course_teacher`, `edu_course_enrollment`, `edu_course_chapter`, `edu_course_lesson`, `edu_course_material`, `edu_lesson_learning_record` | Shared, immutable |
| `V3` | `V3__create_course_review_table.sql` | Member A | `edu_course_review` | Shared, immutable |
| `V20260709110000` | `V20260709110000__create_learning_collaboration_tables.sql` | Member A+B | assignment, submission, grade, forum, warning, exam, question, paper, attempt, AI acceptance audit | Added after contract/schema refresh |

## Planned Migration Slots

These are planning slots only. Do not create the SQL until the corresponding API contract and table design are reviewed.

| Planned file pattern | Owner | Target scope | Prerequisite |
|---|---|---|---|
| `VyyyyMMddHHmmss__extend_assignment_rubric_tables.sql` | Member A | rubric, rubric item, submission rubric score | grading rubric contract reviewed |
| `VyyyyMMddHHmmss__create_notice_tables.sql` | Member A | course notice, system notice | notice contract reviewed |
| `VyyyyMMddHHmmss__extend_exam_session_tables.sql` | Member B | exam session, autosave, proctor log, auto grading rules | exam session ADR reviewed |
| `VyyyyMMddHHmmss__extend_ai_audit_tables.sql` | Member A+B | AI citation persistence or adoption history extensions | ADR 0001 boundary and relevant Biz workflow reviewed |

## Review Checklist

- [ ] File name uses `VyyyyMMddHHmmss__description.sql`.
- [ ] Migration is created from latest `dev`.
- [ ] The table owner is listed in `docs/module-ownership.md`.
- [ ] The API or internal contract is reviewed before SQL is merged.
- [ ] No shared historical migration is modified.
- [ ] All core tables include audit fields, logical delete, and optimistic lock unless the PR explains an approved exception.
- [ ] Indexes and unique constraints match documented query or integrity needs.
- [ ] Empty database migration and upgrade-from-current-schema checks are planned for the implementation PR.
