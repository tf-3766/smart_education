import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({ courses: vi.fn(), assignments: vi.fn(), warnings: vi.fn(), topics: vi.fn() }))
vi.mock('@/services/api', () => ({
  teacherCoursesApi: { list: api.courses },
  assignmentsApi: { teacherList: api.assignments },
  warningsApi: { teacherList: api.warnings },
  forumApi: { teacherTopics: api.topics },
}))

import { loadTeacherOverview } from '@/services/adapters/teacherAdapter'

describe('teacher real-data adapter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.courses.mockResolvedValue({ records: [{ courseId: '10', courseCode: 'PY101', name: 'Python', ownerTeacherId: '2', ownerTeacherName: '李老师', status: { code: 'PUBLISHED', label: '已发布' }, reviewStatus: { code: 'APPROVED', label: '已通过' }, updatedAt: '2026-07-12T00:00:00Z' }], total: 1 })
    api.assignments.mockResolvedValue({ records: [{ assignmentId: '20', courseId: '10', title: '作业一', maxScore: 100, assignmentStatus: { code: 'PUBLISHED', label: '已发布' }, availabilityStatus: { code: 'OPEN', label: '进行中' }, dueAt: '2026-07-20T00:00:00Z', attachments: [], version: 0 }] })
    api.warnings.mockResolvedValue({ records: [] })
    api.topics.mockResolvedValue({ records: [] })
  })

  it('loads all managed course resources and keeps backend versions', async () => {
    const result = await loadTeacherOverview()
    expect(result.courses[0]).toMatchObject({ id: '10', title: 'Python', status: 'PUBLISHED' })
    expect(result.assignments[0]).toMatchObject({ assignmentId: '20', version: 0 })
    expect(api.assignments).toHaveBeenCalledWith('10', { page: 1, size: 100 })
  })
})
