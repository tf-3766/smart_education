import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { notificationsApi } from '@/services/api'
import type { NotificationCategory as BackendNotificationCategory, NotificationVO } from '@/services/api'
import type { Role } from '@/types/domain'

export type NotificationCategory = 'course' | 'assignment' | 'exam' | 'warning' | 'system'
export type NotificationStatusFilter = 'all' | 'unread'
export type NotificationCategoryFilter = 'all' | NotificationCategory

export interface InboxNotification {
  id: string
  title: string
  content: string
  category: NotificationCategory
  categoryLabel: string
  sourceType: string
  timestamp: string
  read: boolean
  announcementId?: string | null
  courseId?: string | null
  assignmentId?: string | null
  examId?: string | null
  warningId?: string | null
}

const categoryMap: Record<BackendNotificationCategory, NotificationCategory> = {
  COURSE: 'course',
  ASSIGNMENT: 'assignment',
  EXAM: 'exam',
  WARNING: 'warning',
  SYSTEM: 'system',
}

function toInbox(item: NotificationVO): InboxNotification {
  return {
    id: item.notificationId,
    title: item.title,
    content: item.content,
    category: categoryMap[item.category],
    categoryLabel: item.categoryLabel,
    sourceType: item.sourceType,
    timestamp: item.createdAt,
    read: item.read,
    announcementId: item.announcementId,
    courseId: item.courseId,
    assignmentId: item.assignmentId,
    examId: item.examId,
    warningId: item.warningId,
  }
}

export function notificationTarget(item: InboxNotification, role: Role): string | undefined {
  if (role === 'admin') return (item.announcementId || item.category === 'system') ? '/admin/content' : '/admin/dashboard'
  if (role === 'student') {
    if (item.sourceType === 'GRADE_PUBLISHED') return '/student/grades'
    if (item.assignmentId) return `/student/assignments?assignmentId=${encodeURIComponent(item.assignmentId)}`
    if (item.examId) return `/student/exams?examId=${encodeURIComponent(item.examId)}`
    if (item.warningId) return `/student/warnings?warningId=${encodeURIComponent(item.warningId)}`
    if (item.courseId) return '/student/courses'
    return undefined
  }
  if (item.assignmentId) {
    const query = new URLSearchParams({ assignmentId: item.assignmentId })
    if (item.courseId) query.set('courseId', item.courseId)
    return `/teacher/assignments?${query.toString()}`
  }
  if (item.examId) return `/teacher/exams?examId=${encodeURIComponent(item.examId)}`
  if (item.warningId) {
    const query = new URLSearchParams({ warningId: item.warningId })
    if (item.courseId) query.set('courseId', item.courseId)
    return `/teacher/warnings?${query.toString()}`
  }
  if (item.courseId) return `/teacher/courses/${encodeURIComponent(item.courseId)}/content`
  return undefined
}

export const useNotificationStore = defineStore('notifications', () => {
  const items = ref<InboxNotification[]>([])
  const statusFilter = ref<NotificationStatusFilter>('all')
  const categoryFilter = ref<NotificationCategoryFilter>('all')
  const unreadCount = ref(0)
  const loading = ref(false)
  const initialized = ref(false)
  const error = ref<string | null>(null)
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const subscribedCategories = ref<NotificationCategory[]>(['course', 'assignment', 'exam', 'warning', 'system'])
  const preferencesLoading = ref(false)
  const realtimeConnected = ref(false)
  let requestVersion = 0
  let stopStream: (() => void) | undefined

  const visibleItems = computed(() => items.value.filter((item) => {
    const statusMatches = statusFilter.value === 'all' || !item.read
    const categoryMatches = categoryFilter.value === 'all' || item.category === categoryFilter.value
    return statusMatches && categoryMatches
  }))
  const hasMore = computed(() => items.value.length < total.value)

  async function refreshUnreadCount() {
    try {
      unreadCount.value = await notificationsApi.unreadCount()
    } catch (reason) {
      error.value = reason instanceof Error ? reason.message : '消息数量加载失败，请重试。'
    }
  }

  async function load(options: { reset?: boolean } = {}) {
    const reset = options.reset !== false
    const version = ++requestVersion
    loading.value = true
    error.value = null
    if (reset) page.value = 1
    try {
      const result = await notificationsApi.list({
        page: page.value,
        size: pageSize.value,
        ...(categoryFilter.value !== 'all' ? { category: categoryFilter.value.toUpperCase() as BackendNotificationCategory } : {}),
        ...(statusFilter.value === 'unread' ? { unread: true } : {}),
      })
      if (version !== requestVersion) return
      const records = result.records.map(toInbox)
      const combined = reset ? records : [...items.value, ...records]
      items.value = [...new Map(combined.map((item) => [item.id, item])).values()]
      page.value = result.page
      pageSize.value = result.size
      total.value = result.total
      initialized.value = true
      await refreshUnreadCount()
    } catch (reason) {
      if (version === requestVersion) error.value = reason instanceof Error ? reason.message : '消息加载失败，请重试。'
    } finally {
      if (version === requestVersion) loading.value = false
    }
  }

  async function loadMore() {
    if (loading.value || !hasMore.value) return
    const previousPage = page.value
    page.value = previousPage + 1
    await load({ reset: false })
    if (error.value) page.value = previousPage
  }

  async function markRead(id: string) {
    const item = items.value.find((notification) => notification.id === id)
    if (!item || item.read) return
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    try {
      await notificationsApi.markRead(id)
      const current = items.value.find((notification) => notification.id === id)
      if (current) current.read = true
    } catch (reason) {
      const current = items.value.find((notification) => notification.id === id)
      if (current) current.read = false
      unreadCount.value += 1
      error.value = reason instanceof Error ? reason.message : '消息已读状态保存失败，请重试。'
    }
  }

  async function markAllRead() {
    const unreadItems = items.value.filter((item) => !item.read)
    if (unreadCount.value === 0) return
    unreadItems.forEach((item) => { item.read = true })
    unreadCount.value = 0
    try {
      await notificationsApi.markAllRead()
    } catch (reason) {
      unreadItems.forEach((item) => { item.read = false })
      await refreshUnreadCount()
      error.value = reason instanceof Error ? reason.message : '全部已读保存失败，请重试。'
    }
  }

  async function archive(id: string) {
    const index = items.value.findIndex((item) => item.id === id)
    if (index < 0) return
    const [removed] = items.value.splice(index, 1)
    total.value = Math.max(0, total.value - 1)
    if (!removed.read) unreadCount.value = Math.max(0, unreadCount.value - 1)
    try {
      await notificationsApi.archive(id)
    } catch (reason) {
      items.value.splice(index, 0, removed)
      total.value += 1
      if (!removed.read) unreadCount.value += 1
      error.value = reason instanceof Error ? reason.message : '消息归档失败，请重试。'
    }
  }

  async function loadPreferences() {
    preferencesLoading.value = true
    try {
      const result = await notificationsApi.preferences()
      subscribedCategories.value = result.enabledCategories.map((category) => categoryMap[category])
    } catch (reason) {
      error.value = reason instanceof Error ? reason.message : '订阅偏好加载失败，请重试。'
    } finally {
      preferencesLoading.value = false
    }
  }

  async function setCategorySubscription(category: NotificationCategory, enabled: boolean) {
    const previous = [...subscribedCategories.value]
    subscribedCategories.value = enabled
      ? [...new Set([...subscribedCategories.value, category])]
      : subscribedCategories.value.filter((item) => item !== category)
    preferencesLoading.value = true
    try {
      const result = await notificationsApi.updatePreferences({
        enabledCategories: subscribedCategories.value.map((item) => item.toUpperCase() as BackendNotificationCategory),
      })
      subscribedCategories.value = result.enabledCategories.map((item) => categoryMap[item])
    } catch (reason) {
      subscribedCategories.value = previous
      error.value = reason instanceof Error ? reason.message : '订阅偏好保存失败，请重试。'
    } finally {
      preferencesLoading.value = false
    }
  }

  function stopRealtime() {
    stopStream?.()
    stopStream = undefined
    realtimeConnected.value = false
  }

  function startRealtime() {
    stopRealtime()
    stopStream = notificationsApi.subscribe(
      (event) => {
        if (event.type === 'connected') {
          realtimeConnected.value = true
          return
        }
        if (event.type === 'heartbeat') return
        void refreshUnreadCount()
        if (initialized.value) void load()
        if (event.type === 'preferences-changed') void loadPreferences()
      },
      () => { realtimeConnected.value = false },
    )
  }

  function reset() {
    stopRealtime()
    requestVersion += 1
    items.value = []
    unreadCount.value = 0
    page.value = 1
    total.value = 0
    initialized.value = false
    loading.value = false
    error.value = null
    subscribedCategories.value = ['course', 'assignment', 'exam', 'warning', 'system']
  }

  watch([statusFilter, categoryFilter], () => {
    if (initialized.value) void load()
  })

  return {
    items,
    statusFilter,
    categoryFilter,
    unreadCount,
    visibleItems,
    loading,
    initialized,
    error,
    page,
    pageSize,
    total,
    hasMore,
    subscribedCategories,
    preferencesLoading,
    realtimeConnected,
    load,
    loadMore,
    refreshUnreadCount,
    markRead,
    markAllRead,
    archive,
    loadPreferences,
    setCategorySubscription,
    startRealtime,
    stopRealtime,
    reset,
  }
})
