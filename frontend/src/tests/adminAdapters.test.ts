import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({ statistics: vi.fn(), users: vi.fn(), reviews: vi.fn(), aiStatus: vi.fn() }))
vi.mock('@/services/api', () => ({
  adminStatisticsApi: { get: api.statistics },
  adminUsersApi: { list: api.users },
  courseReviewsApi: { list: api.reviews },
  aiApi: { adminStatus: api.aiStatus },
}))
import { loadAdminOverview } from '@/services/adapters/adminAdapter'

describe('administrator real-data adapter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.statistics.mockResolvedValue({ totalUsers: 10, enabledUsers: 9, students: 6, teachers: 2, administrators: 1, totalCourses: 3, publishedCourses: 2, pendingCourseReviews: 1, activeEnrollments: 12, publishedAssignments: 4, submittedAssignments: 8, publishedExams: 1, openWarnings: 2, publishedAnnouncements: 1 })
    api.users.mockResolvedValue({ records: [{ userId: '1', username: 'student', displayName: '测试学生', userStatus: 'ENABLED', roles: ['STUDENT'], superAdministrator: false, createdAt: '2026-07-01', version: 0 }] })
    api.reviews.mockResolvedValue({ records: [] })
    api.aiStatus.mockResolvedValue({ provider: 'fallback', available: true, mode: 'FRAMEWORK_ONLY' })
  })
  it('loads governance statistics, users, reviews and AI status', async () => {
    const result = await loadAdminOverview()
    expect(result.statistics.totalUsers).toBe(10)
    expect(result.users[0]).toMatchObject({ userId: '1', displayName: '测试学生' })
    expect(result.ai).toMatchObject({ available: true, mode: 'FRAMEWORK_ONLY' })
  })

  it('skips the super-admin-only user list for plain administrators', async () => {
    // 普通管理员无 admin:manage 权限，绝不能发起 /admin/users 请求，否则 403 会把整页跳到无权访问。
    const result = await loadAdminOverview({ includeUsers: false })
    expect(api.users).not.toHaveBeenCalled()
    expect(result.users).toEqual([])
    expect(result.statistics.totalUsers).toBe(10)
    expect(result.ai).toMatchObject({ available: true })
  })
})
