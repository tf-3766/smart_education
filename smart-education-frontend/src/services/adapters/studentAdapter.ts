import { assignmentsApi, examsApi, forumApi, studentLearningApi } from '@/services/api'
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

const settled = async <T>(promise: Promise<T>, fallback: T): Promise<T> => {
  try {
    return await promise
  } catch {
    return fallback
  }
}

// 学生仅在课程处于 PUBLISHED/ONGOING 时可访问其学习内容（与后端 StudentLearningService 一致）。
// 已下线/已结束等课程的 progress、作业、考试、论坛子资源会返回 403，而 httpClient 对任意 403 都会
// 广播 forbidden 事件跳转「无权访问」——settled 只吞掉了 promise，拦不住该事件。故这些课程必须「根本不发起请求」。
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
