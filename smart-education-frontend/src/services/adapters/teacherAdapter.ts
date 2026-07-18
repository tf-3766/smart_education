import { assignmentsApi, forumApi, teacherCoursesApi, warningsApi } from '@/services/api'
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

const settled = async <T>(promise: Promise<T>, fallback: T): Promise<T> => {
  try { return await promise } catch { return fallback }
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
