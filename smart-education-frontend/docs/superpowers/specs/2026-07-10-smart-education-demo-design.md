# Smart Education Demo Frontend Design

## Goal

Deliver a complete, locally runnable demonstration frontend for the online education assistance system described in `/Users/yulang/Downloads/在线教育辅助教学系统_项目设计文档_第一组.md`. The application must demonstrate the student, teacher, and administrator workflows with stable sample data while retaining an explicit path for gateway-backed integration.

## Constraints And Decisions

- Retain Vue 3, Vite, TypeScript, Pinia, Frappe UI, Tailwind, Lucide. Do not add Element Plus.
- **Visual direction (2026-07-10 revision): 腾讯云控制台风格.** Brand primary is Tencent-Cloud blue `#1468e8` (hover `#0c58ca`, weak `#eaf2ff`), per `teaching-workbench-demo.html`; semantic green/amber/red kept distinct from brand. White panels + 1px borders (no shadows), table-first, information-dense. Dark mode is dropped. This supersedes the earlier teal direction in `UI设计规范.dc.html`.
- Rebuild the three-end structure on the committed baseline (`dcf5975`); the earlier uncommitted rewrite was discarded per user decision. Reuse committed types, mock data, and shared components; restyle everything to the blue console system.
- Default to demo data. `VITE_API_MODE=real` is the only opt-in for gateway requests. Real-mode failures must surface a readable error and trace identifier; the application must not silently fall back to demo data.
- Provide demo login credentials for student, teacher, and administrator. Keep a role switcher in the authenticated top bar for presentation use. Switching roles updates navigation, permitted routes, visible data, and default landing page.
- Persist session role, user, and demo mutations in browser storage for a consistent demo after refresh.

## Information Architecture

The authenticated app uses one responsive shell: desktop sidebar, compact mobile navigation, top-level notification entry, role switcher, and account menu. It has three route domains.

| Domain | Primary routes | First-screen priority |
| --- | --- | --- |
| Student | dashboard, courses, course lesson, assignments, exams, grades, forum, contextual AI history | tasks, continue learning, upcoming exams, progress, risks |
| Teacher | dashboard, course management, grading, exams/question bank, warnings, forum | publish/grade queue, course operation, risk interventions |
| Administrator | dashboard, users, course reviews, announcements/forum moderation, statistics, AI health | governance queue, platform totals, exceptions, AI operations |

Shared `/login`, `/403`, and `/404` routes remain outside the shell. Role guards are experience safeguards only; the real API adapter preserves server-side authorization as the authoritative boundary.

## Core Demonstration Workflows

### Student Learning

The course lesson page is a desktop three-column workspace: chapter/lesson outline, lesson content and completion action, and an AI Q&A sidebar. The sidebar always displays course/lesson scope and citations or an explicit no-source state. Completing a lesson changes course progress and dashboard task state. Assignment draft/save/submit, examination answers/save/submit, grades, and forum replies are all demo-mutable.

### Teacher Teaching And Assessment

The teacher dashboard exposes operational counts and routes to courses, assignments, exams, warnings, and interactions. Grading is a three-column workspace: submission queue, submitted content/history, and rubric/feedback panel. AI produces an editable comment draft only. A teacher must save or publish the score and comment; the AI result never writes a grade automatically. Teacher users can create/publish course content, create exams, build papers, request a constrained AI paper suggestion, and resolve warning records.

### Administrator Governance

The admin dashboard shows aggregate totals, review counts, moderation workload, and AI health without student private chat content. Course review is a two-column detail-and-decision page that requires a rejection reason. User status, announcements, moderation, statistics, and AI health each expose a demonstration state change and clear status feedback.

## Data And Service Boundary

`src/services/runtime.ts` owns API mode selection, latency simulation, persistence, cloning, and standardized runtime errors. Domain services expose methods consumed by pages and select either local repository operations or real requests. Pages do not directly import seed arrays. The local repository owns mutable data, generates ids, and serializes it to `localStorage`; `resetDemoData()` restores known seed state.

The request client adds `Authorization` and `X-Trace-Id`, interprets `ApiResponse<T>`, and maps 401 to login, 403 to the forbidden view, conflict to a page-visible message, and AI unavailable to an explicitly retryable state. The initial real-mode scope covers already documented authentication and course-learning routes; designed-but-not-yet-backed workflows remain visibly labelled as demo data rather than fabricating real API success.

## Visual And Interaction Design

The product remains a restrained, data-dense campus workbench. Teal marks brand, selected navigation, primary actions, and AI scope/confirmation only. Neutral surfaces, compact panels, tables, state badges, and consistent 8px-or-smaller radii carry the majority of information. Frappe UI components and Lucide icons are reused for familiar controls.

At 390px, the sidebar becomes compact navigation, fixed three-column workspaces stack in task order, data tables gain horizontal containment, and action labels wrap rather than overflow. Empty, loading, error, permission, and confirmation states are first-class page states.

## Verification

- Unit tests cover API-mode selection, session persistence and role changes, local demo mutations, AI confirmation boundary, and role route guards.
- `npm run build`, `npm test`, and `vue-tsc --noEmit` must pass.
- Browser verification covers login, role switching, each role dashboard, student lesson completion, assignment submit, teacher grading/AI confirmation, exam/paper action, admin course review, real-mode error state, and mobile 390px overflow.
