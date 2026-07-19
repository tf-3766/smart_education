# Smart Education Demo Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete student, teacher, and administrator demonstration frontend described in the project design document.

**Architecture:** Restore the Vue entry, route domains, session store, reusable shell, and domain services around a persistent local demo repository. Services route through an explicit runtime adapter so demo data is default and gateway integration is enabled only by `VITE_API_MODE=real`. Role-specific pages consume the same shared primitives while implementing their own business workspaces.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Pinia, Frappe UI, Tailwind, Lucide, Vitest, browser runtime verification.

---

### Task 1: Restore application runtime and session contract

**Files:**
- Create: `src/main.ts`, `src/App.vue`, `src/types/domain.ts`, `src/services/runtime.ts`, `src/services/httpClient.ts`, `src/stores/session.ts`, `src/tests/runtime.test.ts`, `src/tests/session.test.ts`
- Modify: `src/router/index.ts`, `src/layouts/AppShell.vue`

- [ ] Write failing tests proving demo mode is default, real mode is opt-in, persisted role is restored, and role switching updates the user.
- [ ] Run `npm test -- src/tests/runtime.test.ts src/tests/session.test.ts` and confirm the failures name missing runtime/session contracts.
- [ ] Implement the typed runtime and persisted Pinia session contract with `login`, `logout`, `switchRole`, `isDemoMode`, and trace-id error normalization.
- [ ] Restore the Vue boot sequence, shell route outlet, unauthenticated route boundary, and role-aware navigation.
- [ ] Re-run the focused tests and commit the restored runtime.

### Task 2: Build persistent local domain repository and service adapters

**Files:**
- Create: `src/mocks/data.ts`, `src/mocks/repository.ts`, `src/services/authService.ts`, `src/services/courseService.ts`, `src/services/assignmentService.ts`, `src/services/examService.ts`, `src/services/gradeService.ts`, `src/services/forumService.ts`, `src/services/aiService.ts`, `src/tests/repository.test.ts`
- Modify: `src/types/domain.ts`

- [ ] Write failing tests for lesson completion, assignment submission, grade publication, course review rejection validation, and AI confirmation not changing formal records.
- [ ] Run `npm test -- src/tests/repository.test.ts` and confirm the expected missing-service failures.
- [ ] Add seed fixtures, cloning/persistence/reset support, typed service methods, and explicit demo labels.
- [ ] Add the real API adapter only to documented, already available auth/course routes; surface any real-mode error instead of falling back.
- [ ] Re-run repository and runtime tests and commit the data boundary.

### Task 3: Implement authentication, routing, and shared visual primitives

**Files:**
- Create: `src/domains/auth/LoginPage.vue`, `src/domains/system/ForbiddenPage.vue`, `src/domains/system/NotFoundPage.vue`, `src/components/AppButton.vue`, `src/components/AppMetric.vue`, `src/components/AppModal.vue`, `src/components/StatusBadge.vue`, `src/components/EmptyState.vue`, `src/components/FChart.vue`
- Modify: `src/styles/index.css`, `src/layouts/AppShell.vue`, `src/router/index.ts`

- [ ] Write failing component/router tests for login selection, role-only navigation, forbidden redirection, and mobile shell behavior.
- [ ] Run the focused test files and confirm they fail before component implementation.
- [ ] Implement the visual primitives and responsive teal shell with notification, account, demo-role switcher, confirmation modal, and error/toast patterns.
- [ ] Restore chart lifecycle cleanup before chart rerender and unmount.
- [ ] Run focused tests, typecheck, and commit shared UI.

### Task 4: Implement student workflows

**Files:**
- Create: `src/domains/student/StudentDashboardPage.vue`, `src/domains/student/StudentCourseListPage.vue`, `src/domains/student/LessonWorkspacePage.vue`, `src/domains/student/StudentAssignmentsPage.vue`, `src/domains/student/StudentGradesPage.vue`, `src/domains/student/StudentForumPage.vue`, `src/domains/student/StudentAiHistoryPage.vue`, `src/tests/student-workflows.test.ts`
- Modify: `src/router/index.ts`, `src/services/courseService.ts`, `src/services/assignmentService.ts`, `src/services/forumService.ts`, `src/services/aiService.ts`

- [ ] Write failing tests for task ordering, lesson completion progress, assignment submit confirmation, AI citation/no-source state, and forum reply persistence.
- [ ] Implement the routes and interaction states using services rather than direct seed imports.
- [ ] Verify the focused test suite, desktop browser path, and 390px lesson workspace.
- [ ] Commit the student flow.

### Task 5: Implement teacher workflows

**Files:**
- Create: `src/domains/teacher/TeacherDashboardPage.vue`, `src/domains/teacher/CourseManagePage.vue`, `src/domains/teacher/GradingWorkspacePage.vue`, `src/domains/teacher/QuestionBankPage.vue`, `src/domains/teacher/WarningsPage.vue`, `src/tests/teacher-workflows.test.ts`
- Modify: `src/domains/exams/ExamPage.vue`, `src/domains/grades/GradeStatisticsPage.vue`, `src/router/index.ts`, `src/services/assignmentService.ts`, `src/services/examService.ts`, `src/services/aiService.ts`

- [ ] Write failing tests for loading a submission, generating/editing/confirming an AI feedback draft, grade publication, paper item addition, and warning handling.
- [ ] Implement the three-column grading workspace and teacher-only actions around current exam/grade changes.
- [ ] Verify tests, desktop browser flow, and stacked mobile workspaces.
- [ ] Commit the teacher flow.

### Task 6: Implement administrator workflows and analytics

**Files:**
- Create: `src/domains/admin/AdminDashboardPage.vue`, `src/domains/admin/UserManagementPage.vue`, `src/domains/admin/CourseReviewPage.vue`, `src/domains/admin/ContentGovernancePage.vue`, `src/domains/admin/AnalyticsPage.vue`, `src/domains/admin/AiOperationsPage.vue`, `src/tests/admin-workflows.test.ts`
- Modify: `src/router/index.ts`, `src/services/courseService.ts`, `src/services/analyticsService.ts`

- [ ] Write failing tests for admin route guards, reject-reason validation, course approval/rejection persistence, and AI health error display.
- [ ] Implement the governance views, operational tables, stateful review decisions, and chart widgets.
- [ ] Verify tests, browser flow, and responsive tables.
- [ ] Commit the administrator flow.

### Task 7: End-to-end verification and documentation

**Files:**
- Modify: `README.md`, `src/tests/*.test.ts`, `docs/superpowers/specs/2026-07-10-smart-education-demo-design.md`

- [ ] Add a quick-start section with default demo mode, `VITE_API_MODE=real`, three demo accounts, reset data, build, and test commands.
- [ ] Run `npm test`, `npm run build`, and `npx vue-tsc --noEmit`.
- [ ] Start Vite, run desktop role/workflow checks and 390px mobile overflow checks in the browser, and resolve all discovered console errors.
- [ ] Commit the verified demonstration frontend.

## Self Review

The tasks cover the agreed full demo scope: shared role-aware shell, persistent demo/default data mode, student learning/AI/assignment/exam/forum flows, teacher assessment/exam/warning flows, administrator governance/analytics/AI health flows, error and responsive states, and verification. No task uses Element Plus or automatic AI writes. The method names used in later tasks are introduced in Tasks 1-2; each page consumes services rather than seed imports.
