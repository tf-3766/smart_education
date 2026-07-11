# Database Bootstrap Change Register

> The project no longer uses Flyway. This file records coordinated changes to the single Bootstrap SQL source.

## Source of Truth

- `backend/edu-biz-service/src/main/resources/db/online_education_bootstrap.sql`
- The script contains the complete MySQL schema, local accounts, and demo data.
- It is executed automatically only when the Compose MySQL data volume is empty.

## Change Rules

- Update the Bootstrap SQL in the same pull request as any Entity, Mapper, or database contract change.
- Review table ownership in `docs/module-ownership.md` before changing another member's tables.
- Validate the script against an empty MySQL 8.4 database before merge.
- Do not use the script to upgrade an environment containing business data. Back up data and prepare a dedicated upgrade plan first.
- AI vector collections, Redis namespaces, prompts, and model configuration belong to `edu-ai-service`; they are not part of the Biz Bootstrap SQL.

## Current Baseline

| File | Owner | Scope | Status |
|---|---|---|---|
| `online_education_bootstrap.sql` | A+B | auth, course, learning, assignment, grade, forum, warning, exam, question, category, announcement, and AI acceptance audit tables with demo data | Active |

## Review Checklist

- [ ] The Bootstrap SQL matches the Entity and Mapper changes.
- [ ] The API or internal contract is reviewed before the SQL is merged.
- [ ] Core tables retain audit fields, logical delete, and optimistic locking unless an exception is documented.
- [ ] Indexes and unique constraints match documented query or integrity needs.
- [ ] The script succeeds on an empty MySQL 8.4 database.
