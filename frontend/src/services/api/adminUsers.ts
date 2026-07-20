// 3.7 超级管理员用户授权接口
import { demoDelay } from '../runtime'
import { del, get, isRealMode, put } from './client'
import { conflict, db, notFound, paginate, persist } from './demo/db'
import type { UserRow } from './demo/db'
import type { AdminUserQuery, AdminUserVO, PageResponse } from './types'

function toVO(row: UserRow): AdminUserVO {
  return {
    userId: row.userId,
    username: row.username,
    displayName: row.displayName,
    userStatus: row.userStatus,
    roles: [...row.roles],
    superAdministrator: row.superAdministrator,
    createdAt: row.createdAt,
    version: row.version,
  }
}

function requireUser(userId: string): UserRow {
  return db.users.find((item) => item.userId === userId) ?? notFound('用户不存在。')
}

export const adminUsersApi = {
  async list(query: AdminUserQuery = {}): Promise<PageResponse<AdminUserVO>> {
    if (isRealMode()) return get<PageResponse<AdminUserVO>>('/api/v1/admin/users', { ...query })
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.users
      .filter((row) => !query.status || row.userStatus === query.status)
      .filter((row) => !keyword || row.username.includes(keyword) || row.displayName.toLowerCase().includes(keyword))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return demoDelay(paginate(rows.map(toVO), query))
  },

  async grantAdministrator(userId: string): Promise<AdminUserVO> {
    if (isRealMode()) return put<AdminUserVO>(`/api/v1/admin/users/${userId}/administrator`)
    const row = requireUser(userId)
    if (row.userStatus !== 'ENABLED') conflict('只能把已启用的用户设为管理员。', 'OPERATION_NOT_ALLOWED')
    if (row.roles.includes('ADMIN')) conflict('该用户已是管理员。', 'OPERATION_NOT_ALLOWED')
    row.roles = [...row.roles, 'ADMIN']
    row.version += 1
    persist()
    return demoDelay(toVO(row))
  },

  async revokeAdministrator(userId: string): Promise<AdminUserVO> {
    if (isRealMode()) return del<AdminUserVO>(`/api/v1/admin/users/${userId}/administrator`)
    const row = requireUser(userId)
    if (row.superAdministrator) conflict('系统超级管理员的管理员身份不可撤销。', 'SUPER_ADMIN_PROTECTED')
    if (!row.roles.includes('ADMIN')) conflict('该用户不是管理员。', 'OPERATION_NOT_ALLOWED')
    row.roles = row.roles.filter((role) => role !== 'ADMIN')
    row.version += 1
    persist()
    return demoDelay(toVO(row))
  },

  async approveTeacher(userId: string): Promise<AdminUserVO> {
    if (isRealMode()) return put<AdminUserVO>(`/api/v1/admin/users/${userId}/teacher-approval`)
    const row = requireUser(userId)
    if (row.userStatus !== 'PENDING') conflict('该教师申请不在待审核状态。', 'TEACHER_REGISTRATION_NOT_PENDING')
    row.userStatus = 'ENABLED'
    row.version += 1
    persist()
    return demoDelay(toVO(row))
  },

  async rejectTeacher(userId: string): Promise<AdminUserVO> {
    if (isRealMode()) return del<AdminUserVO>(`/api/v1/admin/users/${userId}/teacher-approval`)
    const row = requireUser(userId)
    if (row.userStatus !== 'PENDING') conflict('该教师申请不在待审核状态。', 'TEACHER_REGISTRATION_NOT_PENDING')
    row.userStatus = 'REJECTED'
    row.version += 1
    persist()
    return demoDelay(toVO(row))
  },
}
