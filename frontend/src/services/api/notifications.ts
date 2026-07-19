// 站内信：真实模式使用持久化通知与 SSE，演示模式从各业务表派生角色可见消息。
import { TOKEN_STORAGE_KEY, gateway } from '../httpClient'
import { RuntimeError, createTraceId, demoDelay } from '../runtime'
import { get, isRealMode, post, put } from './client'
import { currentUser, db, nowIso, paginate, persist } from './demo/db'
import type { NotificationCategory, NotificationListQuery, NotificationPreferencesVO, NotificationStreamEvent, NotificationVO, PageResponse, UpdateNotificationPreferencesRequest } from './types'

type DemoRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

const allCategories: NotificationCategory[] = ['COURSE', 'ASSIGNMENT', 'EXAM', 'WARNING', 'SYSTEM']
const categoryLabels: Record<NotificationCategory, string> = {
  COURSE: '课程消息',
  ASSIGNMENT: '作业消息',
  EXAM: '考试消息',
  WARNING: '预警消息',
  SYSTEM: '系统消息',
}

function demoRole(): DemoRole {
  try {
    const stored = JSON.parse(sessionStorage.getItem('smart-education-session') ?? '{}') as { role?: string }
    if (stored.role === 'teacher') return 'TEACHER'
    if (stored.role === 'admin') return 'ADMIN'
    if (stored.role === 'student') return 'STUDENT'
  } catch {
    // Ignore malformed session state and infer the role from the demo database.
  }
  const user = db.users.find((item) => item.userId === db.session.userId)
  if (user?.roles.includes('ADMIN') || user?.roles.includes('SUPER_ADMIN')) return 'ADMIN'
  if (user?.roles.includes('TEACHER')) return 'TEACHER'
  return 'STUDENT'
}

function notification(
  notificationId: string,
  title: string,
  content: string,
  category: NotificationCategory,
  sourceType: string,
  createdAt: string,
  resources: Pick<NotificationVO, 'announcementId' | 'courseId' | 'assignmentId' | 'examId' | 'warningId'> = {},
): NotificationVO {
  return {
    notificationId,
    title,
    content,
    category,
    categoryLabel: categoryLabels[category],
    status: 'PUBLISHED',
    sourceType,
    announcementId: resources.announcementId ?? null,
    courseId: resources.courseId ?? null,
    assignmentId: resources.assignmentId ?? null,
    examId: resources.examId ?? null,
    warningId: resources.warningId ?? null,
    createdAt,
    read: false,
    readAt: null,
  }
}

function demoRows(): NotificationVO[] {
  const role = demoRole()
  const user = currentUser(role)
  const rows: NotificationVO[] = []
  const studentCourseIds = new Set(db.enrollments
    .filter((item) => item.studentId === user.userId && (item.status === 'ENROLLED' || item.status === 'COMPLETED'))
    .map((item) => item.courseId))
  const teacherCourseIds = new Set(db.courseTeachers
    .filter((item) => item.teacherId === user.userId)
    .map((item) => item.courseId))

  db.announcements
    .filter((row) => row.status === 'PUBLISHED')
    .filter((row) => {
      if (role === 'ADMIN') return row.scopeType === 'SYSTEM'
      if (role === 'TEACHER') {
        return (row.scopeType === 'SYSTEM' && (row.audience === 'ALL' || row.audience === 'TEACHER'))
          || (row.scopeType === 'COURSE' && row.audience === 'ALL' && Boolean(row.courseId && teacherCourseIds.has(row.courseId)))
      }
      return (row.scopeType === 'SYSTEM' && (row.audience === 'ALL' || row.audience === 'STUDENT'))
        || (row.scopeType === 'COURSE' && row.audience !== 'TEACHER' && Boolean(row.courseId && studentCourseIds.has(row.courseId)))
    })
    .forEach((row) => rows.push(notification(
      `announcement-${row.announcementId}`,
      row.title,
      row.content,
      row.scopeType === 'COURSE' ? 'COURSE' : 'SYSTEM',
      'ANNOUNCEMENT',
      row.publishedAt,
      { announcementId: row.announcementId, courseId: row.courseId },
    )))

  if (role === 'STUDENT') {
    db.assignments
      .filter((row) => row.assignmentStatus === 'PUBLISHED' && studentCourseIds.has(row.courseId))
      .forEach((row) => rows.push(notification(
        `assignment-${row.assignmentId}-published`,
        `作业已发布：${row.title}`,
        `请在 ${row.dueAt} 前完成作业。`,
        'ASSIGNMENT', 'ASSIGNMENT_PUBLISHED', row.publishedAt ?? row.openAt ?? row.dueAt,
        { courseId: row.courseId, assignmentId: row.assignmentId },
      )))
    db.exams
      .filter((row) => row.status === 'PUBLISHED' && studentCourseIds.has(row.courseId))
      .forEach((row) => rows.push(notification(
        `exam-${row.examId}-published`,
        `考试安排已发布：${row.title}`,
        `考试时间：${row.startAt} 至 ${row.endAt}`,
        'EXAM', 'EXAM_PUBLISHED', row.startAt,
        { courseId: row.courseId, examId: row.examId },
      )))
    db.warnings
      .filter((row) => row.studentId === user.userId)
      .forEach((row) => rows.push(notification(
        `warning-${row.warningId}-created`,
        `学习预警：${row.summary}`,
        row.suggestion ?? '请查看预警详情。',
        'WARNING', 'WARNING_CREATED', row.generatedAt,
        { courseId: row.courseId, warningId: row.warningId },
      )))
    db.grades
      .filter((row) => row.studentId === user.userId && row.status === 'PUBLISHED')
      .forEach((row) => {
        const assignment = db.assignments.find((item) => item.assignmentId === row.assignmentId)
        rows.push(notification(
          `grade-${row.gradeId}-published`,
          `作业批改已完成：${assignment?.title ?? row.assignmentId}`,
          '成绩已发布，请进入成绩页面查看。',
          'ASSIGNMENT', 'GRADE_PUBLISHED', row.publishedAt ?? nowIso(),
          { courseId: row.courseId, assignmentId: row.assignmentId },
        ))
      })
  }

  if (role === 'TEACHER') {
    db.submissions
      .filter((row) => teacherCourseIds.has(row.courseId) && row.submissionStatus !== 'DRAFT')
      .forEach((row) => {
        const assignment = db.assignments.find((item) => item.assignmentId === row.assignmentId)
        rows.push(notification(
          `submission-${row.submissionId}-submitted`,
          `收到作业提交：${assignment?.title ?? row.assignmentId}`,
          '有新的学生作业等待批改。',
          'ASSIGNMENT', 'ASSIGNMENT_SUBMITTED', row.submittedAt ?? nowIso(),
          { courseId: row.courseId, assignmentId: row.assignmentId },
        ))
      })
    db.warnings
      .filter((row) => teacherCourseIds.has(row.courseId))
      .forEach((row) => rows.push(notification(
        `warning-${row.warningId}-created`,
        `学生学习预警：${row.summary}`,
        '请进入预警中心查看并跟进。',
        'WARNING', 'WARNING_CREATED', row.generatedAt,
        { courseId: row.courseId, warningId: row.warningId },
      )))
  }

  return rows
    .filter((row) => !db.notificationArchives.some((item) => item.userId === user.userId && item.notificationId === row.notificationId))
    .map((row) => {
      const receipt = db.notificationReads.find((item) => item.userId === user.userId && item.notificationId === row.notificationId)
      return { ...row, read: Boolean(receipt), readAt: receipt?.readAt ?? null }
    })
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

function demoUserId() {
  return currentUser(demoRole()).userId
}

function markDemoRead(notificationId: string) {
  const userId = demoUserId()
  if (!demoRows().some((item) => item.notificationId === notificationId)) return
  if (!db.notificationReads.some((item) => item.userId === userId && item.notificationId === notificationId)) {
    db.notificationReads.push({ userId, notificationId, readAt: nowIso() })
    persist()
  }
}

async function consumeSse(response: Response, onEvent: (event: NotificationStreamEvent) => void) {
  if (!response.body) throw new Error('实时消息响应缺少数据流。')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const chunk = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const data = chunk.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n')
      if (data) onEvent(JSON.parse(data) as NotificationStreamEvent)
      boundary = buffer.indexOf('\n\n')
    }
  }
}

function subscribeRealTime(
  onEvent: (event: NotificationStreamEvent) => void,
  onError?: (error: unknown) => void,
) {
  let stopped = false
  let controller: AbortController | undefined
  const connect = async () => {
    while (!stopped) {
      controller = new AbortController()
      const traceId = createTraceId()
      try {
        const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)
        const response = await fetch(`${gateway}/api/v1/notifications/stream`, {
          headers: {
            Accept: 'text/event-stream',
            'X-Trace-Id': traceId,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          signal: controller.signal,
        })
        if (response.status === 401) {
          // 实时消息流是非关键能力：其鉴权失败不应判定整个会话失效。
          // 真正的令牌过期由主请求通道 httpClient 统一处理并触发登出；
          // 这里仅停止重连，避免一次 SSE 401 把刚登录的有效会话踢回登录页。
          stopped = true
          onError?.(new RuntimeError('实时消息连接未授权，已停止重连。', traceId, 'HTTP_401', 401))
          return
        }
        if (!response.ok) throw new RuntimeError('实时消息连接失败。', traceId, `HTTP_${response.status}`, response.status)
        await consumeSse(response, onEvent)
      } catch (error) {
        if (!stopped && !(error instanceof DOMException && error.name === 'AbortError')) onError?.(error)
      }
      if (!stopped) await new Promise((resolve) => window.setTimeout(resolve, 3000))
    }
  }
  void connect()
  return () => {
    stopped = true
    controller?.abort()
  }
}

export const notificationsApi = {
  async list(query: NotificationListQuery = {}): Promise<PageResponse<NotificationVO>> {
    if (isRealMode()) return get<PageResponse<NotificationVO>>('/api/v1/notifications', { ...query })
    const filtered = demoRows()
      .filter((item) => !query.category || item.category === query.category)
      .filter((item) => !query.unread || !item.read)
    return demoDelay(paginate(filtered, query))
  },

  async unreadCount(): Promise<number> {
    if (isRealMode()) return get<number>('/api/v1/notifications/unread-count')
    return demoDelay(demoRows().filter((item) => !item.read).length)
  },

  async markRead(notificationId: string): Promise<void> {
    if (isRealMode()) return post<void>(`/api/v1/notifications/${notificationId}/read`)
    markDemoRead(notificationId)
    return demoDelay(undefined)
  },

  async markAllRead(): Promise<void> {
    if (isRealMode()) return post<void>('/api/v1/notifications/read-all')
    demoRows().forEach((item) => markDemoRead(item.notificationId))
    return demoDelay(undefined)
  },

  async archive(notificationId: string): Promise<void> {
    if (isRealMode()) return post<void>(`/api/v1/notifications/${notificationId}/archive`)
    const userId = demoUserId()
    if (!db.notificationArchives.some((item) => item.userId === userId && item.notificationId === notificationId)) {
      db.notificationArchives.push({ userId, notificationId, archivedAt: nowIso() })
      persist()
    }
    return demoDelay(undefined)
  },

  async preferences(): Promise<NotificationPreferencesVO> {
    if (isRealMode()) return get<NotificationPreferencesVO>('/api/v1/notifications/preferences')
    const row = db.notificationPreferences.find((item) => item.userId === demoUserId())
    return demoDelay({ enabledCategories: [...(row?.enabledCategories ?? allCategories)] })
  },

  async updatePreferences(body: UpdateNotificationPreferencesRequest): Promise<NotificationPreferencesVO> {
    if (isRealMode()) return put<NotificationPreferencesVO>('/api/v1/notifications/preferences', body)
    const userId = demoUserId()
    const row = db.notificationPreferences.find((item) => item.userId === userId)
    if (row) row.enabledCategories = [...body.enabledCategories]
    else db.notificationPreferences.push({ userId, enabledCategories: [...body.enabledCategories] })
    persist()
    return demoDelay({ enabledCategories: [...body.enabledCategories] })
  },

  subscribe(onEvent: (event: NotificationStreamEvent) => void, onError?: (error: unknown) => void) {
    if (!isRealMode()) return () => undefined
    return subscribeRealTime(onEvent, onError)
  },
}
