// 4.2 学习预警接口（规则生成 + 查询 + 处理；AI 不得直接关闭预警）
import { demoDelay } from '../runtime'
import { get, isRealMode, post } from './client'
import { assertVersion, currentUser, db, nextId, notFound, nowIso, paginate, persist, userName } from './demo/db'
import type { WarningRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type {
  CodeLabel,
  GenerateCourseWarningsRequest,
  LearningWarningVO,
  PageResponse,
  WarningGenerationResultVO,
  WarningHandleRequest,
  WarningListQuery,
  WarningType,
} from './types'

// 预警域的枚举标签与通用 labels 表存在语义冲突（如 OPEN 在考试域是「开放中」），
// 因此按后端 WarningType/WarningLevel/WarningStatus 枚举单独映射。
const warningLabels: Record<string, string> = {
  PROGRESS_LAG: '学习进度落后',
  MISSING_ASSIGNMENT: '作业缺交',
  LOW_SCORE: '成绩偏低',
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  OPEN: '待处理',
  HANDLED: '已处理',
  IGNORED: '已忽略',
}
const wcl = (code: string): CodeLabel => ({ code, label: warningLabels[code] ?? code })

function toVO(row: WarningRow): LearningWarningVO {
  return {
    ...row,
    studentName: userName(row.studentId),
    warningType: wcl(row.warningType),
    warningLevel: wcl(row.warningLevel),
    warningStatus: wcl(row.warningStatus),
    suggestion: row.suggestion ?? null,
    aiExplanationDraftId: row.aiExplanationDraftId ?? null,
    handledBy: row.handledBy ?? null,
    handleRemark: row.handleRemark ?? null,
    handledAt: row.handledAt ?? null,
    evidences: row.evidences.map((evidence) => ({ ...evidence, sourceId: evidence.sourceId ?? null, metricCode: evidence.metricCode ?? null, metricValue: evidence.metricValue == null ? null : String(evidence.metricValue) })),
  }
}

function filterWarnings(rows: WarningRow[], query: WarningListQuery): WarningRow[] {
  return rows
    .filter((row) => !query.courseId || row.courseId === query.courseId)
    .filter((row) => !query.studentId || row.studentId === query.studentId)
    .filter((row) => !query.warningType || row.warningType === query.warningType)
    .filter((row) => !query.warningLevel || row.warningLevel === query.warningLevel)
    .filter((row) => !query.warningStatus || row.warningStatus === query.warningStatus)
    .sort((a, b) => b.generatedAt.localeCompare(a.generatedAt))
}

/** 规则生成：缺交作业（演示模式实现 MISSING_ASSIGNMENT 规则，其余类型按已有数据跳过）。 */
function generateMissingAssignmentWarnings(courseId: string, studentFilter?: string | null): WarningRow[] {
  const created: WarningRow[] = []
  const publishedAssignments = db.assignments.filter((item) => item.courseId === courseId && item.assignmentStatus !== 'DRAFT')
  const students = db.enrollments
    .filter((item) => item.courseId === courseId && item.status === 'ENROLLED')
    .map((item) => item.studentId)
    .filter((studentId) => !studentFilter || studentId === studentFilter)
  for (const studentId of students) {
    const missing = publishedAssignments.filter((assignment) => !db.submissions.some((submission) => submission.assignmentId === assignment.assignmentId && submission.studentId === studentId && submission.submissionStatus !== 'DRAFT'))
    if (!missing.length) continue
    const exists = db.warnings.some((warning) => warning.courseId === courseId && warning.studentId === studentId && warning.warningType === 'MISSING_ASSIGNMENT' && warning.warningStatus === 'OPEN')
    if (exists) continue
    created.push({
      warningId: nextId(),
      courseId,
      studentId,
      warningType: 'MISSING_ASSIGNMENT',
      warningLevel: missing.length >= 2 ? 'HIGH' : 'MEDIUM',
      warningStatus: 'OPEN',
      summary: `缺交 ${missing.length} 次作业。`,
      suggestion: '建议联系学生确认情况，必要时安排补交。',
      generatedAt: nowIso(),
      evidences: missing.map((assignment) => ({ evidenceId: nextId(), evidenceType: 'ASSIGNMENT', sourceId: assignment.assignmentId, description: `「${assignment.title}」未提交。` })),
      version: 0,
    })
  }
  return created
}

export const warningsApi = {
  async generate(courseId: string, body: GenerateCourseWarningsRequest = {}): Promise<WarningGenerationResultVO> {
    if (isRealMode()) return post<WarningGenerationResultVO>(`/api/v1/teacher/courses/${courseId}/warnings/generation`, body)
    requireTeacherCourse(courseId)
    const types: WarningType[] = body.warningTypes?.length ? body.warningTypes : ['PROGRESS_LAG', 'MISSING_ASSIGNMENT', 'LOW_SCORE']
    const created = types.includes('MISSING_ASSIGNMENT') ? generateMissingAssignmentWarnings(courseId, body.studentId) : []
    const enrolledCount = db.enrollments.filter((item) => item.courseId === courseId && item.status === 'ENROLLED').length
    if (!body.dryRun && created.length) {
      db.warnings.push(...created)
      persist()
    }
    return demoDelay({
      createdCount: body.dryRun ? 0 : created.length,
      skippedCount: Math.max(0, enrolledCount - created.length),
      warnings: created.map(toVO),
    })
  },

  async studentList(query: WarningListQuery = {}): Promise<PageResponse<LearningWarningVO>> {
    if (isRealMode()) return get<PageResponse<LearningWarningVO>>('/api/v1/student/warnings', { ...query })
    const student = currentUser('STUDENT')
    const rows = filterWarnings(db.warnings.filter((row) => row.studentId === student.userId), query)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  async studentDetail(warningId: string): Promise<LearningWarningVO> {
    if (isRealMode()) return get<LearningWarningVO>(`/api/v1/student/warnings/${warningId}`)
    const student = currentUser('STUDENT')
    const row = db.warnings.find((item) => item.warningId === warningId && item.studentId === student.userId) ?? notFound('预警不存在。')
    return demoDelay(toVO(row))
  },

  async teacherList(courseId: string, query: WarningListQuery = {}): Promise<PageResponse<LearningWarningVO>> {
    if (isRealMode()) return get<PageResponse<LearningWarningVO>>(`/api/v1/teacher/courses/${courseId}/warnings`, { ...query })
    requireTeacherCourse(courseId)
    const rows = filterWarnings(db.warnings.filter((row) => row.courseId === courseId), query)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  async teacherDetail(warningId: string): Promise<LearningWarningVO> {
    if (isRealMode()) return get<LearningWarningVO>(`/api/v1/teacher/warnings/${warningId}`)
    const row = db.warnings.find((item) => item.warningId === warningId) ?? notFound('预警不存在。')
    requireTeacherCourse(row.courseId)
    return demoDelay(toVO(row))
  },

  async handle(warningId: string, body: WarningHandleRequest): Promise<LearningWarningVO> {
    if (isRealMode()) return post<LearningWarningVO>(`/api/v1/teacher/warnings/${warningId}/handle`, body)
    const row = db.warnings.find((item) => item.warningId === warningId) ?? notFound('预警不存在。')
    requireTeacherCourse(row.courseId)
    assertVersion(row, body.version)
    const teacher = currentUser('TEACHER')
    Object.assign(row, {
      warningStatus: body.action,
      handledBy: teacher.userId,
      handleRemark: body.remark ?? null,
      handledAt: nowIso(),
      version: row.version + 1,
    })
    persist()
    return demoDelay(toVO(row))
  },
}
