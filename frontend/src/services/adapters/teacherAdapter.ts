import { assignmentsApi, forumApi, teacherCoursesApi, warningsApi } from '@/services/api'
// best-effort 聚合：个别课程/资源越权返回 403 时，tolerant 吞掉异常并抑制全局 forbidden 跳转。
import { tolerant as settled } from '@/services/httpClient'
import type { AssignmentDetailVO, ForumTopicListItemVO, LearningWarningVO, TeacherCourseListItemVO } from '@/services/api/types'

export interface TeacherCourseItem extends Omit<TeacherCourseListItemVO, 'status'> {
  id: string
  title: string
  status: string
  review: string
}

export interface TeacherOverview {
  courses: TeacherCourseItem[]
  assignments: AssignmentDetailVO[]
  warnings: LearningWarningVO[]
  topics: ForumTopicListItemVO[]
}

export async function loadTeacherOverview(): Promise<TeacherOverview> {
  const page = await teacherCoursesApi.list({ page: 1, size: 100 })
  const courses = page.records.map((course) => ({
    ...course,
    id: course.courseId,
    title: course.name,
    status: course.status.code,
    review: course.reviewStatus.code,
  }))
  const pages = await Promise.all(courses.map(async (course) => {
    const [assignmentPage, warningPage, topicPage] = await Promise.all([
      settled(assignmentsApi.teacherList(course.id, { page: 1, size: 100 }), { records: [] } as { records: AssignmentDetailVO[] }),
      settled(warningsApi.teacherList(course.id, { page: 1, size: 100 }), { records: [] } as { records: LearningWarningVO[] }),
      settled(forumApi.teacherTopics(course.id, { page: 1, size: 100 }), { records: [] } as { records: ForumTopicListItemVO[] }),
    ])
    return { assignments: assignmentPage.records, warnings: warningPage.records, topics: topicPage.records }
  }))
  return {
    courses,
    assignments: pages.flatMap((item) => item.assignments),
    warnings: pages.flatMap((item) => item.warnings),
    topics: pages.flatMap((item) => item.topics),
  }
}
