import { describe, expect, it } from 'vitest'
import { flattenDetailItems, getRoleSidebar, isDetailParent } from '@/layouts/roleSidebar'
import type { Role } from '@/types/domain'

function roleSidebar(role: Role) {
  return getRoleSidebar(role)
}

describe('role sidebar', () => {
  it('uses the reference hierarchy with real teacher routes', () => {
    const sidebar = roleSidebar('teacher')

    expect(sidebar.primary.map((item) => item.key)).toEqual(['all', 'resources'])
    expect(sidebar.primary.map((item) => item.label)).toEqual([
      '全部教学功能',
      '我的教学资源',
    ])
    expect(sidebar.secondary.resources.title).toBe('我的教学资源')
    expect(sidebar.secondary.resources.items.map((item) => item.to)).toEqual([
      '/teacher/courses',
      '/teacher/assignments',
      '/teacher/exams',
    ])
    expect(flattenDetailItems(sidebar.detailGroups).map((item) => item.to)).toEqual([
      '/teacher/dashboard',
      '/teacher/courses',
      '/teacher/assignments',
      '/teacher/exams',
      '/teacher/warnings',
      '/teacher/forum',
    ])
  })

  it('models teacher task tools as an expandable detailed navigation group', () => {
    const sidebar = roleSidebar('teacher')
    const taskGroup = sidebar.detailGroups[0].items.find((item) => item.label === '作业与考试')

    expect(taskGroup).toBeDefined()
    expect(isDetailParent(taskGroup!)).toBe(true)
    if (!taskGroup || !isDetailParent(taskGroup)) throw new Error('作业与考试应为可展开的详细导航父项')

    expect(taskGroup.children.map((item) => item.to)).toEqual([
      '/teacher/assignments',
      '/teacher/exams',
    ])
    expect(flattenDetailItems(sidebar.detailGroups).map((item) => item.to)).toContain('/teacher/exams')
  })

  it('uses the same hierarchy for the student workspace', () => {
    const sidebar = roleSidebar('student')

    expect(sidebar.primary.map((item) => item.key)).toEqual(['all', 'resources'])
    expect(sidebar.primary.map((item) => item.label)).toEqual([
      '全部学习功能',
      '我的学习资源',
    ])
    expect(sidebar.secondary.resources.items.every((item) => item.to.startsWith('/student/'))).toBe(true)
    expect(sidebar.secondary.all.items.map((item) => item.to)).toContain('/student/enroll')
    expect(flattenDetailItems(sidebar.detailGroups)).toHaveLength(7)
  })

  it('uses the same hierarchy for the administration console', () => {
    const sidebar = roleSidebar('admin')

    expect(sidebar.primary.map((item) => item.key)).toEqual(['all', 'resources'])
    expect(sidebar.primary.map((item) => item.label)).toEqual([
      '全部管理功能',
      '我的管理事项',
    ])
    expect(sidebar.secondary.resources.items.every((item) => item.to.startsWith('/admin/'))).toBe(true)
    expect(flattenDetailItems(sidebar.detailGroups)).toHaveLength(7)
  })

  it('hides 用户管理 from plain administrators (non super admin)', () => {
    const full = getRoleSidebar('admin')
    const limited = getRoleSidebar('admin', { superAdmin: false })
    const allTargets = (config: ReturnType<typeof getRoleSidebar>) =>
      flattenDetailItems(config.detailGroups).map((item) => item.to)

    expect(allTargets(full)).toContain('/admin/users')
    expect(allTargets(limited)).not.toContain('/admin/users')
    expect(limited.secondary.all.items.some((item) => item.to === '/admin/users')).toBe(false)
    expect(limited.secondary.resources.items.some((item) => item.to === '/admin/users')).toBe(false)
    // 超级管理员显式传入 true 时仍保留完整入口。
    expect(allTargets(getRoleSidebar('admin', { superAdmin: true }))).toContain('/admin/users')
  })
})
