# Real Backend Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Switch every student, teacher, and administrator page from demo data to the running Gateway/Biz/AI services without silent Mock fallback.

**Architecture:** Keep `src/services/api/*` as the typed HTTP contract boundary. Add small page-facing adapters and a backend-owned session store, then replace each page's direct `@/mocks/data` import with adapter state, loading/error/empty handling, and post-mutation refreshes.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router, Frappe UI, Vitest, Vite, Gateway HTTP API, SSE.

---

### Task 1: Real Session And Request Lifecycle

**Files:**
- Modify: `lms/smart-education-frontend/src/stores/session.ts`
- Modify: `lms/smart-education-frontend/src/domains/auth/LoginPage.vue`
- Modify: `lms/smart-education-frontend/src/layouts/AppShell.vue`
- Modify: `lms/smart-education-frontend/src/router/index.ts`
- Modify: `lms/smart-education-frontend/src/services/api/auth.ts`
- Test: `lms/smart-education-frontend/src/tests/realSession.test.ts`

- [ ] **Step 1: Write failing tests** for login persistence, `/auth/me` restoration, backend role mapping, logout clearing the token, and expired-token redirect.

```ts
it('maps SUPER_ADMIN to the admin route and persists the backend token', async () => {
  const result = await authApi.login({ username: 'admin', password: 'admin123' })
  expect(result.user.activeRole).toBe('SUPER_ADMIN')
  expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe(result.accessToken)
  expect(mapBackendRole(result.user.activeRole)).toBe('admin')
})
```

- [ ] **Step 2: Run the focused test and verify it fails** because `useSessionStore` currently only accepts a frontend `Role` and `LoginPage` only selects a demo role.

Run: `npm test -- --run src/tests/realSession.test.ts`

- [ ] **Step 3: Implement backend-owned session state.** Add `loginWithCredentials`, `restore`, and `logoutRemote` methods; persist `{ authenticated, role, user }`; call `authApi.me()` on restore; map `STUDENT`, `TEACHER`, `ADMIN`, and `SUPER_ADMIN` to the existing routes; remove the preview-role selector and replace it with a real logout/switch-account action.

- [ ] **Step 4: Add the credential form.** `LoginPage.vue` renders username/password inputs, calls `session.loginWithCredentials`, displays `RuntimeError.message`, and routes to `roleHome[session.currentRole]` only after a successful response.

- [ ] **Step 5: Run the focused test and all existing session tests.**

Run: `npm test -- --run src/tests/realSession.test.ts src/tests/session.test.ts`

- [ ] **Step 6: Commit.**

```bash
git add lms/smart-education-frontend/src/stores/session.ts lms/smart-education-frontend/src/domains/auth/LoginPage.vue lms/smart-education-frontend/src/layouts/AppShell.vue lms/smart-education-frontend/src/router/index.ts lms/smart-education-frontend/src/services/api/auth.ts lms/smart-education-frontend/src/tests/realSession.test.ts
git commit -m "feat: use backend authentication in frontend"
```

### Task 2: Shared Real-Mode Adapter And UI States

**Files:**
- Modify: `lms/smart-education-frontend/src/services/runtime.ts`
- Modify: `lms/smart-education-frontend/src/services/httpClient.ts`
- Create: `lms/smart-education-frontend/src/services/pageState.ts`
- Create: `lms/smart-education-frontend/src/components/AsyncState.vue`
- Test: `lms/smart-education-frontend/src/tests/realHttpClient.test.ts`

- [ ] **Step 1: Write failing tests** for `401`, `403`, `409`, network errors, `X-Trace-Id`, and the invariant that real mode never calls demo code.

```ts
it('throws a traceable RuntimeError for a backend conflict', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
    JSON.stringify({ code: 'CONFLICT', message: '版本已变化', traceId: 'trace-1' }),
    { status: 409, headers: { 'Content-Type': 'application/json' } },
  )))
  await expect(request('/api/v1/teacher/courses/101', { method: 'PUT' })).rejects.toMatchObject({ code: 'CONFLICT', traceId: 'trace-1' })
})
```

- [ ] **Step 2: Run the focused test and verify it fails** because the current client only handles `401` specially and pages have no shared async state.

- [ ] **Step 3: Implement `pageState.ts` and `AsyncState.vue`.** Expose `loading`, `error`, `retry`, and `clearError`; render loading text, empty text, and a retry button without changing the existing visual language.

- [ ] **Step 4: Update `httpClient.ts`.** Preserve trace IDs, clear the token for `401` and `TOKEN_EXPIRED`, throw typed `RuntimeError` for all non-2xx responses, and never import or invoke `src/services/api/demo/*` from real requests.

- [ ] **Step 5: Run focused and existing API contract tests.**

Run: `npm test -- --run src/tests/realHttpClient.test.ts src/tests/apiContract.test.ts`

- [ ] **Step 6: Commit.**

### Task 3: Student Pages

**Files:**
- Modify: `lms/smart-education-frontend/src/domains/student/StudentDashboardPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/StudentCourseListPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/LessonWorkspacePage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/StudentAssignmentsPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/StudentExamsPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/StudentGradesPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/student/StudentForumPage.vue`
- Modify: `lms/smart-education-frontend/src/services/api/studentLearning.ts`
- Modify: `lms/smart-education-frontend/src/services/api/assignments.ts`
- Modify: `lms/smart-education-frontend/src/services/api/exams.ts`
- Modify: `lms/smart-education-frontend/src/services/api/forum.ts`
- Modify: `lms/smart-education-frontend/src/services/api/warnings.ts`
- Test: `lms/smart-education-frontend/src/tests/studentAdapters.test.ts`

- [ ] **Step 1: Write failing adapter tests** for course list mapping, outline/lesson mapping, assignment status mapping, published grades, exam attempts, and forum list mapping.

- [ ] **Step 2: Run them red.**

Run: `npm test -- --run src/tests/studentAdapters.test.ts`

- [ ] **Step 3: Implement student adapters** using `studentLearningApi`, `assignmentsApi`, `examsApi`, `forumApi`, and `warningsApi`; map `CodeLabel` and IDs without reading `mocks/data`.

- [ ] **Step 4: Replace page setup state.** Each page loads from its adapter on mount/route change, shows `AsyncState`, and refreshes after enroll, withdraw, save draft, submit, complete lesson, submit exam, create topic, or reply.

- [ ] **Step 5: Wire lesson Q&A to `aiApi.qaStream`** and update the answer incrementally from `delta` events; use `studentLearningApi.startLesson` and `completeLesson` for progress.

- [ ] **Step 6: Run student tests and commit.**

### Task 4: Teacher Pages

**Files:**
- Modify: `lms/smart-education-frontend/src/domains/teacher/TeacherDashboardPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/CourseManagePage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/GradingWorkspacePage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/QuestionBankPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/WarningsPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/TeacherForumPage.vue`
- Modify: `lms/smart-education-frontend/src/services/api/teacherCourses.ts`
- Modify: `lms/smart-education-frontend/src/services/api/courseContent.ts`
- Modify: `lms/smart-education-frontend/src/services/api/assignments.ts`
- Modify: `lms/smart-education-frontend/src/services/api/exams.ts`
- Modify: `lms/smart-education-frontend/src/services/api/warnings.ts`
- Modify: `lms/smart-education-frontend/src/services/api/forum.ts`
- Test: `lms/smart-education-frontend/src/tests/teacherAdapters.test.ts`

- [ ] **Step 1: Write failing adapter tests** for teacher courses, course content, assignments/submissions, question banks, warnings, and forum moderation.

- [ ] **Step 2: Run them red.**

Run: `npm test -- --run src/tests/teacherAdapters.test.ts`

- [ ] **Step 3: Implement teacher adapters** with server pagination and latest `version` values.

- [ ] **Step 4: Replace dashboard aggregate cards** with real course/assignment/warning queries; remove fixed counts and “示例数据” text. Unsupported trend data renders an empty chart state.

- [ ] **Step 5: Wire all mutations**: create/update/submit-review/publish/offline course, create/update/publish/close assignment, grade/publish grade, question/exam/paper operations, warning generation/handling, and forum visibility.

- [ ] **Step 6: Run teacher tests and commit.**

### Task 5: Administrator Pages

**Files:**
- Modify: `lms/smart-education-frontend/src/domains/admin/AdminDashboardPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/admin/AnalyticsPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/admin/UserManagementPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/admin/CourseReviewPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/admin/ContentGovernancePage.vue`
- Modify: `lms/smart-education-frontend/src/services/api/adminStatistics.ts`
- Modify: `lms/smart-education-frontend/src/services/api/adminUsers.ts`
- Modify: `lms/smart-education-frontend/src/services/api/courseReviews.ts`
- Modify: `lms/smart-education-frontend/src/services/api/categories.ts`
- Modify: `lms/smart-education-frontend/src/services/api/forum.ts`
- Modify: `lms/smart-education-frontend/src/services/api/announcements.ts`
- Test: `lms/smart-education-frontend/src/tests/adminAdapters.test.ts`

- [ ] **Step 1: Write failing tests** for statistics, users, teacher approval, course review, categories, announcements, and moderation mapping.

- [ ] **Step 2: Run them red.**

- [ ] **Step 3: Replace admin Mock imports** with real adapters and explicit unsupported-state rendering for unavailable AI index metrics.

- [ ] **Step 4: Wire approval, rejection, grants, revocation, review, category, announcement, and moderation mutations; refresh after every success.**

- [ ] **Step 5: Run admin tests and commit.**

### Task 6: AI And Encoding Integration

**Files:**
- Modify: `lms/smart-education-frontend/src/services/api/ai.ts`
- Modify: `lms/smart-education-frontend/src/domains/student/LessonWorkspacePage.vue`
- Modify: `lms/smart-education-frontend/src/domains/teacher/QuestionBankPage.vue`
- Modify: `lms/smart-education-frontend/src/domains/admin/AdminDashboardPage.vue`
- Create: `smart_education/backend/scripts/repair-bootstrap-display-names.sql`
- Test: `lms/smart-education-frontend/src/tests/aiRealMode.test.ts`

- [ ] **Step 1: Write failing tests** proving real mode uses SSE/JSON AI endpoints and never returns demo drafts for unsupported operations.

- [ ] **Step 2: Run them red.**

- [ ] **Step 3: Keep only implemented real AI paths** (`qaStream`, `lessonSummaryDraft`, `adminStatus`); render disabled states for comment, warning-explanation, and paper-suggestion actions.

- [ ] **Step 4: Add and execute the local display-name repair SQL** against the running MySQL database using a UTF-8 client, then verify `/api/v1/auth/me` returns readable Chinese. Do not alter the bootstrap SQL because it is already UTF-8-correct.

- [ ] **Step 5: Run AI tests and commit.**

### Task 7: Real Smoke Verification And Browser Acceptance

**Files:**
- Create: `lms/smart-education-frontend/scripts/real-api-smoke.mjs`
- Modify: `lms/smart-education-frontend/.env.local`
- Test: `lms/smart-education-frontend/src/tests/noMockImports.test.ts`

- [ ] **Step 1: Write the no-Mock-import test** that fails if a role page imports `@/mocks/data` or legacy mock-only services.

- [ ] **Step 2: Run it red**, then remove all role-page Mock imports.

- [ ] **Step 3: Implement the smoke script** with `fetch`: login as `student`, `teacher`, and `admin`; call `/auth/me`, one role-specific list endpoint, and one unauthorized endpoint; assert `SUCCESS`, role-specific authorization, and `FORBIDDEN`/`UNAUTHORIZED` where expected.

- [ ] **Step 4: Enable real mode** with `VITE_API_MODE=real` and `VITE_GATEWAY_URL=http://localhost:18080` without committing secrets.

- [ ] **Step 5: Run browser checks** for all role routes, inspect network/DOM state, and verify no Mock data appears.

- [ ] **Step 6: Run final verification and commit.**

```bash
npm test
npm run build
node scripts/real-api-smoke.mjs
git diff --check
```

Expected: all tests pass, Vite build succeeds, smoke checks report three successful logins and role authorization, and no diff-check output is produced.
