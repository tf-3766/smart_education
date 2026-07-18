// 4.3 管理员统计接口
import { demoDelay } from '../runtime'
import { get, isRealMode } from './client'
import { db } from './demo/db'
import type { AdminStatisticsVO } from './types'

export const adminStatisticsApi = {
  async get(): Promise<AdminStatisticsVO> {
    if (isRealMode()) return get<AdminStatisticsVO>('/api/v1/admin/statistics')
    return demoDelay({
      totalUsers: db.users.length,
      enabledUsers: db.users.filter((user) => user.userStatus === 'ENABLED').length,
      students: db.users.filter((user) => user.roles.includes('STUDENT')).length,
      teachers: db.users.filter((user) => user.roles.includes('TEACHER')).length,
      administrators: db.users.filter((user) => user.roles.includes('ADMIN')).length,
      totalCourses: db.courses.length,
      publishedCourses: db.courses.filter((course) => course.status === 'PUBLISHED').length,
      pendingCourseReviews: db.courses.filter((course) => course.reviewStatus === 'PENDING').length,
      activeEnrollments: db.enrollments.filter((enrollment) => enrollment.status === 'ENROLLED').length,
      publishedAssignments: db.assignments.filter((assignment) => assignment.assignmentStatus === 'PUBLISHED').length,
      submittedAssignments: db.submissions.filter((submission) => submission.submissionStatus !== 'DRAFT').length,
      publishedExams: db.exams.filter((exam) => exam.status === 'PUBLISHED').length,
      openWarnings: db.warnings.filter((warning) => warning.warningStatus === 'OPEN').length,
      publishedAnnouncements: db.announcements.filter((announcement) => announcement.status === 'PUBLISHED').length,
    })
  },
}
