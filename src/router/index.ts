import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/student/dashboard' },
    {
      path: '/student/dashboard',
      name: 'StudentDashboard',
      component: () => import('../views/student/StudentDashboardView.vue'),
    },
    {
      path: '/student/courses/data-structures/lessons/binary-tree',
      name: 'StudentLesson',
      component: () => import('../views/student/StudentLessonView.vue'),
    },
    {
      path: '/teacher/dashboard',
      name: 'TeacherDashboard',
      component: () => import('../views/teacher/TeacherDashboardView.vue'),
    },
    {
      path: '/teacher/assignments/tree-traversal/grading/li',
      name: 'TeacherGrading',
      component: () => import('../views/teacher/TeacherGradingView.vue'),
    },
    {
      path: '/teacher/exams/data-structures-final/paper',
      name: 'TeacherPaper',
      component: () => import('../views/teacher/TeacherPaperView.vue'),
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('../views/admin/AdminDashboardView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/student/dashboard' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
