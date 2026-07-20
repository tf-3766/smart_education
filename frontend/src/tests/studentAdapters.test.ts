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
})
