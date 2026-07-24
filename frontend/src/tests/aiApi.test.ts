import { afterEach, describe, expect, it, vi } from 'vitest'
import { aiApi } from '@/services/api'
import { db } from '@/services/api/demo/db'

function ok(data: unknown) {
  return new Response(JSON.stringify({ code: 'SUCCESS', message: 'OK', data, traceId: 't' }), {
    status: 200, headers: { 'Content-Type': 'application/json' },
  })
}
const aiDraft = { requestId: 'r1', draftType: 'X', businessId: 'b1', content: 'AI 草稿', provider: 'dashscope', model: 'qwen-plus', status: 'DRAFT', citations: [], createdAt: '2026-07-15T00:00:00Z' }

describe('教师端 AI 草稿接口 · 真实模式切换', () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); localStorage.clear() })

  function stub() {
    const fetchMock = vi.fn().mockResolvedValue(ok(aiDraft))
    vi.stubEnv('VITE_API_MODE', 'real')
    vi.stubGlobal('fetch', fetchMock)
    return fetchMock
  }

  it('批改评语 POST 到真实端点并带 instruction 体', async () => {
    const fetchMock = stub()
    await aiApi.commentDraft('501', '语气鼓励一些')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/submissions/501/comment-draft')
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ instruction: '语气鼓励一些' })
  })

  it('预警解读 POST 到真实端点（无自定义要求时 instruction 为 null）', async () => {
    const fetchMock = stub()
    await aiApi.warningExplanation('701')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/warnings/701/explanation')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ instruction: null })
  })

  it('组卷建议 POST 到真实端点，courseId 字符串直传保雪花精度', async () => {
    const fetchMock = stub()
    await aiApi.paperSuggestion({ courseId: '2076677237032816641', questionCount: 10, totalScore: 100, requirements: '覆盖第一章' })
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/exams/paper-suggestions')
    const body = JSON.parse(fetchMock.mock.calls[0][1].body)
    expect(body).toEqual({ courseId: '2076677237032816641', questionCount: 10, totalScore: 100, requirements: '覆盖第一章' })
    // 雪花 ID 不能因转数字丢精度
    expect(fetchMock.mock.calls[0][1].body).toContain('2076677237032816641')
  })
  it('知识库状态与同步分别使用 GET 和 POST 真实端点', async () => {
    const fetchMock = stub()
    await aiApi.knowledgeBaseStatus('21001')
    await aiApi.syncKnowledgeBase('21001')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/courses/21001/knowledge-base/status')
    expect(fetchMock.mock.calls[0][1].method ?? 'GET').toBe('GET')
    expect(String(fetchMock.mock.calls[1][0])).toContain('/api/v1/ai/courses/21001/knowledge-base/sync')
    expect(fetchMock.mock.calls[1][1].method).toBe('POST')
  })

  it('教学包计划 POST 到课程真实端点', async () => {
    const fetchMock = stub()
    await aiApi.teachingPackagePlan('21001', '面向零基础学生')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/courses/21001/teaching-package-plan')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ instruction: '面向零基础学生' })
  })

  it('管理员运营简报 POST 到管理员真实端点', async () => {
    const fetchMock = stub()
    await aiApi.adminOperationsBrief('优先检查服务异常')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/admin/operations-brief')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ instruction: '优先检查服务异常' })
  })
  it('风险干预计划 POST 到预警真实端点', async () => {
    const fetchMock = stub()
    await aiApi.warningInterventionPlan('701', '三天后复查')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/warnings/701/intervention-plan')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ instruction: '三天后复查' })
  })

  it('批量辅助批改 POST 结构化评分标准和复核阈值', async () => {
    const fetchMock = stub()
    await aiApi.batchGradingDraft({ submissionIds: ['32001', '32002'], rubric: '概念准确 60 分，示例完整 40 分', reviewThreshold: 0.8 })
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/submissions/batch-grading-draft')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ submissionIds: ['32001', '32002'], rubric: '概念准确 60 分，示例完整 40 分', reviewThreshold: 0.8 })
  })

  it('管理端治理预审 POST 教师和课程 ID，且不调用正式审批接口', async () => {
    const fetchMock = stub()
    await aiApi.adminGovernanceDraft({ teacherUserIds: ['1002'], courseIds: ['21001'], criteria: '核验身份与课程资料完整度' })
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/admin/governance-review-draft')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ teacherUserIds: ['1002'], courseIds: ['21001'], criteria: '核验身份与课程资料完整度' })
    expect(String(fetchMock.mock.calls[0][0])).not.toContain('/approve')
  })

  it('助手打开时从统一端点加载角色能力目录', async () => {
    const fetchMock = stub()
    await aiApi.capabilities('21001')
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/capabilities')
    expect(String(fetchMock.mock.calls[0][0])).toContain('courseId=21001')
    expect(fetchMock.mock.calls[0][1].method ?? 'GET').toBe('GET')
  })

  it('演示模式提供完整角色能力并识别超级管理员', async () => {
    vi.stubEnv('VITE_API_MODE', 'demo')
    const originalUserId = db.session.userId
    try {
      db.session.userId = db.users.find((item) => item.roles.includes('STUDENT'))!.userId
      const student = await aiApi.capabilities(null)
      expect(student.every((item) => item.mode === 'ANSWER')).toBe(true)
      expect(student.map((item) => item.capabilityId)).toEqual(expect.arrayContaining([
        'student.learning-overview.query', 'student.task.query', 'student.grade.query',
        'student.warning.query', 'student.communication.query',
      ]))

      db.session.userId = db.users.find((item) => item.roles.includes('TEACHER'))!.userId
      const teacher = await aiApi.capabilities(null)
      expect(teacher.map((item) => item.capabilityId)).toEqual(expect.arrayContaining([
        'course.lesson-summary.generate', 'course.question-bank.create',
        'course.submission.batch-grade-assist', 'course.submission.grade',
      ]))

      db.session.userId = db.users.find((item) => item.roles.includes('SUPER_ADMIN'))!.userId
      const superAdmin = await aiApi.capabilities(null)
      expect(superAdmin.map((item) => item.capabilityId)).toEqual(expect.arrayContaining([
        'admin.user-governance.query', 'admin.teacher-registration.batch-precheck',
        'admin.teacher-registration.review',
      ]))
    } finally {
      db.session.userId = originalUserId
    }
  })

  it('演示模式与后端对齐教师和课程的目标版本及合规规则', async () => {
    vi.stubEnv('VITE_API_MODE', 'demo')
    const result = await aiApi.adminGovernanceDraft({ teacherUserIds: ['7'], courseIds: ['21001', '21003'] })

    expect(result.teacherReviews[0]).toMatchObject({
      userId: '7', targetVersion: 0, username: 'teacher.gao', recommendation: 'MANUAL_REVIEW',
    })
    expect(result.teacherReviews[0].registeredAt).toBeTruthy()
    expect(result.courseCompliance.find((item) => item.courseId === '21001')).toMatchObject({
      targetVersion: 2, summary: expect.any(String), categoryId: '1', term: expect.any(String),
      department: expect.any(String), credit: 3, enrollmentOpenAt: expect.any(String),
      enrollmentCloseAt: expect.any(String), startAt: expect.any(String), endAt: expect.any(String),
      readinessScore: 100, issueCodes: [], reviewRequired: false,
    })
    expect(result.courseCompliance.find((item) => item.courseId === '21003')).toMatchObject({
      targetVersion: 1, readinessScore: 30,
      issueCodes: ['NO_MATERIALS', 'REVIEW_NOT_APPROVED', 'MISSING_COURSE_WINDOW', 'MISSING_ENROLLMENT_WINDOW'],
      reviewRequired: true,
    })
  })

  it('演示模式检查非法选课时间窗并保持与后端相同扣分', async () => {
    vi.stubEnv('VITE_API_MODE', 'demo')
    const course = db.courses.find((item) => item.courseId === '21001')!
    const originalCloseAt = course.enrollmentCloseAt
    course.enrollmentCloseAt = course.enrollmentOpenAt
    try {
      const result = await aiApi.adminGovernanceDraft({ courseIds: [course.courseId] })
      expect(result.courseCompliance[0]).toMatchObject({
        readinessScore: 85, issueCodes: ['INVALID_ENROLLMENT_WINDOW'], reviewRequired: true,
      })
    } finally {
      course.enrollmentCloseAt = originalCloseAt
    }
  })

})
