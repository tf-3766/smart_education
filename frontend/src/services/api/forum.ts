// 4.2 论坛接口（学生发帖回帖 + 教师/管理员可见性治理）
import { demoDelay } from '../runtime'
import { get, isRealMode, patch, post } from './client'
import { assertVersion, cl, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist, userName } from './demo/db'
import type { ReplyRow, TopicRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type {
  ForumReplyCreateRequest,
  ForumReplyVO,
  ForumTopicCreateRequest,
  ForumTopicDetailVO,
  ForumTopicListItemVO,
  ForumTopicListQuery,
  ForumVisibilityRequest,
  PageQuery,
  PageResponse,
} from './types'

function toTopicItem(row: TopicRow): ForumTopicListItemVO {
  const replies = db.forumReplies.filter((item) => item.topicId === row.topicId && item.status === 'VISIBLE')
  const repliedTimes = replies.map((item) => item.createdAt).sort()
  return {
    topicId: row.topicId,
    courseId: row.courseId,
    title: row.title,
    authorId: row.authorId,
    authorName: userName(row.authorId),
    status: cl(row.status),
    pinned: row.pinned,
    replyCount: replies.length,
    lastRepliedAt: repliedTimes.length ? repliedTimes[repliedTimes.length - 1] : null,
    createdAt: row.createdAt,
    version: row.version,
  }
}

function toTopicDetail(row: TopicRow): ForumTopicDetailVO {
  return {
    topicId: row.topicId,
    courseId: row.courseId,
    title: row.title,
    content: row.content,
    authorId: row.authorId,
    authorName: userName(row.authorId),
    status: cl(row.status),
    moderationReason: row.moderationReason ?? null,
    moderatedBy: row.moderatedBy ?? null,
    moderatedAt: row.moderatedAt ?? null,
    createdAt: row.createdAt,
    version: row.version,
  }
}

function toReplyVO(row: ReplyRow): ForumReplyVO {
  return {
    replyId: row.replyId,
    topicId: row.topicId,
    courseId: row.courseId,
    authorId: row.authorId,
    authorName: userName(row.authorId),
    parentReplyId: row.parentReplyId ?? null,
    content: row.content,
    status: cl(row.status),
    moderationReason: row.moderationReason ?? null,
    moderatedBy: row.moderatedBy ?? null,
    moderatedAt: row.moderatedAt ?? null,
    createdAt: row.createdAt,
    version: row.version,
  }
}

function requireEnrolled(courseId: string): string {
  const student = currentUser('STUDENT')
  const enrolled = db.enrollments.some((item) => item.courseId === courseId && item.studentId === student.userId && item.status === 'ENROLLED')
  if (!enrolled) conflict('尚未选修该课程。', 'FORBIDDEN')
  return student.userId
}

function filterTopics(courseId: string, query: ForumTopicListQuery, includeHidden: boolean): ForumTopicListItemVO[] {
  const keyword = query.keyword?.trim().toLowerCase()
  return db.forumTopics
    .filter((row) => row.courseId === courseId)
    .filter((row) => (query.status ? row.status === query.status : includeHidden || row.status === 'VISIBLE'))
    .filter((row) => !keyword || row.title.toLowerCase().includes(keyword))
    .sort((a, b) => Number(b.pinned) - Number(a.pinned) || b.createdAt.localeCompare(a.createdAt))
    .map(toTopicItem)
}

function applyVisibility(row: TopicRow | ReplyRow, body: ForumVisibilityRequest, moderatorRole: 'TEACHER' | 'ADMIN') {
  assertVersion(row, body.version)
  const moderator = currentUser(moderatorRole)
  Object.assign(row, {
    status: body.visible ? 'VISIBLE' : 'HIDDEN',
    moderationReason: body.visible ? null : body.reason ?? null,
    moderatedBy: moderator.userId,
    moderatedAt: nowIso(),
    version: row.version + 1,
  })
  persist()
}

export const forumApi = {
  // —— 学生端 ——
  async studentTopics(courseId: string, query: ForumTopicListQuery = {}): Promise<PageResponse<ForumTopicListItemVO>> {
    if (isRealMode()) return get<PageResponse<ForumTopicListItemVO>>(`/api/v1/student/courses/${courseId}/forum/topics`, { ...query })
    requireEnrolled(courseId)
    return demoDelay(paginate(filterTopics(courseId, query, false), query))
  },

  async createTopic(courseId: string, body: ForumTopicCreateRequest): Promise<ForumTopicDetailVO> {
    if (isRealMode()) return post<ForumTopicDetailVO>(`/api/v1/student/courses/${courseId}/forum/topics`, body)
    const studentId = requireEnrolled(courseId)
    const row: TopicRow = { topicId: nextId(), courseId, title: body.title, content: body.content, authorId: studentId, status: 'VISIBLE', pinned: false, createdAt: nowIso(), version: 0 }
    db.forumTopics.push(row)
    persist()
    return demoDelay(toTopicDetail(row))
  },

  async topicDetail(topicId: string): Promise<ForumTopicDetailVO> {
    if (isRealMode()) return get<ForumTopicDetailVO>(`/api/v1/student/forum/topics/${topicId}`)
    const row = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    const student = currentUser('STUDENT')
    if (row.status === 'HIDDEN' && row.authorId !== student.userId) notFound('主题不存在或已被隐藏。')
    return demoDelay(toTopicDetail(row))
  },

  async listReplies(topicId: string, query: PageQuery & { status?: string } = {}): Promise<PageResponse<ForumReplyVO>> {
    if (isRealMode()) return get<PageResponse<ForumReplyVO>>(`/api/v1/student/forum/topics/${topicId}/replies`, { ...query })
    const rows = db.forumReplies
      .filter((row) => row.topicId === topicId)
      .filter((row) => (query.status ? row.status === query.status : row.status === 'VISIBLE'))
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
    return demoDelay(paginate(rows.map(toReplyVO), query))
  },

  async createReply(topicId: string, body: ForumReplyCreateRequest): Promise<ForumReplyVO> {
    if (isRealMode()) return post<ForumReplyVO>(`/api/v1/student/forum/topics/${topicId}/replies`, body)
    const topic = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    if (topic.status === 'HIDDEN') conflict('主题已被隐藏，不能回复。', 'OPERATION_NOT_ALLOWED')
    const studentId = requireEnrolled(topic.courseId)
    const row: ReplyRow = { replyId: nextId(), topicId, courseId: topic.courseId, authorId: studentId, parentReplyId: body.parentReplyId ?? null, content: body.content, status: 'VISIBLE', createdAt: nowIso(), version: 0 }
    db.forumReplies.push(row)
    persist()
    return demoDelay(toReplyVO(row))
  },

  // —— 教师端 ——
  async teacherTopics(courseId: string, query: ForumTopicListQuery = {}): Promise<PageResponse<ForumTopicListItemVO>> {
    if (isRealMode()) return get<PageResponse<ForumTopicListItemVO>>(`/api/v1/teacher/courses/${courseId}/forum/topics`, { ...query })
    requireTeacherCourse(courseId)
    return demoDelay(paginate(filterTopics(courseId, query, true), query))
  },

  /** 教师查看主题详情（含隐藏帖，走 /teacher 路径；学生请用 topicDetail）。 */
  async teacherTopicDetail(topicId: string): Promise<ForumTopicDetailVO> {
    if (isRealMode()) return get<ForumTopicDetailVO>(`/api/v1/teacher/forum/topics/${topicId}`)
    const row = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    requireTeacherCourse(row.courseId)
    return demoDelay(toTopicDetail(row))
  },

  /** 教师查看主题回复（含隐藏回复）。 */
  async teacherListReplies(topicId: string, query: PageQuery & { status?: string } = {}): Promise<PageResponse<ForumReplyVO>> {
    if (isRealMode()) return get<PageResponse<ForumReplyVO>>(`/api/v1/teacher/forum/topics/${topicId}/replies`, { ...query })
    const topic = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    requireTeacherCourse(topic.courseId)
    const rows = db.forumReplies
      .filter((row) => row.topicId === topicId)
      .filter((row) => !query.status || row.status === query.status)
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
    return demoDelay(paginate(rows.map(toReplyVO), query))
  },

  /** 教师在主题下回帖。 */
  async teacherCreateReply(topicId: string, body: ForumReplyCreateRequest): Promise<ForumReplyVO> {
    if (isRealMode()) return post<ForumReplyVO>(`/api/v1/teacher/forum/topics/${topicId}/replies`, body)
    const topic = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    if (topic.status === 'HIDDEN') conflict('主题已被隐藏，不能回复。', 'OPERATION_NOT_ALLOWED')
    requireTeacherCourse(topic.courseId)
    const teacher = currentUser('TEACHER')
    const row: ReplyRow = { replyId: nextId(), topicId, courseId: topic.courseId, authorId: teacher.userId, parentReplyId: body.parentReplyId ?? null, content: body.content, status: 'VISIBLE', createdAt: nowIso(), version: 0 }
    db.forumReplies.push(row)
    persist()
    return demoDelay(toReplyVO(row))
  },

  async teacherTopicVisibility(topicId: string, body: ForumVisibilityRequest): Promise<ForumTopicDetailVO> {
    if (isRealMode()) return patch<ForumTopicDetailVO>(`/api/v1/teacher/forum/topics/${topicId}/visibility`, body)
    const row = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    requireTeacherCourse(row.courseId)
    applyVisibility(row, body, 'TEACHER')
    return demoDelay(toTopicDetail(row))
  },

  async teacherReplyVisibility(replyId: string, body: ForumVisibilityRequest): Promise<ForumReplyVO> {
    if (isRealMode()) return patch<ForumReplyVO>(`/api/v1/teacher/forum/replies/${replyId}/visibility`, body)
    const row = db.forumReplies.find((item) => item.replyId === replyId) ?? notFound('回复不存在。')
    requireTeacherCourse(row.courseId)
    applyVisibility(row, body, 'TEACHER')
    return demoDelay(toReplyVO(row))
  },

  // —— 管理员端 ——
  async adminTopicVisibility(topicId: string, body: ForumVisibilityRequest): Promise<ForumTopicDetailVO> {
    if (isRealMode()) return patch<ForumTopicDetailVO>(`/api/v1/admin/forum/topics/${topicId}/visibility`, body)
    const row = db.forumTopics.find((item) => item.topicId === topicId) ?? notFound('主题不存在。')
    applyVisibility(row, body, 'ADMIN')
    return demoDelay(toTopicDetail(row))
  },

  async adminReplyVisibility(replyId: string, body: ForumVisibilityRequest): Promise<ForumReplyVO> {
    if (isRealMode()) return patch<ForumReplyVO>(`/api/v1/admin/forum/replies/${replyId}/visibility`, body)
    const row = db.forumReplies.find((item) => item.replyId === replyId) ?? notFound('回复不存在。')
    applyVisibility(row, body, 'ADMIN')
    return demoDelay(toReplyVO(row))
  },
}
