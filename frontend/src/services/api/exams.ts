// 4.3 题库、考试、试卷与答题接口
import { demoDelay } from '../runtime'
import { del, get, isRealMode, post, put } from './client'
import { assertVersion, badRequest, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist } from './demo/db'
import type { AttemptRow, ExamRow, PaperRow, QuestionBankRow, QuestionRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type {
  CreateExamPaperRequest,
  CreateExamRequest,
  CreateQuestionBankRequest,
  CreateQuestionRequest,
  ExamAttemptListQuery,
  ExamAttemptVO,
  ExamListQuery,
  ExamPaperVO,
  ExamVO,
  GradeExamAttemptRequest,
  PageQuery,
  PageResponse,
  QuestionBankListQuery,
  QuestionBankVO,
  QuestionListQuery,
  QuestionVO,
  StudentExamListItemVO,
  SubmitExamAttemptRequest,
  UpdateExamPaperRequest,
  UpdateExamRequest,
  UpdateQuestionBankRequest,
  UpdateQuestionRequest,
} from './types'

function requireBank(bankId: string): QuestionBankRow {
  const row = db.questionBanks.find((item) => item.bankId === bankId) ?? notFound('题库不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requireQuestion(questionId: string): QuestionRow {
  const row = db.questions.find((item) => item.questionId === questionId) ?? notFound('题目不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requireExam(examId: string): ExamRow {
  const row = db.exams.find((item) => item.examId === examId) ?? notFound('考试不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requirePaper(paperId: string): PaperRow {
  const row = db.papers.find((item) => item.paperId === paperId) ?? notFound('试卷不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function questionInPublishedPaper(questionId: string): boolean {
  return db.papers.some((paper) => paper.status === 'PUBLISHED' && paper.questions.some((item) => item.questionId === questionId))
}

/** 契约规则：选择题至少两个选项，单选/判断恰一个正确项，多选至少两个正确项，简答不携带选项。 */
function validateQuestion(body: CreateQuestionRequest | UpdateQuestionRequest): void {
  const options = body.options ?? []
  const correctCount = options.filter((option) => option.correct).length
  if (body.questionType === 'SHORT_ANSWER' || body.questionType === 'FILL_BLANK') {
    if (options.length) badRequest('简答题不得携带选项。')
    return
  }
  if (options.length < 2) badRequest('选择题至少需要两个选项。')
  if ((body.questionType === 'SINGLE_CHOICE' || body.questionType === 'TRUE_FALSE') && correctCount !== 1) {
    badRequest('单选题和判断题必须恰好有一个正确选项。')
  }
  if (body.questionType === 'MULTIPLE_CHOICE' && correctCount < 2) badRequest('多选题至少需要两个正确选项。')
}

function correctAnswerOf(question: QuestionRow): string {
  return question.options.filter((option) => option.correct).map((option) => option.label).sort().join(',')
}

function toAttemptVO(row: AttemptRow): ExamAttemptVO {
  const paper = db.papers.find((item) => item.paperId === row.paperId)
  const questions = (paper?.questions ?? [])
    .slice()
    .sort((a, b) => a.questionOrder - b.questionOrder)
    .map((paperQuestion) => {
      const question = db.questions.find((item) => item.questionId === paperQuestion.questionId)
      return {
        questionId: paperQuestion.questionId,
        questionOrder: paperQuestion.questionOrder,
        score: paperQuestion.score,
        questionType: question?.questionType ?? 'SHORT_ANSWER',
        stem: question?.stem ?? '',
        options: (question?.options ?? []).map((option) => ({ label: option.label, content: option.content, sortOrder: option.sortOrder })),
      }
    })
  return {
    attemptId: row.attemptId,
    examId: row.examId,
    paperId: row.paperId,
    studentId: row.studentId,
    status: row.status,
    startedAt: row.startedAt,
    deadlineAt: row.deadlineAt ?? null,
    submittedAt: row.submittedAt ?? null,
    score: row.score ?? null,
    questions,
    answers: row.answers.map((answer) => ({ questionId: answer.questionId, answerContent: answer.answerContent, score: answer.score ?? null })),
    version: row.version,
  }
}

export const examsApi = {
  // —— 题库 ——
  async listBanks(courseId: string, query: QuestionBankListQuery = {}): Promise<PageResponse<QuestionBankVO>> {
    if (isRealMode()) return get<PageResponse<QuestionBankVO>>(`/api/v1/teacher/courses/${courseId}/question-banks`, { ...query })
    requireTeacherCourse(courseId)
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.questionBanks
      .filter((row) => row.courseId === courseId)
      .filter((row) => !query.status || row.status === query.status)
      .filter((row) => !keyword || row.name.toLowerCase().includes(keyword))
    return demoDelay(paginate(rows.map((row) => ({ ...row, description: row.description ?? null })), query))
  },

  async createBank(courseId: string, body: CreateQuestionBankRequest): Promise<QuestionBankVO> {
    if (isRealMode()) return post<QuestionBankVO>(`/api/v1/teacher/courses/${courseId}/question-banks`, body)
    requireTeacherCourse(courseId)
    const row: QuestionBankRow = { bankId: nextId(), courseId, name: body.name, description: body.description ?? null, status: 'ACTIVE', source: 'HUMAN', version: 0 }
    db.questionBanks.push(row)
    persist()
    return demoDelay({ ...row })
  },

  async confirmBank(bankId: string): Promise<QuestionBankVO> {
    if (isRealMode()) return post<QuestionBankVO>(`/api/v1/teacher/question-banks/${bankId}/confirm`, {})
    const row = requireBank(bankId)
    row.status = 'ACTIVE'
    persist()
    return demoDelay({ ...row, description: row.description ?? null })
  },

  async getBank(bankId: string): Promise<QuestionBankVO> {
    if (isRealMode()) return get<QuestionBankVO>(`/api/v1/teacher/question-banks/${bankId}`)
    return demoDelay({ ...requireBank(bankId) })
  },

  async updateBank(bankId: string, body: UpdateQuestionBankRequest): Promise<QuestionBankVO> {
    if (isRealMode()) return put<QuestionBankVO>(`/api/v1/teacher/question-banks/${bankId}`, body)
    const row = requireBank(bankId)
    assertVersion(row, body.version)
    Object.assign(row, { name: body.name, description: body.description ?? null, status: body.status, version: row.version + 1 })
    persist()
    return demoDelay({ ...row })
  },

  async deleteBank(bankId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/question-banks/${bankId}`)
    const row = requireBank(bankId)
    if (db.questions.some((item) => item.bankId === bankId)) conflict('题库下仍有题目，先删除题目。', 'OPERATION_NOT_ALLOWED')
    db.questionBanks.splice(db.questionBanks.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  // —— 题目 ——
  async listQuestions(bankId: string, query: QuestionListQuery = {}): Promise<PageResponse<QuestionVO>> {
    if (isRealMode()) return get<PageResponse<QuestionVO>>(`/api/v1/teacher/question-banks/${bankId}/questions`, { ...query })
    requireBank(bankId)
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.questions
      .filter((row) => row.bankId === bankId)
      .filter((row) => !query.questionType || row.questionType === query.questionType)
      .filter((row) => !query.difficulty || row.difficulty === query.difficulty)
      .filter((row) => !query.status || row.status === query.status)
      .filter((row) => !keyword || row.stem.toLowerCase().includes(keyword))
    return demoDelay(paginate(rows.map((row) => ({ ...row, analysis: row.analysis ?? null })), query))
  },

  async createQuestion(bankId: string, body: CreateQuestionRequest): Promise<QuestionVO> {
    if (isRealMode()) return post<QuestionVO>(`/api/v1/teacher/question-banks/${bankId}/questions`, body)
    const bank = requireBank(bankId)
    validateQuestion(body)
    const row: QuestionRow = {
      questionId: nextId(),
      bankId,
      courseId: bank.courseId,
      questionType: body.questionType,
      stem: body.stem,
      analysis: body.analysis ?? null,
      difficulty: body.difficulty,
      score: body.score,
      status: 'ACTIVE',
      options: body.options ?? [],
      version: 0,
    }
    db.questions.push(row)
    persist()
    return demoDelay({ ...row })
  },

  async getQuestion(questionId: string): Promise<QuestionVO> {
    if (isRealMode()) return get<QuestionVO>(`/api/v1/teacher/questions/${questionId}`)
    return demoDelay({ ...requireQuestion(questionId) })
  },

  async updateQuestion(questionId: string, body: UpdateQuestionRequest): Promise<QuestionVO> {
    if (isRealMode()) return put<QuestionVO>(`/api/v1/teacher/questions/${questionId}`, body)
    const row = requireQuestion(questionId)
    assertVersion(row, body.version)
    if (questionInPublishedPaper(questionId)) conflict('题目已被已发布试卷引用，不可修改。', 'OPERATION_NOT_ALLOWED')
    validateQuestion(body)
    Object.assign(row, {
      questionType: body.questionType,
      stem: body.stem,
      analysis: body.analysis ?? null,
      difficulty: body.difficulty,
      score: body.score,
      options: body.options ?? [],
      status: body.status ?? row.status,
      version: row.version + 1,
    })
    persist()
    return demoDelay({ ...row })
  },

  async deleteQuestion(questionId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/questions/${questionId}`)
    const row = requireQuestion(questionId)
    if (db.papers.some((paper) => paper.questions.some((item) => item.questionId === questionId))) {
      conflict('题目已被试卷引用，不可删除。', 'OPERATION_NOT_ALLOWED')
    }
    db.questions.splice(db.questions.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  // —— 考试 ——
  async listExams(courseId: string, query: ExamListQuery = {}): Promise<PageResponse<ExamVO>> {
    if (isRealMode()) return get<PageResponse<ExamVO>>(`/api/v1/teacher/courses/${courseId}/exams`, { ...query })
    requireTeacherCourse(courseId)
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.exams
      .filter((row) => row.courseId === courseId)
      .filter((row) => !query.status || row.status === query.status)
      .filter((row) => !keyword || row.title.toLowerCase().includes(keyword))
      .sort((a, b) => b.startAt.localeCompare(a.startAt))
    return demoDelay(paginate(rows.map((row) => ({ ...row, description: row.description ?? null })), query))
  },

  async createExam(courseId: string, body: CreateExamRequest): Promise<ExamVO> {
    if (isRealMode()) return post<ExamVO>(`/api/v1/teacher/courses/${courseId}/exams`, body)
    requireTeacherCourse(courseId)
    const row: ExamRow = { examId: nextId(), courseId, title: body.title, description: body.description ?? null, status: 'DRAFT', startAt: body.startAt, endAt: body.endAt, durationMinutes: body.durationMinutes, totalScore: body.totalScore, version: 0 }
    db.exams.push(row)
    persist()
    return demoDelay({ ...row })
  },

  async getExam(examId: string): Promise<ExamVO> {
    if (isRealMode()) return get<ExamVO>(`/api/v1/teacher/exams/${examId}`)
    return demoDelay({ ...requireExam(examId) })
  },

  async updateExam(examId: string, body: UpdateExamRequest): Promise<ExamVO> {
    if (isRealMode()) return put<ExamVO>(`/api/v1/teacher/exams/${examId}`, body)
    const row = requireExam(examId)
    assertVersion(row, body.version)
    Object.assign(row, { title: body.title, description: body.description ?? null, startAt: body.startAt, endAt: body.endAt, durationMinutes: body.durationMinutes, totalScore: body.totalScore, version: row.version + 1 })
    persist()
    return demoDelay({ ...row })
  },

  async deleteExam(examId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/exams/${examId}`)
    const row = requireExam(examId)
    if (row.status === 'PUBLISHED') conflict('已发布考试不可删除。', 'OPERATION_NOT_ALLOWED')
    db.papers = db.papers.filter((paper) => paper.examId !== examId)
    db.exams.splice(db.exams.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  // —— 试卷 ——
  async createPaper(examId: string, body: CreateExamPaperRequest): Promise<ExamPaperVO> {
    if (isRealMode()) return post<ExamPaperVO>(`/api/v1/teacher/exams/${examId}/papers`, body)
    const exam = requireExam(examId)
    validatePaperQuestions(exam.courseId, body)
    const row: PaperRow = {
      paperId: nextId(),
      examId,
      courseId: exam.courseId,
      title: body.title,
      totalScore: body.questions.reduce((sum, item) => sum + item.score, 0),
      status: 'DRAFT',
      questions: body.questions,
      version: 0,
    }
    db.papers.push(row)
    persist()
    return demoDelay({ ...row })
  },

  async getPaper(paperId: string): Promise<ExamPaperVO> {
    if (isRealMode()) return get<ExamPaperVO>(`/api/v1/teacher/exam-papers/${paperId}`)
    return demoDelay({ ...requirePaper(paperId) })
  },

  async updatePaper(paperId: string, body: UpdateExamPaperRequest): Promise<ExamPaperVO> {
    if (isRealMode()) return put<ExamPaperVO>(`/api/v1/teacher/exam-papers/${paperId}`, body)
    const row = requirePaper(paperId)
    assertVersion(row, body.version)
    if (row.status === 'PUBLISHED') conflict('已发布试卷不可修改。', 'OPERATION_NOT_ALLOWED')
    validatePaperQuestions(row.courseId, body)
    Object.assign(row, { title: body.title, questions: body.questions, totalScore: body.questions.reduce((sum, item) => sum + item.score, 0), version: row.version + 1 })
    persist()
    return demoDelay({ ...row })
  },

  async deletePaper(paperId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/exam-papers/${paperId}`)
    const row = requirePaper(paperId)
    if (row.status === 'PUBLISHED') conflict('已发布试卷不可删除。', 'OPERATION_NOT_ALLOWED')
    db.papers.splice(db.papers.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  async publishPaper(paperId: string): Promise<ExamPaperVO> {
    if (isRealMode()) return post<ExamPaperVO>(`/api/v1/teacher/exam-papers/${paperId}/publish`)
    const row = requirePaper(paperId)
    const exam = requireExam(row.examId)
    if (row.totalScore !== exam.totalScore) conflict('试卷总分必须等于考试总分。', 'OPERATION_NOT_ALLOWED')
    if (db.papers.some((paper) => paper.examId === row.examId && paper.status === 'PUBLISHED' && paper.paperId !== paperId)) {
      conflict('一个考试只能发布一份试卷。', 'OPERATION_NOT_ALLOWED')
    }
    row.status = 'PUBLISHED'
    row.version += 1
    exam.status = 'PUBLISHED'
    exam.version += 1
    persist()
    return demoDelay({ ...row })
  },

  // —— 学生答题 ——
  async studentExams(courseId: string, query: PageQuery & { keyword?: string } = {}): Promise<PageResponse<StudentExamListItemVO>> {
    if (isRealMode()) return get<PageResponse<StudentExamListItemVO>>(`/api/v1/student/courses/${courseId}/exams`, { ...query })
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.exams
      .filter((row) => row.courseId === courseId && row.status === 'PUBLISHED')
      .filter((row) => !keyword || row.title.toLowerCase().includes(keyword))
      .sort((a, b) => a.startAt.localeCompare(b.startAt))
      .map((row) => ({ examId: row.examId, courseId: row.courseId, title: row.title, description: row.description ?? null, startAt: row.startAt, endAt: row.endAt, durationMinutes: row.durationMinutes, totalScore: row.totalScore }))
    return demoDelay(paginate(rows, query))
  },

  /** 开始答题：时间窗内幂等，重复调用返回未提交的进行中记录。 */
  async startAttempt(examId: string): Promise<ExamAttemptVO> {
    if (isRealMode()) return post<ExamAttemptVO>(`/api/v1/student/exams/${examId}/attempts`)
    const exam = db.exams.find((item) => item.examId === examId && item.status === 'PUBLISHED') ?? notFound('考试不存在或未发布。')
    const student = currentUser('STUDENT')
    const existing = db.attempts.find((item) => item.examId === examId && item.studentId === student.userId)
    if (existing) {
      if (existing.status === 'IN_PROGRESS') return demoDelay(toAttemptVO(existing))
      conflict('已提交过该考试，不能重新开始。', 'OPERATION_NOT_ALLOWED')
    }
    const now = nowIso()
    if (now < exam.startAt || now > exam.endAt) conflict('不在考试时间窗内。', 'OPERATION_NOT_ALLOWED')
    const paper = db.papers.find((item) => item.examId === examId && item.status === 'PUBLISHED') ?? conflict('考试尚未发布试卷。', 'OPERATION_NOT_ALLOWED')
    const deadline = new Date(Math.min(Date.now() + exam.durationMinutes * 60 * 1000, new Date(exam.endAt).getTime())).toISOString()
    const row: AttemptRow = { attemptId: nextId(), examId, paperId: paper.paperId, studentId: student.userId, status: 'IN_PROGRESS', startedAt: now, deadlineAt: deadline, answers: [], version: 0 }
    db.attempts.push(row)
    persist()
    return demoDelay(toAttemptVO(row))
  },

  async getAttempt(attemptId: string): Promise<ExamAttemptVO> {
    if (isRealMode()) return get<ExamAttemptVO>(`/api/v1/student/exam-attempts/${attemptId}`)
    const student = currentUser('STUDENT')
    const row = db.attempts.find((item) => item.attemptId === attemptId && item.studentId === student.userId) ?? notFound('答题记录不存在。')
    return demoDelay(toAttemptVO(row))
  },

  /** 提交答卷：客观题自动判分；包含简答题时进入 SUBMITTED 待教师评分。 */
  async submitAttempt(attemptId: string, body: SubmitExamAttemptRequest): Promise<ExamAttemptVO> {
    if (isRealMode()) return post<ExamAttemptVO>(`/api/v1/student/exam-attempts/${attemptId}/submit`, body)
    const student = currentUser('STUDENT')
    const row = db.attempts.find((item) => item.attemptId === attemptId && item.studentId === student.userId) ?? notFound('答题记录不存在。')
    if (row.status !== 'IN_PROGRESS') conflict('答卷已提交，不能重复提交。')
    assertVersion(row, body.version)
    const seen = new Set<string>()
    for (const answer of body.answers) {
      if (seen.has(answer.questionId)) badRequest('同一题不能重复作答。')
      seen.add(answer.questionId)
    }
    const paper = db.papers.find((item) => item.paperId === row.paperId) ?? notFound('试卷不存在。')
    let hasShortAnswer = false
    let objectiveScore = 0
    row.answers = body.answers.map((answer) => {
      const paperQuestion = paper.questions.find((item) => item.questionId === answer.questionId)
      const question = db.questions.find((item) => item.questionId === answer.questionId)
      if (!paperQuestion || !question) badRequest('答案包含不属于该试卷的题目。')
      if (question.questionType === 'SHORT_ANSWER' || question.questionType === 'FILL_BLANK') {
        hasShortAnswer = true
        return { questionId: answer.questionId, answerContent: answer.answerContent }
      }
      const normalized = answer.answerContent.split(',').map((part) => part.trim()).sort().join(',')
      const score = normalized === correctAnswerOf(question) ? paperQuestion.score : 0
      objectiveScore += score
      return { questionId: answer.questionId, answerContent: answer.answerContent, score }
    })
    Object.assign(row, {
      status: hasShortAnswer ? 'SUBMITTED' : 'GRADED',
      submittedAt: nowIso(),
      score: hasShortAnswer ? null : objectiveScore,
      version: row.version + 1,
    })
    persist()
    return demoDelay(toAttemptVO(row))
  },

  // —— 教师阅卷 ——
  async listAttempts(examId: string, query: ExamAttemptListQuery = {}): Promise<PageResponse<ExamAttemptVO>> {
    if (isRealMode()) return get<PageResponse<ExamAttemptVO>>(`/api/v1/teacher/exams/${examId}/attempts`, { ...query })
    requireExam(examId)
    const rows = db.attempts
      .filter((row) => row.examId === examId)
      .filter((row) => !query.status || row.status === query.status)
      .sort((a, b) => (b.submittedAt ?? b.startedAt).localeCompare(a.submittedAt ?? a.startedAt))
    return demoDelay(paginate(rows.map(toAttemptVO), query))
  },

  /** 简答题人工评分：合计客观题与人工分，答卷进入 GRADED。 */
  async gradeAttempt(attemptId: string, body: GradeExamAttemptRequest): Promise<ExamAttemptVO> {
    if (isRealMode()) return post<ExamAttemptVO>(`/api/v1/teacher/exam-attempts/${attemptId}/grade`, body)
    const row = db.attempts.find((item) => item.attemptId === attemptId) ?? notFound('答题记录不存在。')
    requireExam(row.examId)
    if (row.status !== 'SUBMITTED') conflict('仅待评分的答卷可以人工评分。', 'OPERATION_NOT_ALLOWED')
    assertVersion(row, body.version)
    for (const graded of body.answers) {
      const answer = row.answers.find((item) => item.questionId === graded.questionId)
      if (!answer) badRequest('评分包含不属于该答卷的题目。')
      answer.score = graded.score
      answer.teacherComment = graded.teacherComment ?? null
    }
    Object.assign(row, {
      status: 'GRADED',
      score: row.answers.reduce((sum, answer) => sum + (answer.score ?? 0), 0),
      version: row.version + 1,
    })
    persist()
    return demoDelay(toAttemptVO(row))
  },
}

/** 契约规则：题目必须属于同一课程；题目与题号不能重复。 */
function validatePaperQuestions(courseId: string, body: CreateExamPaperRequest | UpdateExamPaperRequest): void {
  const ids = new Set<string>()
  const orders = new Set<number>()
  for (const item of body.questions) {
    if (ids.has(item.questionId)) badRequest('试卷中的题目不能重复。')
    if (orders.has(item.questionOrder)) badRequest('试卷中的题号不能重复。')
    ids.add(item.questionId)
    orders.add(item.questionOrder)
    const question = db.questions.find((question) => question.questionId === item.questionId)
    if (!question) notFound(`题目 ${item.questionId} 不存在。`)
    if (question.courseId !== courseId) conflict('题目只能用于同一课程的试卷。', 'OPERATION_NOT_ALLOWED')
  }
}
