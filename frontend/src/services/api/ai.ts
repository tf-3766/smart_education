// 4.4 AI 接口。
// 真实模式统一请求 edu-ai-service：课程问答 SSE、课时摘要、批改评语、预警解读、
// 组卷建议、服务状态。演示模式回退本地合成数据。
// AI 只返回草稿与建议，正式业务数据必须经人工确认后由业务接口写入。
import { demoDelay } from '../runtime'
import { get, isRealMode, post, postEventStream } from './client'
import { db, nextId, notFound, nowIso } from './demo/db'
import type { AdminCourseComplianceItemVO, AdminGovernanceDraftRequest, AdminGovernanceDraftVO, AdminTeacherReviewItemVO, AiActionEvent, AiCapabilityEvent, AiCitationVO, AiDraftVO, AssistantChatRequest, AiKnowledgeBaseStatusVO, AiServiceStatusVO, AiStreamEvent, BatchGradingDraftRequest, BatchGradingDraftVO, CourseQaRequest, LessonSummaryRequest, PaperSuggestionRequest } from './types'

function draft(draftType: string, businessId: string, content: string, citations: AiCitationVO[] = []): AiDraftVO {
  return {
    requestId: nextId(),
    draftType,
    businessId,
    content,
    provider: 'demo',
    model: null,
    status: 'DRAFT',
    citations,
    createdAt: nowIso(),
  }
}

function lessonCitations(courseId: string, lessonId?: string | null): AiCitationVO[] {
  const lesson = db.lessons.find((item) => (lessonId ? item.lessonId === lessonId : item.courseId === courseId && item.status === 'PUBLISHED'))
  if (!lesson) return []
  return [{ resourceType: 'LESSON', resourceId: lesson.lessonId, title: lesson.title, locator: null }]
}

async function emitDemoStream(requestId: string, chunks: string[], citations: AiCitationVO[], onEvent: (event: AiStreamEvent) => void, capabilities?: AiCapabilityEvent[]): Promise<void> {
  const emit = (type: AiStreamEvent['type'], data: unknown) => onEvent({ type, requestId, data, timestamp: nowIso() })
  emit('meta', { provider: 'demo', model: null, vectorStoreConfigured: true, toolCallingConfigured: true })
  emit('capability', capabilities ?? demoCapabilities(demoRole(), null))
  emit('tool', { toolName: 'courseKnowledgeSearch', status: 'COMPLETED', input: null, summary: '已检索演示课程知识', result: citations })
  for (const chunk of chunks) {
    await demoDelay(undefined, 60)
    emit('delta', chunk)
  }
  for (const citation of citations) emit('citation', citation)
  emit('done', null)
}

const governanceReason: Record<string, string> = {
  NO_MATERIALS: '课程尚无可供审核的资料，RAG 问答和依据核验不完整',
  NO_LESSONS: '课程尚未配置课时，无法核验教学内容结构',
  NO_LESSON_CONTENT: '课时存在但没有可核验的正文内容',
  MATERIAL_TEXT_UNAVAILABLE: '课程资料存在但没有可核验的抽取正文',
  REVIEW_NOT_APPROVED: '课程审核状态不是 APPROVED，不能视为已完成合规审核',
  UNKNOWN_STATUS: '课程状态不在受支持的生命周期内',
  MISSING_SUMMARY: '课程简介为空，无法核验课程目标与内容范围',
  MISSING_CATEGORY: '课程未设置分类',
  MISSING_TERM: '课程未设置学期',
  MISSING_DEPARTMENT: '课程未设置开课院系',
  INVALID_CREDIT: '课程学分缺失或不大于 0',
  MISSING_COURSE_WINDOW: '课程开始或结束时间未完整设置',
  INVALID_COURSE_WINDOW: '课程结束时间必须晚于开始时间',
  MISSING_ENROLLMENT_WINDOW: '选课开放或截止时间未完整设置',
  INVALID_ENROLLMENT_WINDOW: '选课截止时间必须晚于开放时间',
}

function demoTeacherGovernanceItem(userId: string, criteria?: string | null): AdminTeacherReviewItemVO {
  const candidate = db.users.find((item) => item.userId === userId && item.userStatus === 'PENDING')
  if (!candidate) return {
    userId, targetVersion: null, username: null, displayName: null, registeredAt: null,
    candidate: '未在当前待审核快照中找到该教师', recommendation: 'NOT_ELIGIBLE', confidence: 1,
    reviewRequired: true, riskCodes: ['NOT_PENDING'], reasons: ['账号可能已被其他管理员处理或目标 ID 不属于待审核教师，请刷新列表'],
    evidence: ['当前授权待审核教师快照中无此 ID'],
  }
  const label = `${candidate.displayName}（用户名 ${candidate.username}，用户ID ${userId}，版本 ${candidate.version}）`
  return {
    userId, targetVersion: candidate.version, username: candidate.username, displayName: candidate.displayName,
    registeredAt: candidate.createdAt, candidate: label, recommendation: 'MANUAL_REVIEW', confidence: 0.65,
    reviewRequired: true, riskCodes: ['IDENTITY_EVIDENCE_REQUIRED'],
    reasons: ['账号仍处于待审核状态；正式通过或驳回必须由管理员逐项确认', ...(criteria?.trim() ? [`人工复核标准：${criteria.trim().slice(0, 300)}`] : [])],
    evidence: [label],
  }
}

function demoCourseGovernanceItem(courseId: string): AdminCourseComplianceItemVO {
  const course = db.courses.find((item) => item.courseId === courseId)
  if (!course) return {
    courseId, targetVersion: null, courseCode: '', courseName: '未知课程', courseStatus: 'UNKNOWN', reviewStatus: 'UNKNOWN',
    summary: null, categoryId: null, term: null, department: null, credit: null,
    enrollmentOpenAt: null, enrollmentCloseAt: null, startAt: null, endAt: null,
    lessonCount: 0, materialCount: 0, readinessScore: 0, recommendation: 'UNAVAILABLE', failed: true, reviewRequired: true,
    issueCodes: ['CONTEXT_UNAVAILABLE'], reasons: ['课程不存在或当前管理员无权访问'], evidence: [],
  }
  const lessons = db.lessons.filter((item) => item.courseId === courseId)
  const materials = db.materials.filter((item) => item.courseId === courseId)
  const issues: Array<[string, number]> = []
  if (!materials.length) issues.push(['NO_MATERIALS', 35])
  if (!lessons.length) issues.push(['NO_LESSONS', 35])
  else if (!lessons.some((item) => item.content?.trim())) issues.push(['NO_LESSON_CONTENT', 20])
  if (materials.length && !materials.some((item) => item.extractedText?.trim())) issues.push(['MATERIAL_TEXT_UNAVAILABLE', 20])
  if (course.reviewStatus !== 'APPROVED') issues.push(['REVIEW_NOT_APPROVED', 15])
  if (!['DRAFT', 'PUBLISHED', 'ARCHIVED'].includes(course.status)) issues.push(['UNKNOWN_STATUS', 15])
  if (!course.summary?.trim()) issues.push(['MISSING_SUMMARY', 10])
  if (course.categoryId == null) issues.push(['MISSING_CATEGORY', 10])
  if (!course.term?.trim()) issues.push(['MISSING_TERM', 10])
  if (!course.department?.trim()) issues.push(['MISSING_DEPARTMENT', 10])
  if (course.credit == null || course.credit <= 0) issues.push(['INVALID_CREDIT', 10])
  if (!course.startAt || !course.endAt) issues.push(['MISSING_COURSE_WINDOW', 10])
  else if (Date.parse(course.endAt) <= Date.parse(course.startAt)) issues.push(['INVALID_COURSE_WINDOW', 15])
  if (!course.enrollmentOpenAt || !course.enrollmentCloseAt) issues.push(['MISSING_ENROLLMENT_WINDOW', 10])
  else if (Date.parse(course.enrollmentCloseAt) <= Date.parse(course.enrollmentOpenAt)) issues.push(['INVALID_ENROLLMENT_WINDOW', 15])
  const issueCodes = issues.map(([code]) => code)
  const evidence = [
    `课程状态=${course.status}，审核状态=${course.reviewStatus}`,
    `版本=${course.version}，学期=${course.term ?? '-'}，分类=${course.categoryId ?? '-'}，院系=${course.department ?? '-'}，学分=${course.credit ?? '-'}`,
    `简介=${course.summary ?? '-'}`,
    `课程时间=${course.startAt ?? '-'} 至 ${course.endAt ?? '-'}，选课时间=${course.enrollmentOpenAt ?? '-'} 至 ${course.enrollmentCloseAt ?? '-'}`,
    `课时数=${lessons.length}，资料数=${materials.length}`,
    ...lessons.slice(0, 3).map((item) => `课时：${item.title}；正文片段=${item.content?.slice(0, 500) ?? '-'}`),
    ...materials.slice(0, 3).map((item) => `资料：${item.name}，抽取状态=${item.extractionStatus ?? '-'}；正文片段=${item.extractedText?.slice(0, 500) ?? '-'}`),
  ]
  return {
    courseId, targetVersion: course.version, courseCode: course.courseCode, courseName: course.name,
    courseStatus: course.status, reviewStatus: course.reviewStatus, lessonCount: lessons.length, materialCount: materials.length,
    summary: course.summary ?? null, categoryId: course.categoryId ?? null, term: course.term ?? null,
    department: course.department ?? null, credit: course.credit ?? null,
    enrollmentOpenAt: course.enrollmentOpenAt ?? null, enrollmentCloseAt: course.enrollmentCloseAt ?? null,
    startAt: course.startAt ?? null, endAt: course.endAt ?? null,
    readinessScore: Math.max(0, 100 - issues.reduce((sum, [, deduction]) => sum + deduction, 0)),
    recommendation: issueCodes.length ? 'REMEDIATE_AND_REVIEW' : 'READY_FOR_ADMIN_REVIEW', failed: false,
    reviewRequired: issueCodes.length > 0, issueCodes, reasons: issueCodes.map((code) => governanceReason[code]), evidence,
  }
}

function demoAdminGovernanceDraft(body: AdminGovernanceDraftRequest): Promise<AdminGovernanceDraftVO> {
  const teacherReviews = [...new Set(body.teacherUserIds ?? [])].map((id) => demoTeacherGovernanceItem(id, body.criteria))
  const courseCompliance = [...new Set(body.courseIds ?? [])].map(demoCourseGovernanceItem)
  const failureCount = courseCompliance.filter((item) => item.failed).length
  return demoDelay({
    requestId: nextId(), status: 'FRAMEWORK_ONLY', totalCount: teacherReviews.length + courseCompliance.length,
    successCount: teacherReviews.length + courseCompliance.length - failureCount, failureCount,
    reviewCount: teacherReviews.filter((item) => item.reviewRequired).length + courseCompliance.filter((item) => item.reviewRequired).length,
    teacherReviews, courseCompliance, createdAt: nowIso(),
  })
}

type DemoRole = 'STUDENT' | 'TEACHER' | 'ADMIN' | 'SUPER_ADMIN'
type CapabilityMode = AiCapabilityEvent['mode']
type CapabilityRisk = AiCapabilityEvent['riskLevel']
type ConfirmationPolicy = NonNullable<AiCapabilityEvent['confirmationPolicy']>

function demoRole(): DemoRole {
  const roles = db.users.find((item) => item.userId === db.session.userId)?.roles ?? ['STUDENT']
  if (roles.includes('SUPER_ADMIN')) return 'SUPER_ADMIN'
  return (roles[0] as DemoRole | undefined) ?? 'STUDENT'
}

function capability(
  capabilityId: string, name: string, description: string, roles: DemoRole[], mode: CapabilityMode,
  riskLevel: CapabilityRisk = 'READ_ONLY', requiredContext: string[] = [],
  confirmationPolicy: ConfirmationPolicy = 'NONE', deepLinkTemplate: string | null = null,
  courseId?: string | null,
): AiCapabilityEvent {
  const requiresCourseContext = requiredContext.includes('courseId')
  const enabled = !requiresCourseContext || Boolean(courseId)
  return { capabilityId, name, description, roles, mode, riskLevel, requiredContext, confirmationPolicy,
    deepLinkTemplate, requiresCourseContext, enabled, unavailableReason: enabled ? null : '进入具体课程后可用' }
}

function demoCapabilities(role: DemoRole, courseId?: string | null): AiCapabilityEvent[] {
  const all: DemoRole[] = ['STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN']
  const admins: DemoRole[] = ['ADMIN', 'SUPER_ADMIN']
  const definitions: AiCapabilityEvent[] = [
    capability('platform.authorized-context', '授权数据问答', '按当前账号权限查询真实业务数据', all, 'ANSWER', 'READ_ONLY', [], 'NONE', null, courseId),
    capability('course.knowledge.qa', '课程知识问答', '检索已授权课时和资料正文并附引用', all, 'ANSWER', 'READ_ONLY', ['courseId'], 'NONE', null, courseId),
    capability('student.learning-overview.query', '学习概览查询', '查询本人课程、进度、成绩和预警', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/dashboard', courseId),
    capability('student.course-enrollment.query', '课程与选课问答', '查询本人课程、可见课程和选课时间', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/courses', courseId),
    capability('student.lesson-material.query', '课时资料问答', '查询本人可访问的章节、课时和资料正文', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/courses', courseId),
    capability('student.task.query', '作业考试查询', '查询本人待办作业和考试安排', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/assignments', courseId),
    capability('student.grade.query', '成绩问答', '查询本人已发布成绩与教师反馈', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/grades', courseId),
    capability('student.warning.query', '学习预警问答', '查询并解释本人的学习预警', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/warnings', courseId),
    capability('student.communication.query', '公告讨论问答', '查询本人课程公告、讨论和通知', ['STUDENT'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/student/forum', courseId),
    capability('teacher.teaching-overview.query', '教学任务查询', '查询负责课程、作业、考试和学习预警', ['TEACHER'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/teacher/dashboard', courseId),
    capability('teacher.course-content.query', '课程内容问答', '查询负责课程的章节、课时、资料与发布状态', ['TEACHER'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/teacher/courses', courseId),
    capability('teacher.grading.query', '批改与成绩问答', '查询负责课程的提交、成绩和反馈状态', ['TEACHER'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/teacher/assignments', courseId),
    capability('teacher.learning-risk.query', '学情预警问答', '查询负责课程的学习进度和预警', ['TEACHER'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/teacher/warnings', courseId),
    capability('teacher.communication.query', '公告讨论问答', '查询负责课程的公告、讨论和通知', ['TEACHER'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/teacher/forum', courseId),
    capability('course.lesson-summary.generate', '课时摘要草稿', '根据授权课时正文生成摘要草稿', ['TEACHER'], 'DRAFT', 'LOW', [], 'DRAFT_REVIEW', '/teacher/courses', courseId),
    capability('course.submission.comment.generate', '单份批改建议', '为授权提交生成评语和分数建议草稿', ['TEACHER'], 'DRAFT', 'MEDIUM', ['submissionId'], 'DRAFT_REVIEW', '/teacher/assignments', courseId),
    capability('course.warning.explain', '预警解读草稿', '根据授权预警证据生成教师解读', ['TEACHER'], 'DRAFT', 'LOW', [], 'DRAFT_REVIEW', '/teacher/warnings', courseId),
    capability('course.teaching-package.plan', '教学包自动流', '基于课程资料规划教案、摘要、作业、题目和公告草稿', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/courses', courseId),
    capability('course.risk-intervention.plan', '风险干预计划', '根据学习预警生成提醒、补救材料和补交任务计划草稿', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/warnings', courseId),
    capability('course.question-bank.create', '生成题库草稿', '基于课程资料创建待审核题库与题目', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/exams', courseId),
    capability('course.assignment.create', '生成作业草稿', '基于课程上下文创建待发布作业', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/assignments', courseId),
    capability('course.exam.create', '生成考试草稿', '创建待编排和发布的考试外壳', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/exams', courseId),
    capability('course.paper-suggestion.generate', '组卷建议草稿', '从授权题库生成题型、难度和分值结构建议', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/exams', courseId),
    capability('course.announcement.create', '生成公告草稿', '创建待确认发布的课程公告', ['TEACHER'], 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/teacher/forum', courseId),
    capability('course.assignment.publish', '确认发布作业', '为已有草稿作业生成发布预览并等待确认', ['TEACHER'], 'ACTION', 'MEDIUM', ['assignmentId'], 'EXPLICIT_CONFIRM', '/teacher/assignments', courseId),
    capability('course.submission.batch-grade-assist', '批量辅助批改', '按评分标准生成建议并将异常答案进入人工复核', ['TEACHER'], 'DRAFT', 'HIGH', [], 'DRAFT_REVIEW', '/teacher/assignments', courseId),
    capability('course.submission.grade', '评分与反馈', '为授权提交生成分数和评语预览，确认后保存或发布', ['TEACHER'], 'ACTION', 'HIGH', ['submissionId'], 'EXPLICIT_CONFIRM', '/teacher/assignments', courseId),
    capability('admin.platform-overview.query', '平台运营查询', '查询用户、课程、审核和教学运营指标', admins, 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/dashboard', courseId),
    capability('admin.course-governance.query', '课程治理问答', '查询课程审核、内容完整度和合规状态', admins, 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/course-reviews', courseId),
    capability('admin.content-governance.query', '内容治理问答', '查询平台公告、讨论与内容治理信号', admins, 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/content', courseId),
    capability('admin.ai-service.query', 'AI 服务问答', '查询模型、知识库与 AI 服务运行状态', admins, 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/ai-settings', courseId),
    capability('admin.statistics.query', '运营统计问答', '查询用户、课程、作业、考试和预警汇总指标', admins, 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/statistics', courseId),
    capability('admin.user-governance.query', '用户治理问答', '查询用户与待审核教师注册', ['SUPER_ADMIN'], 'ANSWER', 'READ_ONLY', [], 'NONE', '/admin/users', courseId),
    capability('platform.term-enrollment-window.upsert', '设置学期选课时间', '预览并确认学期统一选课窗口变更', admins, 'ACTION', 'HIGH', [], 'STRONG_CONFIRM', '/admin/term-enrollment', courseId),
    capability('admin.operations-brief.generate', '运营简报生成', '汇总平台指标、异常信号和可确认处置建议', admins, 'DRAFT', 'LOW', [], 'DRAFT_REVIEW', '/admin/dashboard', courseId),
    capability('admin.teacher-registration.batch-precheck', '教师注册批量预审', '批量核对待审核状态并生成逐项人工复核建议', ['SUPER_ADMIN'], 'DRAFT', 'HIGH', [], 'DRAFT_REVIEW', '/admin/users', courseId),
    capability('admin.course.compliance-check', '课程合规检查', '检查课程审核状态、课时与资料完整度并给出整改建议', admins, 'DRAFT', 'MEDIUM', [], 'DRAFT_REVIEW', '/admin/course-reviews', courseId),
    capability('admin.teacher-registration.review', '教师注册审核', '预览并确认待审核教师的通过或驳回', ['SUPER_ADMIN'], 'ACTION', 'HIGH', ['userId'], 'STRONG_CONFIRM', '/admin/users', courseId),
  ]
  return definitions.filter((item) => item.roles?.includes(role))
}

export const aiApi = {
  async capabilities(courseId?: string | null): Promise<AiCapabilityEvent[]> {
    if (isRealMode()) return get<AiCapabilityEvent[]>('/api/v1/ai/capabilities', courseId ? { courseId } : undefined)
    return demoDelay(demoCapabilities(demoRole(), courseId))
  },
  async assistantStream(body: AssistantChatRequest, onEvent: (event: AiStreamEvent) => void): Promise<void> {
    if (isRealMode()) return postEventStream('/api/v1/ai/assistant/stream', body, onEvent)
    const role = demoRole()
    const roleHint = role === 'TEACHER' ? '教师可从课程管理、作业批改、考试题库和课程互动完成教学闭环。' : role === 'ADMIN' || role === 'SUPER_ADMIN' ? '管理员可处理授权治理、平台统计和 AI 服务状态。' : '学生可从我的课程进入课时资料，并在学习任务和考试安排中完成待办。'
    const answer = `${roleHint} 你当前的问题是“${body.question}”。${body.courseId ? '我会优先结合当前课程知识库与课时上下文回答。' : '如需查询具体课程事实，请先进入对应课程页面。'}`
    return emitDemoStream(nextId(), answer.match(/.{1,24}/g) ?? [answer], [], onEvent, demoCapabilities(role, body.courseId))
  },
  async confirmAction(action: AiActionEvent, confirmationText?: string): Promise<AiActionEvent> {
    if (isRealMode()) return post<AiActionEvent>(`/api/v1/assistant-actions/${action.actionId}/confirm`, {
      confirmationText: confirmationText || null,
    })
    return demoDelay({
      ...action,
      status: 'SUCCEEDED',
      requiresConfirmation: false,
      confirmedAt: nowIso(),
      executedAt: nowIso(),
    })
  },
  async cancelAction(action: AiActionEvent): Promise<AiActionEvent> {
    if (isRealMode()) return post<AiActionEvent>(`/api/v1/assistant-actions/${action.actionId}/cancel`)
    return demoDelay({ ...action, status: 'CANCELLED', requiresConfirmation: false })
  },
  async retryAction(action: AiActionEvent): Promise<AiActionEvent> {
    if (isRealMode()) return post<AiActionEvent>(`/api/v1/assistant-actions/${action.actionId}/retry`)
    return demoDelay({ ...action, actionId: nextId(), status: 'WAITING_CONFIRMATION', requiresConfirmation: true, errorCode: null, errorMessage: null, confirmedAt: null, executedAt: null, createdAt: nowIso() })
  },
  async listActions(limit = 20): Promise<AiActionEvent[]> {
    if (isRealMode()) return get<AiActionEvent[]>('/api/v1/assistant-actions', { limit })
    return demoDelay([])
  },
  async knowledgeBaseStatus(courseId: string): Promise<AiKnowledgeBaseStatusVO> {
    if (isRealMode()) return get<AiKnowledgeBaseStatusVO>(`/api/v1/ai/courses/${courseId}/knowledge-base/status`)
    const lessons = db.lessons.filter((item) => item.courseId === courseId && item.content)
    return demoDelay({ courseId, vectorStoreConfigured: true, indexedChunks: lessons.length, lastSyncedAt: null })
  },

  async syncKnowledgeBase(courseId: string): Promise<AiKnowledgeBaseStatusVO> {
    if (isRealMode()) return post<AiKnowledgeBaseStatusVO>(`/api/v1/ai/courses/${courseId}/knowledge-base/sync`)
    const lessons = db.lessons.filter((item) => item.courseId === courseId && item.content)
    return demoDelay({ courseId, vectorStoreConfigured: true, indexedChunks: lessons.length, lastSyncedAt: nowIso() })
  },
  /** 课程问答（SSE 流式）。演示模式基于课时内容合成回答并附引用。 */
  async qaStream(courseId: string, body: CourseQaRequest, onEvent: (event: AiStreamEvent) => void): Promise<void> {
    if (isRealMode()) return postEventStream(`/api/v1/ai/courses/${courseId}/qa/stream`, body, onEvent)
    const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
    const citations = lessonCitations(courseId, body.lessonId)
    const source = body.lessonId ? db.lessons.find((item) => item.lessonId === body.lessonId) : undefined
    const context = source?.content ?? db.lessons.find((item) => item.courseId === courseId && item.content)?.content
    const answer = context
      ? `围绕「${body.question}」，可以先回到《${course.name}》的相关课时：${context} 建议把问题拆成概念定义、适用条件和一个课堂例子来理解。`
      : `关于「${body.question}」，当前课程《${course.name}》暂无可引用的课时内容，以下建议仅供参考：先明确概念边界，再结合作业中的实际场景验证理解。`
    const chunks = answer.match(/.{1,24}/g) ?? [answer]
    return emitDemoStream(nextId(), chunks, context ? citations : [], onEvent, demoCapabilities(demoRole(), courseId))
  },

  /** 课时摘要草稿（教师）。仅返回草稿，不写入正式课时内容。 */
  async lessonSummaryDraft(lessonId: string, body: LessonSummaryRequest): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>(`/api/v1/ai/lessons/${lessonId}/summary-draft`, body)
    const lesson = db.lessons.find((item) => item.lessonId === lessonId) ?? notFound('课时不存在。')
    return demoDelay(draft(
      'LESSON_SUMMARY',
      lessonId,
      `《${lesson.title}》建议总结为三点：一是掌握核心概念；二是能解释典型应用场景；三是通过练习暴露常见误区。${lesson.content ? `要点素材：${lesson.content}` : ''}`,
      [{ resourceType: 'LESSON', resourceId: lessonId, title: lesson.title, locator: null }],
    ))
  },

  /** AI 服务状态（管理员）。配置来自后端环境变量，前端只读取状态。 */
  async teachingPackagePlan(courseId: string, instruction?: string): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>(`/api/v1/ai/courses/${courseId}/teaching-package-plan`, { instruction: instruction ?? null })
    const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
    return demoDelay(draft('TEACHING_PACKAGE_PLAN', courseId,
      `《${course.name}》教学包执行计划：\n1. 资料梳理：读取已发布课时与资料，核对引用。\n2. 教案设计：生成教学目标、重难点与课堂活动预览。\n3. 课时摘要：逐课时生成摘要草稿。\n4. 作业草稿：生成作业但不发布。\n5. 题库题目：生成 AI 草稿题库并人工复核。\n6. 公告草稿：生成课程公告但不通知学生。\n\n每一步都需要教师确认，发布作业、题库和公告时再次确认。${instruction ? `\n补充要求：${instruction}` : ''}`,
      lessonCitations(courseId)))
  },
  async adminOperationsBrief(instruction?: string): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>('/api/v1/ai/admin/operations-brief', { instruction: instruction ?? null })
    return demoDelay(draft('ADMIN_OPERATIONS_BRIEF', db.session.userId || 'admin',
      `今日运营简报\n核心指标：平台用户、运行课程、待审核课程与开放预警已完成汇总。\n异常信号：当前演示数据未发现 AI 服务中断；开放预警需要教师持续跟进。\n处理建议：优先核对待审核课程与高等级预警。教师审批、课程下线和批量通知必须进入强确认与完整审计。${instruction ? `\n补充要求：${instruction}` : ''}`))
  },

  async warningInterventionPlan(warningId: string, instruction?: string): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>(`/api/v1/ai/warnings/${warningId}/intervention-plan`, { instruction: instruction ?? null })
    const warning = db.warnings.find((item) => item.warningId === warningId) ?? notFound('预警不存在。')
    return demoDelay(draft(
      'RISK_INTERVENTION_PLAN',
      warningId,
      `干预计划草稿：\n1. 学生提醒：以尊重、具体的方式说明“${warning.summary}”。\n2. 补救材料：复习对应课时核心概念并完成一组基础练习。\n3. 补交任务：安排可完成的小任务与明确截止时间。\n4. 复查：教师在约定时间核对学习进度与提交结果。\n\n本计划不会自动通知学生或创建任务，须教师逐项确认。${instruction ? `\n补充要求：${instruction}` : ''}`,
    ))
  },

  async adminGovernanceDraft(body: AdminGovernanceDraftRequest): Promise<AdminGovernanceDraftVO> {
    if (isRealMode()) return post<AdminGovernanceDraftVO>('/api/v1/ai/admin/governance-review-draft', body)
    return demoAdminGovernanceDraft(body)
  },

  async adminStatus(): Promise<AiServiceStatusVO> {
    if (isRealMode()) return get<AiServiceStatusVO>('/api/v1/ai/admin/status')
    return demoDelay({
      serviceStatus: 'UP',
      framework: 'Spring AI',
      frameworkVersion: '1.1.8',
      provider: 'fallback',
      model: 'none',
      modelConfigured: false,
      vectorStoreConfigured: false,
      checkedAt: nowIso(),
    })
  },
  /** 批改评语草稿（教师）。真实模式请求后端；仅返回草稿，须人工确认后再写入。 */
  async commentDraft(submissionId: string, instruction?: string): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>(`/api/v1/ai/submissions/${submissionId}/comment-draft`, { instruction: instruction ?? null })
    const submission = db.submissions.find((item) => item.submissionId === submissionId) ?? notFound('提交不存在。')
    const assignment = db.assignments.find((item) => item.assignmentId === submission.assignmentId)
    const tone = (submission.score ?? 75) >= 85 ? '完成度较高' : (submission.score ?? 75) >= 60 ? '基础目标基本达成' : '仍存在较明显缺口'
    return demoDelay(draft(
      'SUBMISSION_COMMENT',
      submissionId,
      `${tone}。针对《${assignment?.title ?? '本次作业'}》，建议肯定学生已完成的核心部分，同时指出需要补充的证据或测试样例。可写为：思路基本清楚，建议进一步完善边界条件说明，并补充一组反例验证。`,
    ))
  },

  async batchGradingDraft(body: BatchGradingDraftRequest): Promise<BatchGradingDraftVO> {
    if (isRealMode()) return post<BatchGradingDraftVO>('/api/v1/ai/submissions/batch-grading-draft', body)
    const threshold = body.reviewThreshold ?? 0.75
    const items = body.submissionIds.map((submissionId) => {
      const submission = db.submissions.find((item) => item.submissionId === submissionId) ?? notFound('提交不存在。')
      const assignment = db.assignments.find((item) => item.assignmentId === submission.assignmentId) ?? notFound('作业不存在。')
      const contentLength = submission.content?.trim().length ?? 0
      const confidence = contentLength >= 80 ? 0.86 : contentLength >= 40 ? 0.72 : 0.48
      const anomalyCodes = contentLength === 0 ? ['EMPTY_CONTENT'] : contentLength < 40 ? ['SHORT_ANSWER'] : []
      if (confidence < threshold) anomalyCodes.push('LOW_CONFIDENCE')
      const reviewReasons = anomalyCodes.map((code) => code === 'EMPTY_CONTENT'
        ? '提交正文为空或仅包含附件，需要人工查看原始文件'
        : code === 'SHORT_ANSWER' ? '答案内容较短，自动评分依据不足' : `置信度低于人工复核阈值 ${Math.round(threshold * 100)}%`)
      return {
        submissionId,
        assignmentId: submission.assignmentId,
        maxScore: assignment.maxScore,
        suggestedScore: submission.score ?? Math.round(assignment.maxScore * (confidence >= threshold ? 0.82 : 0.68)),
        comment: `依据“${body.rubric}”生成的评语建议：已完成主要任务，建议进一步补充边界情况、过程依据和自检说明。`,
        confidence,
        reviewRequired: anomalyCodes.length > 0,
        anomalyCodes,
        reviewReasons,
        citations: [{ resourceType: 'SUBMISSION', resourceId: submissionId, title: assignment.title, locator: `submission:${submissionId}` }],
      }
    })
    return demoDelay({
      requestId: nextId(), rubric: body.rubric, reviewThreshold: threshold,
      totalCount: items.length, reviewCount: items.filter((item) => item.reviewRequired).length,
      status: 'DRAFT', items, createdAt: nowIso(),
    })
  },

  /** 预警解读草稿（教师）。真实模式请求后端；仅供参考，处理须教师人工确认。 */
  async warningExplanation(warningId: string, instruction?: string): Promise<AiDraftVO> {
    if (isRealMode()) return post<AiDraftVO>(`/api/v1/ai/warnings/${warningId}/explanation`, { instruction: instruction ?? null })
    const warning = db.warnings.find((item) => item.warningId === warningId) ?? notFound('预警不存在。')
    const evidences = warning.evidences.map((item) => item.description).join('；')
    return demoDelay(draft(
      'WARNING_EXPLANATION',
      warningId,
      `该预警（${warning.warningLevel} 级）的成因：${warning.summary} 依据：${evidences || '暂无明细'}。建议：${warning.suggestion ?? '优先与学生沟通确认实际情况，再决定干预方式'}。AI 解读仅供参考，预警处理需教师人工确认。`,
    ))
  },

  /** 组卷建议草稿（教师）。真实模式请求后端；建议为草稿，正式试卷须教师确认生成。 */
  async paperSuggestion(body: PaperSuggestionRequest): Promise<AiDraftVO> {
    // courseId 保持字符串直传，避免雪花 ID 转数字丢精度（Jackson 默认可将字符串强转 Long）。
    if (isRealMode()) return post<AiDraftVO>('/api/v1/ai/exams/paper-suggestions', {
      courseId: body.courseId,
      questionCount: body.questionCount,
      totalScore: body.totalScore,
      requirements: body.requirements ?? null,
    })
    const course = db.courses.find((item) => item.courseId === body.courseId) ?? notFound('课程不存在。')
    const bankIds = db.questionBanks.filter((item) => item.courseId === body.courseId).map((item) => item.bankId)
    const pool = db.questions.filter((item) => bankIds.includes(item.bankId) && item.status === 'ACTIVE')
    return demoDelay(draft(
      'PAPER_SUGGESTION',
      body.courseId,
      `《${course.name}》组卷建议：题库现有可用题目 ${pool.length} 道，目标 ${body.questionCount} 题、总分 ${body.totalScore} 分。建议客观题覆盖基础概念、简答题检验综合能力；${body.requirements ? `结合要求「${body.requirements}」，` : ''}发布前请核对试卷总分与考试总分一致。该建议为草稿，正式试卷须由教师在试卷编排中确认生成。`,
    ))
  },
}
