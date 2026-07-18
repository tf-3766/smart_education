# Real Backend Integration Design

**Date:** 2026-07-13

**Goal:** Replace the Vue demo application's Mock-backed runtime with the running Gateway/Biz/AI services for all student, teacher, and administrator pages.

## Scope

The frontend will use `http://localhost:18080` as the only API origin. The Gateway routes authenticated requests to Biz (`18081`) and AI (`18082`). All three role areas are included:

- Authentication, current-user restoration, logout, role-aware route guards.
- Student courses, lessons, assignments, exams, grades, warnings, and forum.
- Teacher dashboard, course management, grading, question banks/exams, warnings, and forum.
- Administrator dashboard/statistics, users, course reviews, and content governance.
- Implemented AI course Q&A SSE, lesson-summary drafts, and AI status.

Mock data remains available only for isolated unit tests and the explicit demo mode. `VITE_API_MODE=real` never falls back to Mock data after a request failure.

## Architecture

Existing `src/services/api/*` modules remain the contract boundary. Pages use small domain adapters that map `PageResponse`, `CodeLabel`, and backend DTOs into the current presentation models. The adapters own loading requests, pagination, optimistic-lock versions, and post-mutation refreshes; Vue components remain focused on rendering and user actions.

The session store becomes backend-owned:

1. Login submits username/password to `/api/v1/auth/login`.
2. The returned JWT and `CurrentUserVO` are persisted.
3. App startup calls `/api/v1/auth/me` when a token exists.
4. A `401` or expired token clears storage and redirects to `/login`.
5. `STUDENT`, `TEACHER`, `ADMIN`, and `SUPER_ADMIN` map to the existing frontend role routes.
6. The current preview-role selector becomes a real “切换账号” action that logs out and returns to the login form.

## Data Rules

- IDs are treated as strings in the frontend, including URL parameters.
- Statuses use backend `CodeLabel.code`; Chinese labels are presentation-only.
- Every write sends the latest `version` returned by the backend.
- Lists use server pagination and expose loading, empty, error, and retry states.
- Dashboard aggregates are composed from real list/statistics endpoints. Unsupported chart series show an explicit empty state instead of fabricated values.
- AI features that have no public backend contract remain disabled with an explanatory state; they do not call legacy Mock services.
- Bootstrap account display names must be UTF-8 when shown through the API. The live local database is corrected without changing the public contract; the bootstrap SQL remains the source for clean environments.

## Error and Security Handling

- `401`/`TOKEN_EXPIRED`: clear token/session and navigate to login.
- `403`: navigate to the existing forbidden page.
- `409`: show a conflict message and reload the affected detail/list so a stale version cannot overwrite newer data.
- Network and `5xx`: preserve the current route, show a retry action, and keep the response error/trace ID available for diagnostics.
- Do not log passwords, JWTs, or full request bodies.
- Preserve `X-Trace-Id` on every request and show it only in diagnostic error details.

## Verification

- Vitest tests cover real-mode request construction, authentication persistence, role mapping, adapter field conversion, pagination, and error mapping.
- A local smoke script logs in as `student`, `teacher`, and `admin`, calls representative read endpoints, and verifies the response envelope and role authorization.
- Reversible write checks cover student enroll/withdraw, assignment draft save, and warning dry-run where seed data permits.
- Browser checks visit every role route with real mode enabled and confirm that page text comes from API responses, no legacy Mock service is called, and loading/error/empty states render correctly.
- `npm test`, `npm run build`, and `git diff --check` must pass before completion.
