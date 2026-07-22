// 4.1 作业、提交与成绩接口
import { demoDelay } from '../runtime'
import { get, isRealMode, post, put } from './client'
import { assertVersion, badRequest, cl, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist, userName } from './demo/db'
import type { AssignmentRow, GradeRow, SubmissionRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type {
  AssignmentCreateRequest,
  AssignmentDetailVO,
  AssignmentStatisticsVO,
  AssignmentUpdateRequest,
  CourseGradeStatisticsVO,
  GradeListQuery,
  GradeSubmissionRequest,
  PageQuery,
  PageResponse,
  PublishGradeRequest,
  StudentAssignmentDetailVO,
  StudentAssignmentListItemVO,
  StudentGradeVO,
  SubmissionDetailVO,
  SubmissionSaveRequest,
  SubmissionSubmitRequest,
  TeacherSubmissionGradeVO,
  TeacherSubmissionRosterVO,
} from './types'

function availabilityOf(row: AssignmentRow): string {
  if (row.assignmentStatus === 'DRAFT') return 'UPCOMING'
  if (row.assignmentStatus === 'CLOSED') return 'CLOSED'
  const now = nowIso()
  if (row.openAt && row.openAt > now) return 'UPCOMING'
  if (row.dueAt < now) return 'CLOSED'
  return 'OPEN'
}

function toAssignmentVO(row: AssignmentRow): AssignmentDetailVO {
  return {
    assignmentId: row.assignmentId,
    courseId: row.courseId,
    lessonId: row.lessonId ?? null,
    title: row.title,
    description: row.description ?? null,
    responseMode: (row.responseMode ?? 'MIXED') as AssignmentDetailVO['responseMode'],
    questions: row.questions ?? [],
    maxScore: row.maxScore,
    assignmentStatus: cl(row.assignmentStatus),
    availabilityStatus: cl(availabilityOf(row)),
    openAt: row.openAt ?? null,
    dueAt: row.dueAt,
    publishedAt: row.publishedAt ?? null,
    attachments: row.attachments,
    source: row.source,
    version: row.version,
  }
}

function toSubmissionVO(row: SubmissionRow): SubmissionDetailVO {
  return {
    submissionId: row.submissionId,
    assignmentId: row.assignmentId,
    courseId: row.courseId,
    studentId: row.studentId,
    attemptNo: row.attemptNo,
    content: row.content ?? null,
    answers: row.answers ?? {},
    fileId: row.fileId ?? null,
    fileKey: row.fileKey ?? null,
    fileUrl: row.fileUrl ?? null,
    submissionStatus: cl(row.submissionStatus),
    submittedAt: row.submittedAt ?? null,
    score: row.score ?? null,
    teacherComment: row.teacherComment ?? null,
    aiCommentDraftId: row.aiCommentDraftId ?? null,
    gradedBy: row.gradedBy ?? null,
    gradedAt: row.gradedAt ?? null,
    publishedAt: row.publishedAt ?? null,
    version: row.version,
  }
}

function toTeacherGradeVO(row: SubmissionRow, assignment: AssignmentRow): TeacherSubmissionGradeVO {
  const grade = db.grades.find((item) => item.gradeId === row.gradeId)
  return {
    submissionId: row.submissionId,
    assignmentId: row.assignmentId,
    courseId: row.courseId,
    studentId: row.studentId,
    studentName: userName(row.studentId),
    submissionStatus: cl(row.submissionStatus),
    submittedAt: row.submittedAt ?? null,
    content: row.content ?? null,
    answers: row.answers ?? {},
    fileId: row.fileId ?? null,
    fileKey: row.fileKey ?? null,
    fileUrl: row.fileUrl ?? null,
    score: row.score ?? null,
    maxScore: assignment.maxScore,
    teacherComment: row.teacherComment ?? null,
    aiCommentDraftId: row.aiCommentDraftId ?? null,
    gradedBy: row.gradedBy ?? null,
    gradedAt: row.gradedAt ?? null,
    gradeId: grade?.gradeId ?? null,
    gradeStatus: grade ? cl(grade.status) : null,
    publishedAt: grade?.publishedAt ?? null,
    version: row.version,
    gradeVersion: grade?.version ?? null,
  }
}

function requireTeacherAssignment(assignmentId: string): AssignmentRow {
  const row = db.assignments.find((item) => item.assignmentId === assignmentId) ?? notFound('作业不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requireStudentAssignment(assignmentId: string): AssignmentRow {
  const row = db.assignments.find((item) => item.assignmentId === assignmentId) ?? notFound('作业不存在。')
  if (row.assignmentStatus === 'DRAFT') notFound('作业不存在。')
  const student = currentUser('STUDENT')
  const enrolled = db.enrollments.some((item) => item.courseId === row.courseId && item.studentId === student.userId && item.status === 'ENROLLED')
  if (!enrolled) conflict('尚未选修该课程。', 'FORBIDDEN')
  return row
}

function submissionOf(assignmentId: string, studentId: string): SubmissionRow | undefined {
  return db.submissions.find((item) => item.assignmentId === assignmentId && item.studentId === studentId)
}

function applyAssignmentFields(row: AssignmentRow, body: AssignmentCreateRequest | AssignmentUpdateRequest): void {
  Object.assign(row, {
    lessonId: body.lessonId ?? null,
    title: body.title,
    description: body.description ?? null,
    responseMode: body.responseMode ?? 'MIXED',
    questions: body.questions ?? [],
    maxScore: body.maxScore,
    openAt: body.openAt ?? null,
    dueAt: body.dueAt,
    attachments: (body.attachments ?? []).map((attachment, index) => ({
      attachmentId: row.attachments[index]?.attachmentId ?? nextId(),
      name: attachment.name,
      fileId: attachment.fileId == null ? null : String(attachment.fileId),
      fileKey: attachment.fileKey ?? null,
      fileUrl: attachment.fileUrl ?? null,
      fileSize: attachment.fileSize ?? null,
      mimeType: attachment.mimeType ?? null,
      sortOrder: attachment.sortOrder,
    })),
  })
}

export const assignmentsApi = {
  // —— 教师端 ——
  async teacherList(courseId: string, query: PageQuery & { status?: string; sort?: string } = {}): Promise<PageResponse<AssignmentDetailVO>> {
    if (isRealMode()) return get<PageResponse<AssignmentDetailVO>>(`/api/v1/teacher/courses/${courseId}/assignments`, { ...query })
    requireTeacherCourse(courseId)
    const rows = db.assignments
      .filter((row) => row.courseId === courseId)
      .filter((row) => !query.status || row.assignmentStatus === query.status)
      .sort((a, b) => b.dueAt.localeCompare(a.dueAt))
    return demoDelay(paginate(rows.map(toAssignmentVO), query))
  },

  async create(courseId: string, body: AssignmentCreateRequest): Promise<AssignmentDetailVO> {
    if (isRealMode()) return post<AssignmentDetailVO>(`/api/v1/teacher/courses/${courseId}/assignments`, body)
    requireTeacherCourse(courseId)
    const row: AssignmentRow = { assignmentId: nextId(), courseId, title: body.title, maxScore: body.maxScore, assignmentStatus: 'DRAFT', dueAt: body.dueAt, attachments: [], source: 'HUMAN', version: 0 }
    applyAssignmentFields(row, body)
    db.assignments.push(row)
    persist()
    return demoDelay(toAssignmentVO(row))
  },

  async update(assignmentId: string, body: AssignmentUpdateRequest): Promise<AssignmentDetailVO> {
    if (isRealMode()) return put<AssignmentDetailVO>(`/api/v1/teacher/assignments/${assignmentId}`, body)
    const row = requireTeacherAssignment(assignmentId)
    assertVersion(row, body.version)
    applyAssignmentFields(row, body)
    row.version += 1
    persist()
    return demoDelay(toAssignmentVO(row))
  },

  async publish(assignmentId: string): Promise<AssignmentDetailVO> {
    if (isRealMode()) return post<AssignmentDetailVO>(`/api/v1/teacher/assignments/${assignmentId}/publish`)
    const row = requireTeacherAssignment(assignmentId)
    if (row.assignmentStatus !== 'DRAFT') conflict('仅草稿状态的作业可以发布。', 'OPERATION_NOT_ALLOWED')
    Object.assign(row, { assignmentStatus: 'PUBLISHED', publishedAt: nowIso(), version: row.version + 1 })
    persist()
    return demoDelay(toAssignmentVO(row))
  },

  async close(assignmentId: string): Promise<AssignmentDetailVO> {
    if (isRealMode()) return post<AssignmentDetailVO>(`/api/v1/teacher/assignments/${assignmentId}/close`)
    const row = requireTeacherAssignment(assignmentId)
    if (row.assignmentStatus !== 'PUBLISHED') conflict('仅已发布的作业可以截止。', 'OPERATION_NOT_ALLOWED')
    Object.assign(row, { assignmentStatus: 'CLOSED', version: row.version + 1 })
    persist()
    return demoDelay(toAssignmentVO(row))
  },

  // —— 学生端 ——
  async studentList(courseId: string, query: PageQuery & { status?: string } = {}): Promise<PageResponse<StudentAssignmentListItemVO>> {
    if (isRealMode()) return get<PageResponse<StudentAssignmentListItemVO>>(`/api/v1/student/courses/${courseId}/assignments`, { ...query })
    const student = currentUser('STUDENT')
    const rows = db.assignments
      .filter((row) => row.courseId === courseId && row.assignmentStatus !== 'DRAFT')
      .sort((a, b) => a.dueAt.localeCompare(b.dueAt))
      .map((row) => {
        const submission = submissionOf(row.assignmentId, student.userId)
        const grade = db.grades.find((item) => item.assignmentId === row.assignmentId && item.studentId === student.userId && item.status === 'PUBLISHED')
        return {
          assignmentId: row.assignmentId,
          courseId: row.courseId,
          lessonId: row.lessonId ?? null,
          title: row.title,
          maxScore: row.maxScore,
          availabilityStatus: cl(availabilityOf(row)),
          dueAt: row.dueAt,
          submissionStatus: submission ? cl(submission.submissionStatus) : null,
          submittedAt: submission?.submittedAt ?? null,
          graded: Boolean(grade),
        }
      })
    return demoDelay(paginate(rows, query))
  },

  async studentDetail(assignmentId: string): Promise<StudentAssignmentDetailVO> {
    if (isRealMode()) return get<StudentAssignmentDetailVO>(`/api/v1/student/assignments/${assignmentId}`)
    const row = requireStudentAssignment(assignmentId)
    const student = currentUser('STUDENT')
    const submission = submissionOf(assignmentId, student.userId)
    return demoDelay({ assignment: toAssignmentVO(row), submission: submission ? toSubmissionVO(submission) : null })
  },

  async saveDraft(assignmentId: string, body: SubmissionSaveRequest): Promise<SubmissionDetailVO> {
    if (isRealMode()) return put<SubmissionDetailVO>(`/api/v1/student/assignments/${assignmentId}/submission-draft`, body)
    const row = requireStudentAssignment(assignmentId)
    const student = currentUser('STUDENT')
    let submission = submissionOf(assignmentId, student.userId)
    if (submission && submission.submissionStatus !== 'DRAFT' && submission.submissionStatus !== 'RETURNED') {
      conflict('作业已提交，不能再保存草稿。', 'OPERATION_NOT_ALLOWED')
    }
    if (!submission) {
      submission = { submissionId: nextId(), assignmentId, courseId: row.courseId, studentId: student.userId, attemptNo: 1, submissionStatus: 'DRAFT', version: 0 }
      db.submissions.push(submission)
    } else {
      if (body.version != null) assertVersion(submission, body.version)
      submission.version += 1
    }
    Object.assign(submission, {
      content: body.content ?? null,
      answers: body.answers ?? {},
      fileId: body.fileId == null ? null : String(body.fileId),
      fileKey: body.fileKey ?? null,
      fileUrl: body.fileUrl ?? null,
    })
    persist()
    return demoDelay(toSubmissionVO(submission))
  },

  async submit(assignmentId: string, body: SubmissionSubmitRequest): Promise<SubmissionDetailVO> {
    if (isRealMode()) return post<SubmissionDetailVO>(`/api/v1/student/assignments/${assignmentId}/submissions`, body)
    const row = requireStudentAssignment(assignmentId)
    if (availabilityOf(row) !== 'OPEN') conflict('作业不在开放提交时间内。', 'OPERATION_NOT_ALLOWED')
    if (!body.content?.trim() && !Object.keys(body.answers ?? {}).length && body.fileId == null && !body.fileKey && !body.fileUrl) {
      badRequest('正式提交必须有在线回答、题目答案或附件。')
    }
    const student = currentUser('STUDENT')
    let submission = submissionOf(assignmentId, student.userId)
    if (submission?.submissionStatus === 'SUBMITTED' || submission?.submissionStatus === 'GRADED') {
      conflict('作业已提交，请勿重复提交。')
    }
    if (!submission) {
      submission = { submissionId: nextId(), assignmentId, courseId: row.courseId, studentId: student.userId, attemptNo: 1, submissionStatus: 'DRAFT', version: 0 }
      db.submissions.push(submission)
    } else {
      if (body.version != null) assertVersion(submission, body.version)
      submission.attemptNo = submission.submissionStatus === 'RETURNED' ? submission.attemptNo + 1 : submission.attemptNo
      submission.version += 1
    }
    Object.assign(submission, {
      content: body.content ?? null,
      answers: body.answers ?? {},
      fileId: body.fileId == null ? null : String(body.fileId),
      fileKey: body.fileKey ?? null,
      fileUrl: body.fileUrl ?? null,
      submissionStatus: 'SUBMITTED',
      submittedAt: nowIso(),
    })
    persist()
    return demoDelay(toSubmissionVO(submission))
  },

  // —— 批改与成绩 ——
  async listSubmissions(assignmentId: string, query: PageQuery & { status?: string } = {}): Promise<PageResponse<TeacherSubmissionGradeVO>> {
    if (isRealMode()) return get<PageResponse<TeacherSubmissionGradeVO>>(`/api/v1/teacher/assignments/${assignmentId}/submissions`, { ...query })
    const assignment = requireTeacherAssignment(assignmentId)
    const rows = db.submissions
      .filter((row) => row.assignmentId === assignmentId && row.submissionStatus !== 'DRAFT')
      .filter((row) => !query.status || row.submissionStatus === query.status)
      .sort((a, b) => (b.submittedAt ?? '').localeCompare(a.submittedAt ?? ''))
    return demoDelay(paginate(rows.map((row) => toTeacherGradeVO(row, assignment)), query))
  },

  async submissionRoster(assignmentId: string): Promise<TeacherSubmissionRosterVO[]> {
    if (isRealMode()) return get<TeacherSubmissionRosterVO[]>('/api/v1/teacher/assignments/' + assignmentId + '/submission-roster')
    const assignment = requireTeacherAssignment(assignmentId)
    const submitted = new Map(
      db.submissions
        .filter((row) => row.assignmentId === assignmentId && row.submissionStatus !== 'DRAFT')
        .sort((a, b) => (b.attemptNo ?? 0) - (a.attemptNo ?? 0))
        .map((row) => [row.studentId, row] as const),
    )
    return demoDelay(db.enrollments
      .filter((item) => item.courseId === assignment.courseId && ['ENROLLED', 'COMPLETED'].includes(item.status))
      .map((enrollment) => {
        const row = submitted.get(enrollment.studentId)
        return {
          studentId: enrollment.studentId,
          studentName: userName(enrollment.studentId),
          submitted: Boolean(row),
          submission: row ? toTeacherGradeVO(row, assignment) : null,
        }
      }))
  },
  async grade(submissionId: string, body: GradeSubmissionRequest): Promise<TeacherSubmissionGradeVO> {
    if (isRealMode()) return post<TeacherSubmissionGradeVO>(`/api/v1/teacher/submissions/${submissionId}/grade`, body)
    const submission = db.submissions.find((item) => item.submissionId === submissionId) ?? notFound('提交不存在。')
    const assignment = requireTeacherAssignment(submission.assignmentId)
    if (submission.submissionStatus === 'DRAFT') conflict('学生尚未正式提交，不能评分。', 'OPERATION_NOT_ALLOWED')
    assertVersion(submission, body.version)
    if (body.score < 0 || body.score > body.maxScore) badRequest('分数必须在 0 与满分之间。')
    const teacher = currentUser('TEACHER')
    Object.assign(submission, {
      score: body.score,
      teacherComment: body.teacherComment ?? null,
      aiCommentDraftId: body.aiCommentDraftId ?? null,
      submissionStatus: 'GRADED',
      gradedBy: teacher.userId,
      gradedAt: nowIso(),
      version: submission.version + 1,
    })
    let grade = db.grades.find((item) => item.gradeId === submission.gradeId)
    if (!grade) {
      grade = { gradeId: nextId(), courseId: submission.courseId, assignmentId: submission.assignmentId, studentId: submission.studentId, submissionId, score: body.score, maxScore: body.maxScore, teacherComment: body.teacherComment ?? null, status: 'DRAFT', version: 0 } as GradeRow
      db.grades.push(grade)
      submission.gradeId = grade.gradeId
    } else {
      Object.assign(grade, { score: body.score, maxScore: body.maxScore, teacherComment: body.teacherComment ?? null, version: grade.version + 1 })
    }
    if (body.publishNow) {
      Object.assign(grade, { status: 'PUBLISHED', publishedAt: nowIso() })
      submission.publishedAt = grade.publishedAt
    }
    persist()
    return demoDelay(toTeacherGradeVO(submission, assignment))
  },

  async publishGrade(gradeId: string, body: PublishGradeRequest): Promise<TeacherSubmissionGradeVO> {
    if (isRealMode()) return post<TeacherSubmissionGradeVO>(`/api/v1/teacher/grades/${gradeId}/publication`, body)
    const grade = db.grades.find((item) => item.gradeId === gradeId) ?? notFound('成绩不存在。')
    assertVersion(grade, body.version)
    if (grade.status === 'PUBLISHED') conflict('成绩已发布。', 'OPERATION_NOT_ALLOWED')
    Object.assign(grade, { status: 'PUBLISHED', publishedAt: nowIso(), version: grade.version + 1 })
    const submission = db.submissions.find((item) => item.submissionId === grade.submissionId) ?? notFound('提交不存在。')
    submission.publishedAt = grade.publishedAt
    const assignment = requireTeacherAssignment(grade.assignmentId)
    persist()
    return demoDelay(toTeacherGradeVO(submission, assignment))
  },

  async studentGrades(query: GradeListQuery = {}): Promise<PageResponse<StudentGradeVO>> {
    if (isRealMode()) return get<PageResponse<StudentGradeVO>>('/api/v1/student/grades', { ...query })
    const student = currentUser('STUDENT')
    const rows = db.grades
      .filter((row) => row.studentId === student.userId && row.status === 'PUBLISHED')
      .filter((row) => !query.courseId || row.courseId === query.courseId)
      .sort((a, b) => (b.publishedAt ?? '').localeCompare(a.publishedAt ?? ''))
      .map((row) => ({
        gradeId: row.gradeId,
        courseId: row.courseId,
        assignmentId: row.assignmentId,
        assignmentTitle: db.assignments.find((item) => item.assignmentId === row.assignmentId)?.title ?? '作业',
        score: row.score,
        maxScore: row.maxScore,
        scoreRate: row.maxScore ? Math.round((row.score / row.maxScore) * 10000) / 100 : 0,
        teacherComment: row.teacherComment ?? null,
        publishedAt: row.publishedAt ?? '',
      }))
    return demoDelay(paginate(rows, query))
  },

  async statistics(assignmentId: string): Promise<AssignmentStatisticsVO> {
    if (isRealMode()) return get<AssignmentStatisticsVO>(`/api/v1/teacher/assignments/${assignmentId}/statistics`)
    const assignment = requireTeacherAssignment(assignmentId)
    const total = db.enrollments.filter((item) => item.courseId === assignment.courseId && item.status === 'ENROLLED').length
    const submissions = db.submissions.filter((item) => item.assignmentId === assignmentId && item.submissionStatus !== 'DRAFT')
    const graded = submissions.filter((item) => item.submissionStatus === 'GRADED')
    const published = db.grades.filter((item) => item.assignmentId === assignmentId && item.status === 'PUBLISHED')
    const scores = graded.map((item) => item.score ?? 0)
    return demoDelay({
      assignmentId,
      courseId: assignment.courseId,
      totalStudentCount: total,
      submittedCount: submissions.length,
      missingCount: Math.max(0, total - submissions.length),
      gradedCount: graded.length,
      publishedGradeCount: published.length,
      averageScore: scores.length ? Math.round((scores.reduce((sum, value) => sum + value, 0) / scores.length) * 100) / 100 : null,
      lowScoreCount: graded.filter((item) => (item.score ?? 0) / assignment.maxScore < 0.6).length,
    })
  },

  async courseGradeStatistics(courseId: string): Promise<CourseGradeStatisticsVO> {
    if (isRealMode()) return get<CourseGradeStatisticsVO>(`/api/v1/teacher/courses/${courseId}/grade-statistics`)
    requireTeacherCourse(courseId)
    const assignments = db.assignments.filter((item) => item.courseId === courseId)
    const published = db.grades.filter((item) => item.courseId === courseId && item.status === 'PUBLISHED')
    const rates = published.map((item) => (item.maxScore ? item.score / item.maxScore : 0))
    return demoDelay({
      courseId,
      assignmentCount: assignments.length,
      publishedAssignmentCount: assignments.filter((item) => item.assignmentStatus === 'PUBLISHED').length,
      enrolledStudentCount: db.enrollments.filter((item) => item.courseId === courseId && item.status === 'ENROLLED').length,
      gradedRecordCount: db.grades.filter((item) => item.courseId === courseId).length,
      publishedGradeCount: published.length,
      averageScoreRate: rates.length ? Math.round((rates.reduce((sum, value) => sum + value, 0) / rates.length) * 10000) / 100 : null,
      passRate: rates.length ? Math.round((rates.filter((rate) => rate >= 0.6).length / rates.length) * 10000) / 100 : null,
      lowScoreCount: rates.filter((rate) => rate < 0.6).length,
    })
  },
}
