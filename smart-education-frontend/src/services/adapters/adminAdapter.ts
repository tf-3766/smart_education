import { adminStatisticsApi, adminUsersApi, aiApi, courseReviewsApi } from '@/services/api'
import { tolerant } from '@/services/httpClient'
import type { AdminStatisticsVO, AdminUserVO, AiServiceStatusVO, CourseReviewListItemVO } from '@/services/api/types'

export interface AdminOverview {
  statistics: AdminStatisticsVO
  users: AdminUserVO[]
  reviews: CourseReviewListItemVO[]
  ai: AiServiceStatusVO | null
}

export async function loadAdminOverview(options: { includeUsers?: boolean } = {}): Promise<AdminOverview> {
  // 用户列表接口仅超级管理员可用（admin:manage）。普通管理员若发起该请求，网关会返回
  // 403，而 httpClient 对任何 403 都会广播 forbidden 事件跳转到「无权访问」页——即便这里
  // catch 也拦不住跳转。因此普通管理员必须「根本不发起」该请求，而非事后吞掉异常。
  const includeUsers = options.includeUsers ?? true
  const [statistics, users, reviews, ai] = await Promise.all([
    adminStatisticsApi.get(),
    includeUsers ? adminUsersApi.list({ page: 1, size: 100 }).then((page) => page.records) : Promise.resolve([]),
    courseReviewsApi.list({ page: 1, size: 100 }),
    tolerant(aiApi.adminStatus(), null),
  ])
  return { statistics, users, reviews: reviews.records, ai }
}
