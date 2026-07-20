// 前后端联调 harness：通过 Vite SSR 加载前端契约 API 层（real 模式），
// 直接驱动其公开出口打真实网关，记录 PASS/FAIL/WARN 与契约差异。
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const FRONTEND = fileURLToPath(new URL('../../frontend', import.meta.url))
const GATEWAY = 'http://host.docker.internal:18080'

process.env.VITE_API_MODE = 'real'
process.env.VITE_GATEWAY_URL = GATEWAY

// ---- 浏览器全局 polyfill（在加载任何前端模块之前） ----
const storage = new Map()
globalThis.localStorage = {
  getItem: (k) => (storage.has(k) ? storage.get(k) : null),
  setItem: (k, v) => storage.set(k, String(v)),
  removeItem: (k) => storage.delete(k),
  clear: () => storage.clear(),
}
globalThis.window = {
  localStorage: globalThis.localStorage,
  dispatchEvent: () => true,
  addEventListener: () => {},
  setTimeout: (...a) => setTimeout(...a),
  clearTimeout: (...a) => clearTimeout(...a),
}

const { createServer } = await import(`${FRONTEND}/node_modules/vite/dist/node/index.js`)
const server = await createServer({
  root: FRONTEND,
  appType: 'custom',
  logLevel: 'error',
  server: { middlewareMode: true, hmr: false },
})
const api = await server.ssrLoadModule('/src/services/api/index.ts')
const {
  authApi, filesApi, categoriesApi, teacherCoursesApi, courseContentApi,
  studentLearningApi, courseReviewsApi, adminUsersApi, assignmentsApi,
  forumApi, warningsApi, examsApi, announcementsApi, adminStatisticsApi, aiApi,
  TOKEN_STORAGE_KEY,
} = api

// ---- 记录与助手 ----
const results = []
function log(status, name, detail = '') {
  results.push({ status, name, detail })
  console.log(`[${status}] ${name}${detail ? ` — ${detail}` : ''}`)
}
async function step(name, fn) {
  try {
    const v = await fn()
    log('PASS', name)
    return v
  } catch (e) {
    log('FAIL', name, `${e.code ?? e.name}(${e.status ?? '-'}) ${e.message}`)
    return undefined
  }
}
async function probe(name, expectCodes, fn) {
  try {
    await fn()
    log('FAIL', name, `预期错误 ${expectCodes.join('/')} 但请求成功`)
  } catch (e) {
    if (expectCodes.includes(e.code)) log('PASS', name, `按预期拒绝：${e.code}`)
    else log('WARN', name, `被拒绝但错误码不同：got ${e.code}(${e.status ?? '-'}) ${e.message}；预期 ${expectCodes.join('/')}`)
  }
}
function check(name, cond, detail = '') {
  log(cond ? 'PASS' : 'FAIL', name, detail)
}

const ts = Date.now().toString(36)
const tokens = {}
async function loginAs(key, username, password) {
  const vo = await authApi.login({ username, password })
  tokens[key] = vo.accessToken
  return vo
}
const as = (key) => localStorage.setItem(TOKEN_STORAGE_KEY, tokens[key])
const iso = (offsetMs) => new Date(Date.now() + offsetMs).toISOString()

// ================= A 认证 =================
console.log('\n===== A 认证 =====')
await probe('A1 错误密码登录被拒', ['INVALID_CREDENTIALS'], () =>
  authApi.login({ username: 'student', password: 'wrong-pass' }))
const studentLogin = await step('A2 student 登录', () => loginAs('student', 'student', '123456'))
if (studentLogin) check('A2b 登录 VO 含 user/roles', !!studentLogin.user?.userId && studentLogin.roles?.includes('STUDENT'),
  `userId=${studentLogin.user?.userId} roles=${JSON.stringify(studentLogin.roles)}`)
const me = await step('A3 auth/me', () => authApi.me())
if (me) check('A3b me 返回当前学生', me.username === 'student', `username=${me.username} activeRole=${me.activeRole}`)
const regStudent = await step('A4 注册新学生', () =>
  authApi.register({ username: `ldstu_${ts}`, password: `Passw0rd${ts}`, displayName: '联调学生', role: 'STUDENT' }))
if (regStudent) check('A4b 学生注册即启用', regStudent.userStatus === 'ENABLED' && !regStudent.approvalRequired,
  `status=${regStudent.userStatus} approvalRequired=${regStudent.approvalRequired} login=${regStudent.login ? 'yes' : 'null'}`)
const regTeacher = await step('A5 注册新教师(待审核)', () =>
  authApi.register({ username: `ldtea_${ts}`, password: `Passw0rd${ts}`, displayName: '联调教师', role: 'TEACHER' }))
if (regTeacher) check('A5b 教师注册待审核', regTeacher.userStatus === 'PENDING' && regTeacher.approvalRequired === true,
  `status=${regTeacher.userStatus}`)

// ================= B 管理员 =================
console.log('\n===== B 管理员 =====')
await step('B1 admin 登录', () => loginAs('admin', 'admin', 'admin123'))
as('admin')
if (regTeacher) {
  const approved = await step('B2 审核通过新教师', () => adminUsersApi.approveTeacher(regTeacher.userId))
  if (approved) check('B2b 审核后状态 ENABLED', approved.userStatus === 'ENABLED', `status=${approved.userStatus}`)
}
const userPage = await step('B3 用户列表', () => adminUsersApi.list({ page: 1, size: 10 }))
if (userPage) check('B3b 列表分页结构', Array.isArray(userPage.records) && userPage.total >= 4,
  `total=${userPage.total} records=${userPage.records?.length}`)
const stats = await step('B4 平台统计', () => adminStatisticsApi.get())
if (stats) log('INFO', 'B4b 统计内容', JSON.stringify(stats).slice(0, 200))
const category = await step('B5 创建课程分类', () =>
  categoriesApi.create({ name: `联调分类${ts}`, sortOrder: 99, enabled: true }))
if (category) {
  const updated = await step('B6 更新课程分类', () =>
    categoriesApi.update(category.categoryId, { name: `联调分类改${ts}`, sortOrder: 98, enabled: true, version: category.version }))
  await step('B7 删除课程分类', () => categoriesApi.remove((updated ?? category).categoryId))
}

// ================= C 课程生命周期 =================
console.log('\n===== C 课程生命周期 =====')
await step('C0 teacher 登录', () => loginAs('teacher', 'teacher', 't123456'))
as('teacher')
const course = await step('C1 教师创建课程', () => teacherCoursesApi.create({
  courseCode: `LD-${ts}`, name: `联调课程${ts}`, summary: '前后端联调用课程', credit: 2,
}))
let publishedCourseId
if (course) {
  await step('C2 提交审核', () => teacherCoursesApi.submitReview(course.courseId))
  as('admin')
  await probe('C3 驳回缺原因被校验拒绝', ['PARAM_VALIDATION_ERROR'], () =>
    courseReviewsApi.reject(course.courseId, { reason: '' }))
  await step('C4 管理员驳回(带原因)', () => courseReviewsApi.reject(course.courseId, { reason: '资料不完整，请补充课程简介。' }))
  as('teacher')
  await step('C5 教师再次提交审核', () => teacherCoursesApi.submitReview(course.courseId))
  as('admin')
  await step('C6 管理员通过', () => courseReviewsApi.approve(course.courseId, {}))
  const reviewDetail = await step('C7 审核详情(历史)', () => courseReviewsApi.detail(course.courseId))
  if (reviewDetail) log('INFO', 'C7b 审核历史条数', String(reviewDetail.reviews?.length ?? reviewDetail.history?.length ?? 'n/a'))
  as('teacher')
  const pub = await step('C8 教师发布课程', () => teacherCoursesApi.publish(course.courseId))
  if (pub) {
    check('C8b 发布后状态 PUBLISHED', (pub.status?.code ?? pub.status) === 'PUBLISHED', `status=${JSON.stringify(pub.status)}`)
    publishedCourseId = course.courseId
  }
}

// ================= D 课程内容(21001) =================
console.log('\n===== D 课程内容管理 =====')
as('teacher')
const chapter = await step('D1 创建章节', () =>
  courseContentApi.createChapter('21001', { title: `联调章节${ts}`, description: '联调新增', sortOrder: 90 }))
if (chapter) {
  const upd = await step('D2 更新章节(正确版本)', () =>
    courseContentApi.updateChapter(chapter.chapterId, { title: `联调章节改${ts}`, description: '已更新', sortOrder: 90, version: chapter.version }))
  await probe('D3 过期版本更新被拒(乐观锁)', ['RESOURCE_CONFLICT', 'STALE_VERSION', 'CONFLICT'], () =>
    courseContentApi.updateChapter(chapter.chapterId, { title: '过期写入', description: null, sortOrder: 90, version: chapter.version }))
  const lesson = await step('D4 创建课时', () => courseContentApi.createLesson(chapter.chapterId, {
    title: `联调课时${ts}`, contentType: 'RICH_TEXT', content: '# 联调课时内容', estimatedMinutes: 15, sortOrder: 10, unlockType: 'IMMEDIATE',
  }))
  await step('D5 发布章节', () => courseContentApi.publishChapter(chapter.chapterId))
  if (lesson) await step('D6 发布课时', () => courseContentApi.publishLesson(lesson.lessonId))
  const material = await step('D7 创建外链资料', () => courseContentApi.createMaterial('21001', {
    chapterId: null, lessonId: null, name: `联调资料${ts}`, materialType: 'DOCUMENT',
    fileKey: 'mock/harness-material.pdf', fileSize: 2048, mimeType: 'application/pdf', visibility: 'COURSE', sortOrder: 50,
  }))
  if (material) await step('D8 删除资料', () => courseContentApi.deleteMaterial(material.materialId))
}

// ================= E 学生学习 =================
console.log('\n===== E 学生学习 =====')
as('student')
const catalog = await step('E1 课程目录', () => studentLearningApi.catalog({ page: 1, size: 20 }))
if (catalog) check('E1b 目录含已发布课程', catalog.records?.some((c) => c.courseId === '21001'),
  `total=${catalog.total}`)
if (publishedCourseId) {
  const enr1 = await step('E2 选修新发布课程', () => studentLearningApi.enroll(publishedCourseId))
  const enr2 = await step('E3 重复选修幂等返回', () => studentLearningApi.enroll(publishedCourseId))
  if (enr1 && enr2) check('E3b 幂等返回同一 enrollment', enr1.enrollmentId === enr2.enrollmentId,
    `first=${enr1.enrollmentId} second=${enr2.enrollmentId}`)
}
const outline = await step('E4 课程大纲(21001)', () => studentLearningApi.outline('21001'))
if (outline) {
  const lessonIds = (outline.chapters ?? []).flatMap((c) => (c.lessons ?? []).map((l) => l.lessonId))
  check('E4b 大纲含公开课时23001', lessonIds.includes('23001'), `lessons=${JSON.stringify(lessonIds).slice(0, 120)}`)
  check('E4c 草稿课时23003不可见', !lessonIds.includes('23003'))
}
await step('E5 课时详情(23001)', () => studentLearningApi.lessonDetail('23001'))
await step('E6 开始学习(23001)', () => studentLearningApi.startLesson('23001'))
const rec = await step('E7 完成学习(23001)', () => studentLearningApi.completeLesson('23001'))
if (rec) log('INFO', 'E7b 学习记录', JSON.stringify(rec).slice(0, 160))
const progress = await step('E8 课程进度(21001)', () => studentLearningApi.progress('21001'))
if (progress) log('INFO', 'E8b 进度', JSON.stringify(progress).slice(0, 200))
await probe('E9 未解锁课时(23002)拒绝学习', ['OPERATION_NOT_ALLOWED', 'LESSON_LOCKED', 'RESOURCE_CONFLICT', 'FORBIDDEN'], () =>
  studentLearningApi.startLesson('23002'))
const access = await step('E10 资料访问(24001)', () => studentLearningApi.materialAccess('24001'))
if (access) log('INFO', 'E10b 资料访问 VO', JSON.stringify(access).slice(0, 160))

// ================= F 作业 =================
console.log('\n===== F 作业 =====')
as('teacher')
const assignment = await step('F1 创建作业', () => assignmentsApi.create('21001', {
  title: `联调作业${ts}`, description: '联调作业描述', maxScore: 100, dueAt: iso(2 * 3600e3),
}))
if (assignment) {
  await step('F2 发布作业', () => assignmentsApi.publish(assignment.assignmentId))
  as('student')
  await probe('F3 空提交被校验拒绝', ['PARAM_VALIDATION_ERROR'], () =>
    assignmentsApi.submit(assignment.assignmentId, {}))
  const draft = await step('F4 保存草稿', () => assignmentsApi.saveDraft(assignment.assignmentId, { content: '草稿内容' }))
  const submitted = await step('F5 正式提交', () =>
    assignmentsApi.submit(assignment.assignmentId, { content: '联调正式提交', version: draft?.version }))
  await probe('F6 重复提交被拒', ['RESOURCE_CONFLICT', 'OPERATION_NOT_ALLOWED', 'DUPLICATE_SUBMISSION'], () =>
    assignmentsApi.submit(assignment.assignmentId, { content: '再次提交', version: submitted?.version }))
  as('teacher')
  const subs = await step('F7 教师查看提交列表', () => assignmentsApi.listSubmissions(assignment.assignmentId, { page: 1, size: 10 }))
  const sub = subs?.records?.[0]
  if (sub) {
    const graded = await step('F8 批改', () => assignmentsApi.grade(sub.submissionId, {
      score: 88, maxScore: 100, teacherComment: '完成度较高', version: sub.version,
    }))
    if (graded) {
      const published = await step('F9 发布成绩', () => assignmentsApi.publishGrade(graded.gradeId, { version: graded.gradeVersion ?? 0 }))
      if (published) log('INFO', 'F9b 成绩状态', JSON.stringify(published.gradeStatus ?? published.status ?? published).slice(0, 120))
    }
    as('student')
    const grades = await step('F10 学生成绩单', () => assignmentsApi.studentGrades({ page: 1, size: 20 }))
    if (grades) check('F10b 成绩单含本次作业', grades.records?.some((g) => g.assignmentId === assignment.assignmentId),
      `items=${grades.records?.length}`)
    as('teacher')
    const st = await step('F11 作业统计', () => assignmentsApi.statistics(assignment.assignmentId))
    if (st) log('INFO', 'F11b 统计', JSON.stringify(st).slice(0, 200))
    const cst = await step('F12 课程成绩统计', () => assignmentsApi.courseGradeStatistics('21001'))
    if (cst) log('INFO', 'F12b 课程统计', JSON.stringify(cst).slice(0, 200))
  }
}

// ================= G 论坛 =================
console.log('\n===== G 论坛 =====')
as('student')
const topic = await step('G1 学生发主题', () => forumApi.createTopic('21001', { title: `联调主题${ts}`, content: '联调讨论内容' }))
if (topic) {
  await step('G2 学生回帖', () => forumApi.createReply(topic.topicId, { content: '自己补充一句' }))
  await step('G3 学生看主题详情', () => forumApi.topicDetail(topic.topicId))
  as('teacher')
  const tDetail = await step('G3b 教师看主题详情(teacherTopicDetail)', () => forumApi.teacherTopicDetail(topic.topicId))
  if (tDetail) check('G3c 详情 status 为 CodeLabel', tDetail.status?.code === 'VISIBLE', JSON.stringify(tDetail.status))
  const tReply = await step('G3d 教师回帖', () => forumApi.teacherCreateReply(topic.topicId, { content: '教师补充说明' }))
  const tReplies = await step('G3e 教师回复列表', () => forumApi.teacherListReplies(topic.topicId, { page: 1, size: 20 }))
  if (tReplies && tReply) check('G3f 列表含教师回帖', tReplies.records?.some((r) => r.replyId === tReply.replyId), `records=${tReplies.records?.length}`)
  await step('G4 教师隐藏主题', () => forumApi.teacherTopicVisibility(topic.topicId, {
    visible: false, reason: '联调隐藏测试', version: (tDetail ?? topic).version,
  }))
  const hidden = await step('G4b 教师仍可见被隐藏主题', () => forumApi.teacherTopicDetail(topic.topicId))
  if (hidden) check('G4c 隐藏后 status=HIDDEN', hidden.status?.code === 'HIDDEN', JSON.stringify(hidden.status))
  as('student')
  const list = await step('G5 学生主题列表', () => forumApi.studentTopics('21001', { page: 1, size: 20 }))
  if (list) check('G5b 被隐藏主题学生不可见', Array.isArray(list.records) && !list.records.some((t) => t.topicId === topic.topicId), `records=${list.records?.length}`)
}

// ================= H 学习预警 =================
console.log('\n===== H 学习预警 =====')
as('teacher')
const gen = await step('H1 dryRun 生成预警(MISSING_ASSIGNMENT)', () =>
  warningsApi.generate('21001', { warningTypes: ['MISSING_ASSIGNMENT'], dryRun: true }))
if (gen) log('INFO', 'H1b 生成结果', JSON.stringify(gen).slice(0, 200))
const wlist = await step('H2 教师预警列表', () => warningsApi.teacherList('21001', { page: 1, size: 20 }))
if (wlist?.records?.[0]) check('H2b 预警枚举字段为 CodeLabel', !!wlist.records[0].warningStatus?.code && !!wlist.records[0].warningType?.code,
  JSON.stringify({ s: wlist.records[0].warningStatus, t: wlist.records[0].warningType }))
const openWarning = wlist?.records?.find((w) => (w.warningStatus?.code ?? w.warningStatus) === 'OPEN')
if (openWarning) {
  await step('H3 处理预警', () => warningsApi.handle(openWarning.warningId, {
    action: 'HANDLED', remark: '联调处理', version: openWarning.version,
  }))
} else if (wlist?.records?.some((w) => (w.warningStatus?.code ?? w.warningStatus) === 'HANDLED')) {
  log('INFO', 'H3 处理预警', '无 OPEN 预警（种子预警已在此前运行中处理）')
} else log('WARN', 'H3 处理预警', '教师列表中没有 OPEN 状态预警可处理')
as('student')
const swlist = await step('H4 学生预警列表', () => warningsApi.studentList({ page: 1, size: 20 }))
if (swlist) log('INFO', 'H4b 学生可见预警数', String(swlist.total))

// ================= I 考试 =================
console.log('\n===== I 考试 =====')
as('teacher')
const bank = await step('I1 创建题库', () => examsApi.createBank('21001', { name: `联调题库${ts}`, description: '联调' }))
let question
if (bank) {
  await probe('I2 单选两个正确项被校验拒绝', ['PARAM_VALIDATION_ERROR'], () =>
    examsApi.createQuestion(bank.bankId, {
      questionType: 'SINGLE_CHOICE', stem: '非法题目', difficulty: 'EASY', score: 5,
      options: [
        { label: 'A', content: '甲', correct: true, sortOrder: 10 },
        { label: 'B', content: '乙', correct: true, sortOrder: 20 },
      ],
    }))
  question = await step('I3 创建单选题(B为正确)', () => examsApi.createQuestion(bank.bankId, {
    questionType: 'SINGLE_CHOICE', stem: `联调单选题${ts}：正确项是？`, analysis: '选 B', difficulty: 'EASY', score: 5,
    options: [
      { label: 'A', content: '错误选项', correct: false, sortOrder: 10 },
      { label: 'B', content: '正确选项', correct: true, sortOrder: 20 },
      { label: 'C', content: '干扰选项', correct: false, sortOrder: 30 },
    ],
  }))
}
const exam = await step('I4 创建考试(窗口 now-1m..+1h)', () => examsApi.createExam('21001', {
  title: `联调考试${ts}`, description: '联调', startAt: iso(-60e3), endAt: iso(3600e3), durationMinutes: 30, totalScore: 5,
}))
if (exam && question) {
  const paper = await step('I5 创建试卷', () => examsApi.createPaper(exam.examId, {
    title: `联调试卷${ts}`, questions: [{ questionId: question.questionId, questionOrder: 1, score: 5 }],
  }))
  if (paper) {
    await step('I6 发布试卷', () => examsApi.publishPaper(paper.paperId))
    const examAfter = await step('I7 试卷发布后查考试状态', () => examsApi.getExam(exam.examId))
    if (examAfter) log('INFO', 'I7b 考试状态', JSON.stringify(examAfter.status ?? examAfter).slice(0, 120))
    as('student')
    const attempt1 = await step('I8 学生开考', () => examsApi.startAttempt(exam.examId))
    const attempt2 = await step('I9 重复开考(观察幂等)', () => examsApi.startAttempt(exam.examId))
    if (attempt1 && attempt2) check('I9b 重复开考返回同一 attempt', attempt1.attemptId === attempt2.attemptId,
      `a1=${attempt1.attemptId} a2=${attempt2.attemptId}`)
    if (attempt1) {
      const submitted = await step('I10 交卷(答B)', () => examsApi.submitAttempt(attempt1.attemptId, {
        answers: [{ questionId: question.questionId, answerContent: 'B' }],
        version: (attempt2 ?? attempt1).version,
      }))
      if (submitted) check('I10b 客观题自动判分=5', Number(submitted.totalScore ?? submitted.score) === 5,
        `score=${submitted.totalScore ?? submitted.score} status=${JSON.stringify(submitted.status ?? submitted.attemptStatus)}`)
    }
    as('teacher')
    const attempts = await step('I11 教师查看答卷列表', () => examsApi.listAttempts(exam.examId, { page: 1, size: 10 }))
    if (attempts) log('INFO', 'I11b 答卷数', String(attempts.total))
    as('student')
    const sexams = await step('I12 学生考试列表(21001)', () => examsApi.studentExams('21001', { page: 1, size: 20 }))
    if (sexams) check('I12b 列表含本次考试', sexams.records?.some((e) => e.examId === exam.examId), `total=${sexams.total}`)
  }
}

// ================= J 公告 =================
console.log('\n===== J 公告 =====')
as('teacher')
const cAnn = await step('J1 教师发课程公告(STUDENT)', () =>
  announcementsApi.createCourseAnnouncement('21001', { title: `课程公告${ts}`, content: '联调课程公告', audience: 'STUDENT' }))
as('student')
const sAnnList = await step('J2 学生公告列表', () => announcementsApi.studentList({ page: 1, size: 20 }))
if (sAnnList && cAnn) check('J2b 学生能看到课程公告', sAnnList.records?.some((a) => a.announcementId === cAnn.announcementId),
  `total=${sAnnList.total}`)
as('admin')
const gAnn = await step('J3 管理员发全局公告(TEACHER)', () =>
  announcementsApi.adminCreate({ title: `全局公告${ts}`, content: '仅教师可见', audience: 'TEACHER' }))
if (gAnn) {
  as('student')
  const sList2 = await step('J4 学生列表不含 TEACHER 公告', () => announcementsApi.studentList({ page: 1, size: 50 }))
  if (sList2) check('J4b 受众隔离', Array.isArray(sList2.records) && !sList2.records.some((a) => a.announcementId === gAnn.announcementId), `records=${sList2.records?.length}`)
  as('admin')
  await step('J5 管理员撤回全局公告', () => announcementsApi.adminWithdraw(gAnn.announcementId, { version: gAnn.version }))
}
if (cAnn) {
  as('teacher')
  await step('J6 教师撤回课程公告', () => announcementsApi.teacherWithdraw(cAnn.announcementId, { version: cAnn.version }))
}

// ================= K 文件 =================
console.log('\n===== K 文件 =====')
as('teacher')
const file = new File([new TextEncoder().encode(`联调上传内容 ${ts}`)], 'harness.txt', { type: 'text/plain' })
const stored = await step('K1 上传文件', () => filesApi.upload(file, 'GENERAL'))
if (stored) {
  log('INFO', 'K1b 文件 VO', JSON.stringify(stored).slice(0, 200))
  const meta = await step('K2 文件元数据', () => filesApi.getMeta(stored.fileId))
  if (meta) check('K2b 元数据一致', meta.fileId === stored.fileId && meta.originalName === 'harness.txt',
    `name=${meta.originalName} size=${meta.fileSize}`)
  await step('K3 删除文件', () => filesApi.remove(stored.fileId))
}

// ================= L AI =================
console.log('\n===== L AI =====')
as('admin')
const aiStatus = await step('L1 AI 服务状态', () => aiApi.adminStatus())
if (aiStatus) log('INFO', 'L1b 状态', JSON.stringify(aiStatus))
as('student')
const events = []
try {
  await aiApi.qaStream('21001', { question: '这门课的核心概念是什么？', lessonId: '23001' }, (e) => events.push(e))
  const types = events.map((e) => e.type)
  log(events.length ? 'PASS' : 'WARN', 'L2 课程问答 SSE', `events=${JSON.stringify(types)}`)
  const errEvent = events.find((e) => e.type === 'error')
  if (errEvent) log('INFO', 'L2b error 事件', JSON.stringify(errEvent).slice(0, 300))
  const delta = events.filter((e) => e.type === 'delta').map((e) => e.data).join('')
  if (delta) log('INFO', 'L2c 回答片段', delta.slice(0, 160))
} catch (e) {
  log('WARN', 'L2 课程问答 SSE', `抛错：${e.code} ${e.message}（provider=none 下的行为，作为联调发现记录）`)
}
as('teacher')
try {
  const draft = await aiApi.lessonSummaryDraft('23001', { courseId: '21001' })
  log('PASS', 'L3 课时摘要草稿', JSON.stringify({ status: draft.status, provider: draft.provider }).slice(0, 160))
} catch (e) {
  log('WARN', 'L3 课时摘要草稿', `抛错：${e.code} ${e.message}（provider=none 下的行为，作为联调发现记录）`)
}

// ---- 汇总 ----
console.log('\n===== 汇总 =====')
const tally = { PASS: 0, FAIL: 0, WARN: 0, INFO: 0 }
for (const r of results) tally[r.status] = (tally[r.status] ?? 0) + 1
console.log(JSON.stringify(tally))
for (const r of results.filter((x) => x.status === 'FAIL' || x.status === 'WARN')) {
  console.log(`  ${r.status}: ${r.name} — ${r.detail}`)
}
writeFileSync(new URL('./results.json', import.meta.url), JSON.stringify(results, null, 2))
await server.close()
process.exit(tally.FAIL > 0 ? 1 : 0)
