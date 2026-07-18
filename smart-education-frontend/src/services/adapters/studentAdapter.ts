import { assignmentsApi, examsApi, forumApi, studentLearningApi } from '@/services/api'
// 首页并发聚合里的 best-effort 请求：越权/下线课程的子资源会 403，
// 用 tolerant 统一吞掉异常并抑制全局 forbidden 跳转（见 httpClient.tolerant）。
import { tolerant as settled } from '@/services/httpClient'
import type {
  StudentAssignmentListItemVO,
  StudentCourseListItemVO,
  StudentExamListItemVO,
  StudentGradeVO,
  ForumTopicListItemVO,
} from '@/services/api/types'

export interface StudentCourseItem {
  id: string
  code: string
  title: string
  teacher: string
  term: string
  status: string
  progress: number
  nextLessonId: string | null
}

export interface StudentAssignmentItem {
  id: string
  courseId: string
  title: string
  dueAt: string
  maxScore: number
  status: string
  submissionStatus: string
  graded: boolean
}

export interface StudentOverview {
  courses: StudentCourseItem[]
  assignments: StudentAssignmentItem[]
  exams: StudentExamListItemVO[]
  grades: StudentGradeVO[]
  topics: ForumTopicListItemVO[]
}

// 学生仅在课程处于 PUBLISHED/ONGOING 时可访问其学习内容（与后端 StudentLearningService 一致）。
// 已下线/已结束等课程的 progress、作业、考试、论坛子资源会返回 403。虽然 settled(=tolerant) 现已能抑制
// 403 的全局跳转，这里仍按状态过滤，直接不发起注定 403 的请求（减少无谓往返，双保险）。
const LEARNABLE_COURSE_STATUS = new Set(['PUBLISHED', 'ONGOING'])
const isLearnable = (statusCode: string) => LEARNABLE_COURSE_STATUS.has(statusCode)

export async function loadStudentOverview(): Promise<StudentOverview> {
  const coursePage = await studentLearningApi.myCourses({ page: 1, size: 100 })
  const courses = await Promise.all(coursePage.records.map(async (course: StudentCourseListItemVO) => {
    const progress = isLearnable(course.status.code)
      ? await settled(studentLearningApi.progress(course.courseId), null)
      : null
    return {
      id: course.courseId,
      code: course.courseCode,
      title: course.name,
      teacher: course.ownerTeacherName,
      term: course.term ?? '-',
      status: course.status.code,
      progress: progress?.progressPercent ?? 0,
      nextLessonId: progress?.nextLessonId ?? progress?.lastLessonId ?? null,
    }
  }))

  const perCourse = await Promise.all(courses.map(async (course) => {
    if (!isLearnable(course.status)) {
      return { assignments: [] as StudentAssignmentListItemVO[], exams: [] as StudentExamListItemVO[], topics: [] as ForumTopicListItemVO[] }
    }
    const [assignmentPage, examPage, topicPage] = await Promise.all([
      settled(assignmentsApi.studentList(course.id, { page: 1, size: 100 }), { records: [] } as { records: StudentAssignmentListItemVO[] }),
      settled(examsApi.studentExams(course.id, { page: 1, size: 100 }), { records: [] } as { records: StudentExamListItemVO[] }),
      settled(forumApi.studentTopics(course.id, { page: 1, size: 100 }), { records: [] } as { records: ForumTopicListItemVO[] }),
    ])
    return { assignments: assignmentPage.records, exams: examPage.records, topics: topicPage.records }
  }))
  const gradePage = await settled(assignmentsApi.studentGrades({ page: 1, size: 100 }), { records: [] } as { records: StudentGradeVO[] })

  return {
    courses,
    assignments: perCourse.flatMap((item) => item.assignments).map((item) => ({
      id: item.assignmentId,
      courseId: item.courseId,
      title: item.title,
      dueAt: item.dueAt,
      maxScore: item.maxScore,
      status: item.availabilityStatus.code,
      submissionStatus: item.submissionStatus?.code ?? 'NOT_SUBMITTED',
      graded: item.graded,
    })),
    exams: perCourse.flatMap((item) => item.exams),
    grades: gradePage.records,
    topics: perCourse.flatMap((item) => item.topics),
  }
}
