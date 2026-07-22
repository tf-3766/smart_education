// 4.4 AI 接口。
// 真实模式统一请求 edu-ai-service：课程问答 SSE、课时摘要、批改评语、预警解读、
// 组卷建议、服务状态。演示模式回退本地合成数据。
// AI 只返回草稿与建议，正式业务数据必须经人工确认后由业务接口写入。
import { demoDelay } from '../runtime'
import { get, isRealMode, post, postEventStream } from './client'
import { db, nextId, notFound, nowIso } from './demo/db'
import type { AiActionEvent, AiCitationVO, AiDraftVO, AssistantChatRequest, AiKnowledgeBaseStatusVO, AiServiceStatusVO, AiStreamEvent, CourseQaRequest, LessonSummaryRequest, PaperSuggestionRequest } from './types'

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

async function emitDemoStream(requestId: string, chunks: string[], citations: AiCitationVO[], onEvent: (event: AiStreamEvent) => void): Promise<void> {
  const emit = (type: AiStreamEvent['type'], data: unknown) => onEvent({ type, requestId, data, timestamp: nowIso() })
  emit('meta', { provider: 'demo', model: null, vectorStoreConfigured: true, toolCallingConfigured: true })
  emit('capability', [{ capabilityId: 'platform.authorized-context', name: '授权数据问答', mode: 'ANSWER', riskLevel: 'READ_ONLY', requiresCourseContext: false, enabled: true }])
  emit('tool', { toolName: 'courseKnowledgeSearch', status: 'COMPLETED', input: null, summary: '已检索演示课程知识', result: citations })
  for (const chunk of chunks) {
    await demoDelay(undefined, 60)
    emit('delta', chunk)
  }
  for (const citation of citations) emit('citation', citation)
  emit('done', null)
}

export const aiApi = {
  async assistantStream(body: AssistantChatRequest, onEvent: (event: AiStreamEvent) => void): Promise<void> {
    if (isRealMode()) return postEventStream('/api/v1/ai/assistant/stream', body, onEvent)
    const role = db.users.find((item) => item.userId === db.session.userId)?.roles[0] ?? 'STUDENT'
    const roleHint = role === 'TEACHER' ? '教师可从课程管理、作业批改、考试题库和课程互动完成教学闭环。' : role === 'ADMIN' ? '管理员可处理教师审核、平台统计和 AI 服务状态。' : '学生可从我的课程进入课时资料，并在学习任务和考试安排中完成待办。'
    const answer = `${roleHint} 你当前的问题是“${body.question}”。${body.courseId ? '我会优先结合当前课程知识库与课时上下文回答。' : '如需查询具体课程事实，请先进入对应课程页面。'}`
    return emitDemoStream(nextId(), answer.match(/.{1,24}/g) ?? [answer], [], onEvent)
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
    return emitDemoStream(nextId(), chunks, context ? citations : [], onEvent)
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
