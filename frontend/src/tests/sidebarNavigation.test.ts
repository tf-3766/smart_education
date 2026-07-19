import { describe, expect, it } from 'vitest'
import { getRoleSidebar } from '@/layouts/roleSidebar'
import { resolveSidebarRoute } from '@/layouts/sidebarNavigation'

describe('workspace sidebar navigation', () => {
  it('resolves a nested teacher route and its expandable parent', () => {
    const match = resolveSidebarRoute(getRoleSidebar('teacher'), '/teacher/assignments')

    expect(match?.item.label).toBe('作业批改')
    expect(match?.parent).toEqual({ groupIndex: 0, itemIndex: 2 })
  })

  it('resolves direct detail routes without a parent group', () => {
    const match = resolveSidebarRoute(getRoleSidebar('admin'), '/admin/users')

    expect(match?.item.label).toBe('用户管理')
    expect(match?.parent).toBeUndefined()
  })

  it('keeps the course navigation context while a student studies a lesson', () => {
    const match = resolveSidebarRoute(getRoleSidebar('student'), '/student/lessons/lesson-01')

    expect(match?.item.label).toBe('我的课程')
  })
})
