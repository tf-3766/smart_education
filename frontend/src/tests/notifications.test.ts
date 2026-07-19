import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { notificationsApi, resetDemoData } from '@/services/api'
import { notificationTarget, useNotificationStore } from '@/stores/notifications'

describe('notification store', () => {
  beforeEach(() => {
    localStorage.clear()
    resetDemoData()
    sessionStorage.setItem('smart-education-session', JSON.stringify({ authenticated: true, role: 'student' }))
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads role-scoped notifications and the server unread count', async () => {
    const notifications = useNotificationStore()

    expect(notifications.items).toHaveLength(0)
    await notifications.load()

    expect(notifications.items).toHaveLength(6)
    expect(notifications.unreadCount).toBe(6)
    expect(notifications.items.map((item) => item.title)).toContain('第一章作业已发布')
    expect(notifications.items.map((item) => item.category)).toEqual(expect.arrayContaining(['assignment', 'exam', 'system']))
  })

  it('persists a read receipt and removes the message from the unread filter', async () => {
    const notifications = useNotificationStore()
    await notifications.load()
    notifications.statusFilter = 'unread'
    const firstId = notifications.visibleItems[0].id
    const initialUnread = notifications.unreadCount

    await notifications.markRead(firstId)

    expect(notifications.unreadCount).toBe(initialUnread - 1)
    expect(notifications.visibleItems.some((item) => item.id === firstId)).toBe(false)

    setActivePinia(createPinia())
    const restored = useNotificationStore()
    await restored.load()
    expect(restored.items.find((item) => item.id === firstId)?.read).toBe(true)
  })

  it('loads category-filtered messages from the API', async () => {
    const notifications = useNotificationStore()
    notifications.categoryFilter = 'course'
    await notifications.load()

    expect(notifications.visibleItems).toHaveLength(1)
    expect(notifications.visibleItems.every((item) => item.category === 'course')).toBe(true)
  })

  it('marks all messages as read and can clear state on account changes', async () => {
    const notifications = useNotificationStore()
    await notifications.load()

    await notifications.markAllRead()

    expect(notifications.unreadCount).toBe(0)
    expect(notifications.items.every((item) => item.read)).toBe(true)

    notifications.reset()
    expect(notifications.items).toHaveLength(0)
    expect(notifications.unreadCount).toBe(0)
    expect(notifications.initialized).toBe(false)
  })

  it('loads later pages and marks unread messages outside the current page as read', async () => {
    const notifications = useNotificationStore()
    notifications.pageSize = 1
    await notifications.load()

    expect(notifications.items).toHaveLength(1)
    expect(notifications.hasMore).toBe(true)
    await notifications.loadMore()
    expect(notifications.items).toHaveLength(2)

    await notifications.markRead(notifications.items[0].id)
    await notifications.markRead(notifications.items[1].id)
    expect(notifications.unreadCount).toBeGreaterThan(0)
    await notifications.markAllRead()
    expect(notifications.unreadCount).toBe(0)
  })

  it('exposes API failures for the drawer retry state', async () => {
    vi.spyOn(notificationsApi, 'list').mockRejectedValueOnce(new Error('服务暂不可用'))
    const notifications = useNotificationStore()

    await notifications.load()

    expect(notifications.loading).toBe(false)
    expect(notifications.error).toBe('服务暂不可用')
    expect(notifications.items).toHaveLength(0)
  })

  it('persists subscription preferences and archives a message', async () => {
    const notifications = useNotificationStore()
    await notifications.load()
    await notifications.loadPreferences()
    expect(notifications.subscribedCategories).toContain('exam')

    await notifications.setCategorySubscription('exam', false)
    expect(notifications.subscribedCategories).not.toContain('exam')

    const archivedId = notifications.items[0].id
    await notifications.archive(archivedId)
    expect(notifications.items.some((item) => item.id === archivedId)).toBe(false)

    setActivePinia(createPinia())
    const restored = useNotificationStore()
    await restored.load()
    await restored.loadPreferences()
    expect(restored.items.some((item) => item.id === archivedId)).toBe(false)
    expect(restored.subscribedCategories).not.toContain('exam')
  })

  it('resolves role-safe resource targets', async () => {
    const notifications = useNotificationStore()
    await notifications.load()
    const assignment = notifications.items.find((item) => item.assignmentId && item.sourceType === 'ASSIGNMENT_PUBLISHED')
    const exam = notifications.items.find((item) => item.examId)

    expect(assignment && notificationTarget(assignment, 'student')).toContain('/student/assignments')
    expect(assignment && notificationTarget(assignment, 'teacher')).toContain('/teacher/assignments')
    expect(exam && notificationTarget(exam, 'student')).toContain('/student/exams')
  })
})
