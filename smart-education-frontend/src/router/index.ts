import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { roleHome, useSessionStore } from '@/stores/session'
import type { Role } from '@/types/domain'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    role?: Role
  }
}

const AppShell = () => import('@/layouts/AppShell.vue')

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('@/domains/auth/LoginPage.vue'), meta: { title: '登录' } },
  { path: '/403', component: () => import('@/domains/system/ForbiddenPage.vue'), meta: { title: '无权访问' } },

  {
    path: '/account',
    component: AppShell,
    children: [
      { path: '', redirect: '/account/profile' },
      { path: 'profile', component: () => import('@/domains/account/ProfilePage.vue'), meta: { title: '个人中心' } },
    ],
  },

  {
    path: '/student',
    component: AppShell,
    meta: { role: 'student' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', component: () => import('@/domains/student/StudentDashboardPage.vue'), meta: { title: '学习首页' } },
      { path: 'courses', component: () => import('@/domains/student/StudentCourseListPage.vue'), meta: { title: '我的课程' } },
      { path: 'enroll', component: () => import('@/domains/student/EnrollCenterPage.vue'), meta: { title: '选课中心' } },
      { path: 'lessons/:lessonId', component: () => import('@/domains/student/LessonWorkspacePage.vue'), meta: { title: '章节学习' } },
      { path: 'assignments', component: () => import('@/domains/student/StudentAssignmentsPage.vue'), meta: { title: '学习任务' } },
      { path: 'exams', component: () => import('@/domains/student/StudentExamsPage.vue'), meta: { title: '考试安排' } },
      { path: 'exams/:examId/attempt', component: () => import('@/domains/student/ExamAttemptPage.vue'), meta: { title: '在线答题' } },
      { path: 'grades', component: () => import('@/domains/student/StudentGradesPage.vue'), meta: { title: '成绩进度' } },
      { path: 'warnings', component: () => import('@/domains/student/StudentWarningsPage.vue'), meta: { title: '学习预警' } },
      { path: 'forum', component: () => import('@/domains/student/StudentForumPage.vue'), meta: { title: '课程互动' } },
    ],
  },

  {
    path: '/teacher',
    component: AppShell,
    meta: { role: 'teacher' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', component: () => import('@/domains/teacher/TeacherDashboardPage.vue'), meta: { title: '教学工作台' } },
      { path: 'courses', component: () => import('@/domains/teacher/CourseManagePage.vue'), meta: { title: '课程管理' } },
      { path: 'courses/:courseId/content', component: () => import('@/domains/teacher/CourseContentPage.vue'), meta: { title: '内容管理' } },
      { path: 'assignments', component: () => import('@/domains/teacher/GradingWorkspacePage.vue'), meta: { title: '作业批改' } },
      { path: 'exams', component: () => import('@/domains/teacher/QuestionBankPage.vue'), meta: { title: '考试题库' } },
      { path: 'warnings', component: () => import('@/domains/teacher/WarningsPage.vue'), meta: { title: '学情预警' } },
      { path: 'forum', component: () => import('@/domains/teacher/TeacherForumPage.vue'), meta: { title: '课程互动' } },
    ],
  },

  {
    path: '/admin',
    component: AppShell,
    meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('@/domains/admin/AdminDashboardPage.vue'), meta: { title: '数据看板' } },
      { path: 'users', component: () => import('@/domains/admin/UserManagementPage.vue'), meta: { title: '用户管理' } },
      { path: 'course-reviews', component: () => import('@/domains/admin/CourseReviewPage.vue'), meta: { title: '课程治理' } },
      { path: 'term-enrollment', component: () => import('@/domains/admin/TermEnrollmentPage.vue'), meta: { title: '学期选课时间' } },
      { path: 'content', component: () => import('@/domains/admin/ContentGovernancePage.vue'), meta: { title: '内容治理' } },
      { path: 'ai', component: () => import('@/domains/admin/AiSettingsPage.vue'), meta: { title: 'AI 设置' } },
      { path: 'statistics', component: () => import('@/domains/admin/AnalyticsPage.vue'), meta: { title: '数据统计' } },
    ],
  },

  { path: '/:pathMatch(.*)*', component: () => import('@/domains/system/NotFoundPage.vue'), meta: { title: '页面不存在' } },
]

const router = createRouter({ history: createWebHistory(), routes })

if (typeof window !== 'undefined') {
  window.addEventListener('smart-education:auth-expired', () => {
    const session = useSessionStore()
    session.logout()
    if (router.currentRoute.value.path !== '/login') void router.push('/login')
  })
  window.addEventListener('smart-education:forbidden', () => {
    if (router.currentRoute.value.path !== '/403') void router.push('/403')
  })
}

router.beforeEach(async (to) => {
  const session = useSessionStore()
  if (!session.initialized) {
    try {
      await session.restore()
    } catch {
      if (to.path !== '/login') return '/login'
    }
  }
  if (to.path !== '/login' && !session.authenticated) return '/login'
  if (to.path === '/login' && session.authenticated) return roleHome[session.currentRole]
  if (to.meta.role && to.meta.role !== session.currentRole) return '/403'
  // 用户管理仅超级管理员可用（后端 admin:manage）；普通管理员直达该路由时拦在前端，
  // 避免进入页面后必然触发 403 跳转。
  if (to.path === '/admin/users' && !session.isSuperAdmin) return '/403'
  return true
})

export default router
