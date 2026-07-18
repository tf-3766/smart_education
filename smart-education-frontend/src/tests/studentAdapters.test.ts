import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  myCourses: vi.fn(),
  progress: vi.fn(),
  studentList: vi.fn(),
  studentExams: vi.fn(),
  studentGrades: vi.fn(),
  studentTopics: vi.fn(),
}))

vi.mock('@/services/api', () => ({
  studentLearningApi: { myCourses: api.myCourses, progress: api.progress },
  assignmentsApi: { studentList: api.studentList, studentGrades: api.studentGrades },
  examsApi: { studentExams: api.studentExams },
  forumApi: { studentTopics: api.studentTopics },
}))

import { loadStudentOverview } from '@/services/adapters/studentAdapter'

describe('student real-data adapter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.myCourses.mockResolvedValue({
      records: [{ courseId: '10', courseCode: 'PY101', name: 'Python', ownerTeacherName: '李老师', status: { code: 'PUBLISHED', label: '已发布' }, enrollmentStatus: { code: 'ENROLLED', label: '已选' }, enrollable: false }],
      page: 1, size: 20, total: 1, totalPages: 1,
    })
    api.progress.mockResolvedValue({ courseId: '10', totalLessons: 4, availableLessons: 4, completedLessons: 1, progressPercent: 25, nextLessonId: '99' })
    api.studentList.mockResolvedValue({ records: [{ assignmentId: '20', courseId: '10', title: '作业一', maxScore: 100, dueAt: '2026-07-20T00:00:00Z', availabilityStatus: { code: 'OPEN', label: '进行中' }, submissionStatus: { code: 'NOT_SUBMITTED', label: '未提交' }, score: null, publishedGrade: false }], page: 1, size: 20, total: 1, totalPages: 1 })
    api.studentExams.mockResolvedValue({ records: [], page: 1, size: 20, total: 0, totalPages: 0 })
    api.studentGrades.mockResolvedValue({ records: [], page: 1, size: 20, total: 0, totalPages: 0 })
    api.studentTopics.mockResolvedValue({ records: [], page: 1, size: 20, total: 0, totalPages: 0 })
  })

  it('loads enrolled courses and flattens their role-specific resources', async () => {
    const result = await loadStudentOverview()

    expect(result.courses[0]).toMatchObject({ id: '10', title: 'Python', progress: 25, nextLessonId: '99' })
    expect(result.assignments[0]).toMatchObject({ id: '20', courseId: '10', status: 'OPEN' })
    expect(api.studentList).toHaveBeenCalledWith('10', { page: 1, size: 100 })
    expect(api.studentExams).toHaveBeenCalledWith('10', { page: 1, size: 100 })
  })

  // 已下线/已结束课程的子资源会 403，任意 403 都会把整页跳到「无权访问」。
  // 因此这类课程必须「根本不发起子资源请求」，只在列表里保留课程本身。
  it('对不可学习（已下线等）课程不发起 progress/作业/考试/论坛请求，避免 403 跳飞整页', async () => {
    api.myCourses.mockResolvedValue({
      records: [
        { courseId: '10', courseCode: 'PY101', name: 'Python', ownerTeacherName: '李老师', status: { code: 'PUBLISHED', label: '已发布' }, enrollmentStatus: { code: 'ENROLLED', label: '已选' }, enrollable: false },
        { courseId: '11', courseCode: 'OFF1', name: '已下线课', ownerTeacherName: '王老师', status: { code: 'OFFLINE', label: '已下线' }, enrollmentStatus: { code: 'ENROLLED', label: '已选' }, enrollable: false },
      ],
      page: 1, size: 20, total: 2, totalPages: 1,
    })

    const result = await loadStudentOverview()

    // 两门课都在列表里
    expect(result.courses.map((c) => c.id)).toEqual(['10', '11'])
    // 只对可学习课程 10 发起子资源请求，绝不碰下线课 11
    expect(api.progress).toHaveBeenCalledWith('10')
    expect(api.progress).not.toHaveBeenCalledWith('11')
    for (const fn of [api.studentList, api.studentExams, api.studentTopics]) {
      expect(fn).toHaveBeenCalledWith('10', expect.anything())
      expect(fn).not.toHaveBeenCalledWith('11', expect.anything())
    }
  })
})
