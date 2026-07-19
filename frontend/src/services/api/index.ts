// 契约 API 总出口：与 api-reference.md 的全部公开接口一一对应。
// 默认演示模式（本地契约形状数据 + localStorage 持久化）；
// 设置 VITE_API_MODE=real 后经网关访问真实后端，错误显式抛出 RuntimeError，不回退演示数据。
export * from './types'
export { buildQuery, fileContentUrl, isRealMode, TOKEN_STORAGE_KEY } from './client'
export { resetDemoData } from './demo/db'

export { authApi } from './auth'
export { filesApi } from './files'
export { categoriesApi } from './categories'
export { termEnrollmentWindowsApi } from './termEnrollmentWindows'
export { teacherCoursesApi } from './teacherCourses'
export { courseContentApi } from './courseContent'
export { studentLearningApi } from './studentLearning'
export { courseReviewsApi } from './courseReviews'
export { adminUsersApi } from './adminUsers'
export { assignmentsApi } from './assignments'
export { forumApi } from './forum'
export { warningsApi } from './warnings'
export { examsApi } from './exams'
export { announcementsApi } from './announcements'
export { notificationsApi } from './notifications'
export { adminStatisticsApi } from './adminStatistics'
export { aiApi } from './ai'
