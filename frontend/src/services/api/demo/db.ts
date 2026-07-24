// 演示模式的本地数据库：按 api-reference.md 的契约形状保存可变数据，
// 生成 ID、模拟乐观锁并序列化到 localStorage；resetDemoData() 恢复种子状态。
import { RuntimeError } from '../../runtime'
import type {
  AnnouncementAudience,
  AssignmentAttachmentVO,
  CodeLabel,
  ExamPaperQuestionRequest,
  FilePurpose,
  NotificationCategory,
  PageResponse,
  QuestionOptionRequest,
  WarningType,
} from '../types'

const STORAGE_KEY = 'smart-education-demo-db-v1'

export interface UserRow {
  userId: string
  username: string
  password: string
  displayName: string
  avatarFileId?: string | null
  roles: string[]
  userStatus: 'PENDING' | 'ENABLED' | 'DISABLED' | 'REJECTED'
  superAdministrator: boolean
  createdAt: string
  version: number
}

export interface FileRow {
  fileId: string
  originalName: string
  objectKey: string
  accessUrl: string
  fileSize: number
  mimeType: string
  sha256: string
  purpose: FilePurpose
  ownerId: string
  uploadedAt: string
  version: number
}

export interface CategoryRow {
  categoryId: string
  name: string
  sortOrder: number
  enabled: boolean
  version: number
}

export interface CourseTemplateRow {
  templateId: string
  courseCode: string
  name: string
  summary: string | null
}

export interface TermWindowRow {
  windowId: string
  term: string
  enrollmentOpenAt: string | null
  enrollmentCloseAt: string | null
  version: number
}

export interface CourseRow {
  courseId: string
  courseCode: string
  name: string
  summary?: string | null
  coverUrl?: string | null
  categoryId?: string | null
  term?: string | null
  department?: string | null
  credit?: number | null
  ownerTeacherId: string
  status: string
  reviewStatus: string
  enrollmentOpenAt?: string | null
  enrollmentCloseAt?: string | null
  startAt?: string | null
  endAt?: string | null
  latestReviewReason?: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface CourseTeacherRow {
  relationId: string
  courseId: string
  teacherId: string
  role: string
  // ACTIVE 已加入 / PENDING 待确认（被邀请未接受）。
  status: string
  version: number
}

export interface ChapterRow {
  chapterId: string
  courseId: string
  title: string
  description?: string | null
  sortOrder: number
  status: string
  publishedAt?: string | null
  version: number
}

export interface LessonRow {
  lessonId: string
  courseId: string
  chapterId: string
  title: string
  contentType: string
  content?: string | null
  videoUrl?: string | null
  estimatedMinutes?: number | null
  sortOrder: number
  status: string
  unlockType: string
  unlockAt?: string | null
  publishedAt?: string | null
  version: number
}

export interface MaterialRow {
  materialId: string
  courseId: string
  chapterId?: string | null
  lessonId?: string | null
  name: string
  materialType: string
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  fileSize?: number | null
  mimeType?: string | null
  extractedText?: string | null
  extractionStatus?: string | null
  visibility: string
  status: string
  sortOrder: number
  version: number
}

export interface EnrollmentRow {
  enrollmentId: string
  courseId: string
  studentId: string
  status: string
  enrolledAt?: string | null
  withdrawnAt?: string | null
  version: number
}

export interface LearningRecordRow {
  recordId: string
  courseId: string
  chapterId: string
  lessonId: string
  studentId: string
  status: string
  studySeconds?: number
  startedAt?: string | null
  completedAt?: string | null
  lastStudiedAt?: string | null
}

export interface ReviewRow {
  reviewId: string
  courseId: string
  reviewStatus: string
  reviewerId: string
  reason?: string | null
  remark?: string | null
  reviewedAt: string
}

export interface AssignmentRow {
  assignmentId: string
  courseId: string
  lessonId?: string | null
  title: string
  description?: string | null
  responseMode?: string
  questions?: import('../types').AssignmentQuestion[]
  maxScore: number
  assignmentStatus: string
  openAt?: string | null
  dueAt: string
  publishedAt?: string | null
  attachments: AssignmentAttachmentVO[]
  source: 'AI' | 'HUMAN'
  version: number
}

export interface SubmissionRow {
  submissionId: string
  assignmentId: string
  courseId: string
  studentId: string
  attemptNo: number
  content?: string | null
  answers?: Record<string, string[]>
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  submissionStatus: string
  submittedAt?: string | null
  score?: number | null
  teacherComment?: string | null
  aiCommentDraftId?: string | null
  gradedBy?: string | null
  gradedAt?: string | null
  gradeId?: string | null
  publishedAt?: string | null
  version: number
}

export interface GradeRow {
  gradeId: string
  courseId: string
  assignmentId: string
  studentId: string
  submissionId: string
  score: number
  maxScore: number
  teacherComment?: string | null
  status: 'DRAFT' | 'PUBLISHED'
  publishedAt?: string | null
  version: number
}

export interface TopicRow {
  topicId: string
  courseId: string
  title: string
  content: string
  authorId: string
  status: 'VISIBLE' | 'HIDDEN'
  pinned: boolean
  moderationReason?: string | null
  moderatedBy?: string | null
  moderatedAt?: string | null
  createdAt: string
  version: number
}

export interface ReplyRow {
  replyId: string
  topicId: string
  courseId: string
  authorId: string
  parentReplyId?: string | null
  content: string
  status: 'VISIBLE' | 'HIDDEN'
  moderationReason?: string | null
  moderatedBy?: string | null
  moderatedAt?: string | null
  createdAt: string
  version: number
}

export interface WarningRow {
  warningId: string
  courseId: string
  studentId: string
  warningType: WarningType
  warningLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  warningStatus: 'OPEN' | 'HANDLED' | 'IGNORED'
  summary: string
  suggestion?: string | null
  aiExplanationDraftId?: string | null
  generatedAt: string
  handledBy?: string | null
  handleRemark?: string | null
  handledAt?: string | null
  evidences: { evidenceId: string; evidenceType: string; sourceId?: string | null; metricCode?: string | null; metricValue?: number | null; description: string }[]
  version: number
}

export interface QuestionBankRow {
  bankId: string
  courseId: string
  name: string
  description?: string | null
  status: string
  source: string
  version: number
}

export interface QuestionRow {
  questionId: string
  bankId: string
  courseId: string
  questionType: string
  stem: string
  analysis?: string | null
  difficulty: string
  score: number
  status: string
  options: QuestionOptionRequest[]
  version: number
}

export interface ExamRow {
  examId: string
  courseId: string
  title: string
  description?: string | null
  status: string
  startAt: string
  endAt: string
  durationMinutes: number
  totalScore: number
  source: 'AI' | 'HUMAN'
  version: number
}

export interface PaperRow {
  paperId: string
  examId: string
  courseId: string
  title: string
  totalScore: number
  status: string
  questions: ExamPaperQuestionRequest[]
  version: number
}

export interface AttemptRow {
  attemptId: string
  examId: string
  paperId: string
  studentId: string
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED'
  startedAt: string
  deadlineAt?: string | null
  submittedAt?: string | null
  score?: number | null
  answers: { questionId: string; answerContent: string; score?: number | null; teacherComment?: string | null }[]
  version: number
}

export interface AnnouncementRow {
  announcementId: string
  scopeType: 'COURSE' | 'SYSTEM'
  courseId?: string | null
  title: string
  content: string
  audience: AnnouncementAudience
  status: 'DRAFT' | 'PUBLISHED' | 'WITHDRAWN'
  publishedAt?: string | null
  withdrawnAt?: string | null
  publisherId: string
  source: 'AI' | 'HUMAN'
  version: number
}

export interface NotificationReadRow {
  userId: string
  notificationId: string
  readAt: string
}

export interface NotificationArchiveRow {
  userId: string
  notificationId: string
  archivedAt: string
}

export interface NotificationPreferenceRow {
  userId: string
  enabledCategories: NotificationCategory[]
}

export interface DemoDb {
  seq: number
  session: { userId: string | null }
  users: UserRow[]
  files: FileRow[]
  categories: CategoryRow[]
  courseTemplates: CourseTemplateRow[]
  courses: CourseRow[]
  courseTeachers: CourseTeacherRow[]
  chapters: ChapterRow[]
  lessons: LessonRow[]
  materials: MaterialRow[]
  enrollments: EnrollmentRow[]
  learningRecords: LearningRecordRow[]
  courseReviews: ReviewRow[]
  assignments: AssignmentRow[]
  submissions: SubmissionRow[]
  grades: GradeRow[]
  forumTopics: TopicRow[]
  forumReplies: ReplyRow[]
  warnings: WarningRow[]
  questionBanks: QuestionBankRow[]
  questions: QuestionRow[]
  exams: ExamRow[]
  papers: PaperRow[]
  attempts: AttemptRow[]
  announcements: AnnouncementRow[]
  notificationReads: NotificationReadRow[]
  notificationArchives: NotificationArchiveRow[]
  notificationPreferences: NotificationPreferenceRow[]
  termEnrollmentWindows: TermWindowRow[]
}

export function nowIso(): string {
  return new Date().toISOString()
}

function daysFromNow(days: number): string {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString()
}

function seed(): DemoDb {
  return {
    seq: 90001,
    session: { userId: null },
    users: [
      { userId: '1', username: 'admin', password: '123456', displayName: '周敏', roles: ['ADMIN', 'SUPER_ADMIN'], userStatus: 'ENABLED', superAdministrator: true, createdAt: daysFromNow(-180), version: 0 },
      { userId: '2', username: 'teacher', password: '123456', displayName: '李明', roles: ['TEACHER'], userStatus: 'ENABLED', superAdministrator: false, createdAt: daysFromNow(-160), version: 0 },
      { userId: '3', username: 'teacher.chen', password: '123456', displayName: '陈若溪', roles: ['TEACHER'], userStatus: 'ENABLED', superAdministrator: false, createdAt: daysFromNow(-150), version: 0 },
      { userId: '4', username: 'student', password: '123456', displayName: '王一诺', roles: ['STUDENT'], userStatus: 'ENABLED', superAdministrator: false, createdAt: daysFromNow(-120), version: 0 },
      { userId: '5', username: 'student.liu', password: '123456', displayName: '刘子涵', roles: ['STUDENT'], userStatus: 'ENABLED', superAdministrator: false, createdAt: daysFromNow(-110), version: 0 },
      { userId: '6', username: 'student.zhao', password: '123456', displayName: '赵晨', roles: ['STUDENT'], userStatus: 'ENABLED', superAdministrator: false, createdAt: daysFromNow(-100), version: 0 },
      { userId: '7', username: 'teacher.gao', password: '123456', displayName: '高翔', roles: ['TEACHER'], userStatus: 'PENDING', superAdministrator: false, createdAt: daysFromNow(-3), version: 0 },
    ],
    files: [
      { fileId: '71001', originalName: 'python-basics.pdf', objectKey: 'course-material/71001/python-basics.pdf', accessUrl: '/api/v1/files/71001/content', fileSize: 1048576, mimeType: 'application/pdf', sha256: 'demo-sha256-71001', purpose: 'COURSE_MATERIAL', ownerId: '2', uploadedAt: daysFromNow(-40), version: 0 },
    ],
    categories: [
      { categoryId: '1', name: '计算机基础', sortOrder: 1, enabled: true, version: 0 },
      { categoryId: '2', name: '人工智能', sortOrder: 2, enabled: true, version: 0 },
      { categoryId: '3', name: '数据科学', sortOrder: 3, enabled: true, version: 0 },
    ],
    courseTemplates: [
      { templateId: '61001', courseCode: 'PY101', name: 'Python 程序设计', summary: '面向零基础的 Python 入门课程，覆盖语法、函数与常用库。' },
      { templateId: '61002', courseCode: 'DS301', name: '数据结构与算法', summary: '线性表、树、图与经典算法设计。' },
      { templateId: '61003', courseCode: 'AI201', name: '人工智能导论', summary: '介绍人工智能的核心范式与典型应用。' },
      { templateId: '61004', courseCode: 'ST401', name: '概率论与数理统计', summary: '概率、随机变量与统计推断。' },
      { templateId: '61005', courseCode: 'WEB201', name: 'Web 前端开发', summary: 'HTML/CSS/JavaScript 与现代前端框架实践。' },
      { templateId: '61006', courseCode: 'OS301', name: '操作系统原理', summary: '进程与线程、内存管理、文件系统与并发。' },
    ],
    courses: [
      { courseId: '21001', courseCode: 'PY101', name: 'Python 程序设计', summary: '面向零基础的 Python 入门课程，覆盖语法、函数与常用库。', categoryId: '1', term: '2026 春', department: '计算机学院', credit: 3, ownerTeacherId: '2', status: 'PUBLISHED', reviewStatus: 'APPROVED', enrollmentOpenAt: daysFromNow(-60), enrollmentCloseAt: daysFromNow(30), startAt: daysFromNow(-45), endAt: daysFromNow(60), createdAt: daysFromNow(-90), updatedAt: daysFromNow(-2), version: 2 },
      { courseId: '21002', courseCode: 'AI201', name: '人工智能导论', summary: '介绍人工智能的核心范式与典型应用。', categoryId: '2', term: '2026 春', department: '计算机学院', credit: 2, ownerTeacherId: '2', status: 'PUBLISHED', reviewStatus: 'APPROVED', enrollmentOpenAt: daysFromNow(-50), enrollmentCloseAt: daysFromNow(20), startAt: daysFromNow(-30), endAt: daysFromNow(75), createdAt: daysFromNow(-80), updatedAt: daysFromNow(-5), version: 1 },
      { courseId: '21003', courseCode: 'DS301', name: '数据结构与算法', summary: '线性表、树、图与经典算法设计。', categoryId: '1', term: '2026 秋', department: '计算机学院', credit: 4, ownerTeacherId: '2', status: 'DRAFT', reviewStatus: 'PENDING', createdAt: daysFromNow(-10), updatedAt: daysFromNow(-1), version: 1 },
      { courseId: '21004', courseCode: 'ST401', name: '概率论与数理统计', summary: '概率、随机变量与统计推断。', categoryId: '3', term: '2026 春', department: '数学学院', credit: 3, ownerTeacherId: '3', status: 'PUBLISHED', reviewStatus: 'APPROVED', startAt: daysFromNow(-40), endAt: daysFromNow(50), createdAt: daysFromNow(-70), updatedAt: daysFromNow(-8), version: 1 },
    ],
    courseTeachers: [
      { relationId: '25001', courseId: '21001', teacherId: '2', role: 'OWNER', status: 'ACTIVE', version: 0 },
      { relationId: '25002', courseId: '21002', teacherId: '2', role: 'OWNER', status: 'ACTIVE', version: 0 },
      { relationId: '25003', courseId: '21003', teacherId: '2', role: 'OWNER', status: 'ACTIVE', version: 0 },
      { relationId: '25004', courseId: '21004', teacherId: '3', role: 'OWNER', status: 'ACTIVE', version: 0 },
      { relationId: '25005', courseId: '21001', teacherId: '3', role: 'COLLABORATOR', status: 'ACTIVE', version: 0 },
    ],
    chapters: [
      { chapterId: '22001', courseId: '21001', title: '第一章 Python 基础语法', description: '变量、类型与控制流程。', sortOrder: 1, status: 'PUBLISHED', publishedAt: daysFromNow(-44), version: 1 },
      { chapterId: '22002', courseId: '21001', title: '第二章 函数与模块', description: '函数定义、作用域与模块化。', sortOrder: 2, status: 'PUBLISHED', publishedAt: daysFromNow(-30), version: 1 },
      { chapterId: '22003', courseId: '21002', title: '第一章 人工智能概述', sortOrder: 1, status: 'PUBLISHED', publishedAt: daysFromNow(-29), version: 1 },
      { chapterId: '22004', courseId: '21003', title: '第一章 线性表', sortOrder: 1, status: 'DRAFT', version: 0 },
    ],
    lessons: [
      { lessonId: '23001', courseId: '21001', chapterId: '22001', title: '变量与数据类型', contentType: 'TEXT', content: 'Python 的基本数据类型包括数值、字符串、列表、元组、字典与集合。变量无需声明类型，赋值即定义。', estimatedMinutes: 30, sortOrder: 1, status: 'PUBLISHED', unlockType: 'IMMEDIATE', publishedAt: daysFromNow(-44), version: 1 },
      { lessonId: '23002', courseId: '21001', chapterId: '22001', title: '控制流程', contentType: 'VIDEO', videoUrl: 'https://example.com/videos/py-control-flow', estimatedMinutes: 45, sortOrder: 2, status: 'PUBLISHED', unlockType: 'IMMEDIATE', publishedAt: daysFromNow(-42), version: 1 },
      { lessonId: '23003', courseId: '21001', chapterId: '22002', title: '函数定义与作用域', contentType: 'TEXT', content: '使用 def 定义函数；理解 LEGB 作用域规则与默认参数的求值时机。', estimatedMinutes: 40, sortOrder: 1, status: 'PUBLISHED', unlockType: 'IMMEDIATE', publishedAt: daysFromNow(-28), version: 1 },
      { lessonId: '23004', courseId: '21002', chapterId: '22003', title: '什么是人工智能', contentType: 'TEXT', content: '人工智能研究让机器完成需要人类智能的任务，主要范式包括符号主义、连接主义与行为主义。', estimatedMinutes: 35, sortOrder: 1, status: 'PUBLISHED', unlockType: 'IMMEDIATE', publishedAt: daysFromNow(-29), version: 1 },
      { lessonId: '23005', courseId: '21003', chapterId: '22004', title: '顺序表', contentType: 'TEXT', content: '顺序表以连续存储实现线性表。', estimatedMinutes: 40, sortOrder: 1, status: 'DRAFT', unlockType: 'IMMEDIATE', version: 0 },
    ],
    materials: [
      { materialId: '24001', courseId: '21001', chapterId: '22001', name: '讲义《Python 基础》', materialType: 'DOCUMENT', fileId: '71001', fileSize: 1048576, mimeType: 'application/pdf', extractedText: 'Python 基础讲义正文，包含变量、类型、控制流与函数。', extractionStatus: 'COMPLETED', visibility: 'ENROLLED_ONLY', status: 'PUBLISHED', sortOrder: 1, version: 0 },
      { materialId: '24002', courseId: '21002', name: '课程参考资料', materialType: 'LINK', fileUrl: 'https://example.com/ai-intro-readings', extractedText: '人工智能导论参考资料正文。', extractionStatus: 'COMPLETED', visibility: 'PUBLIC', status: 'PUBLISHED', sortOrder: 1, version: 0 },
    ],
    enrollments: [
      { enrollmentId: '26001', courseId: '21001', studentId: '4', status: 'ENROLLED', enrolledAt: daysFromNow(-44), version: 0 },
      { enrollmentId: '26002', courseId: '21002', studentId: '4', status: 'ENROLLED', enrolledAt: daysFromNow(-28), version: 0 },
      { enrollmentId: '26003', courseId: '21001', studentId: '5', status: 'ENROLLED', enrolledAt: daysFromNow(-40), version: 0 },
      { enrollmentId: '26004', courseId: '21001', studentId: '6', status: 'ENROLLED', enrolledAt: daysFromNow(-38), version: 0 },
      { enrollmentId: '26005', courseId: '21004', studentId: '4', status: 'WITHDRAWN', enrolledAt: daysFromNow(-35), withdrawnAt: daysFromNow(-20), version: 1 },
    ],
    learningRecords: [
      { recordId: '27001', courseId: '21001', chapterId: '22001', lessonId: '23001', studentId: '4', status: 'COMPLETED', startedAt: daysFromNow(-43), completedAt: daysFromNow(-42), lastStudiedAt: daysFromNow(-42) },
      { recordId: '27002', courseId: '21001', chapterId: '22001', lessonId: '23002', studentId: '4', status: 'IN_PROGRESS', startedAt: daysFromNow(-2), lastStudiedAt: daysFromNow(-1) },
    ],
    courseReviews: [
      { reviewId: '28001', courseId: '21002', reviewStatus: 'APPROVED', reviewerId: '1', remark: '课程结构完整，通过审核。', reviewedAt: daysFromNow(-30) },
    ],
    assignments: [
      { assignmentId: '31001', courseId: '21001', lessonId: '23001', title: '第一章课后练习', description: '完成变量与数据类型的练习题，并附运行截图。', maxScore: 100, assignmentStatus: 'PUBLISHED', openAt: daysFromNow(-20), dueAt: daysFromNow(10), publishedAt: daysFromNow(-20), attachments: [], source: 'HUMAN', version: 1 },
      { assignmentId: '31002', courseId: '21001', title: '第二章编程作业', description: '实现一个带默认参数的函数并编写测试。', maxScore: 100, assignmentStatus: 'PUBLISHED', openAt: daysFromNow(-10), dueAt: daysFromNow(14), publishedAt: daysFromNow(-10), attachments: [], source: 'HUMAN', version: 1 },
      { assignmentId: '31003', courseId: '21002', title: 'AI 应用调研报告', description: '选择一个行业调研 AI 应用现状。', maxScore: 100, assignmentStatus: 'DRAFT', dueAt: daysFromNow(21), attachments: [], source: 'AI', version: 0 },
    ],
    submissions: [
      { submissionId: '32001', assignmentId: '31001', courseId: '21001', studentId: '4', attemptNo: 1, content: '已完成练习 1-10，截图见附件链接。', submissionStatus: 'SUBMITTED', submittedAt: daysFromNow(-3), version: 1 },
      { submissionId: '32002', assignmentId: '31001', courseId: '21001', studentId: '5', attemptNo: 1, content: '练习完成，含边界样例说明。', submissionStatus: 'GRADED', submittedAt: daysFromNow(-5), score: 88, teacherComment: '思路清楚，注意补充异常输入的处理。', gradedBy: '2', gradedAt: daysFromNow(-2), gradeId: '33001', publishedAt: daysFromNow(-1), version: 2 },
      { submissionId: '32003', assignmentId: '31002', courseId: '21001', studentId: '4', attemptNo: 1, content: '草稿：函数骨架已完成，测试待补。', submissionStatus: 'DRAFT', version: 0 },
    ],
    grades: [
      { gradeId: '33001', courseId: '21001', assignmentId: '31001', studentId: '5', submissionId: '32002', score: 88, maxScore: 100, teacherComment: '思路清楚，注意补充异常输入的处理。', status: 'PUBLISHED', publishedAt: daysFromNow(-1), version: 1 },
    ],
    forumTopics: [
      { topicId: '41001', courseId: '21001', title: '列表和元组的区别是什么？', content: '看完第一课还是不太理解什么场景该用元组。', authorId: '4', status: 'VISIBLE', pinned: false, createdAt: daysFromNow(-6), version: 0 },
      { topicId: '41002', courseId: '21001', title: '第二章作业的测试样例怎么写？', content: '默认参数的测试要覆盖哪些情况？', authorId: '5', status: 'VISIBLE', pinned: true, createdAt: daysFromNow(-4), version: 0 },
      { topicId: '41003', courseId: '21002', title: '广告帖示例', content: '（该帖已被隐藏）', authorId: '6', status: 'HIDDEN', pinned: false, moderationReason: '含广告内容', moderatedBy: '2', moderatedAt: daysFromNow(-2), createdAt: daysFromNow(-3), version: 1 },
    ],
    forumReplies: [
      { replyId: '42001', topicId: '41001', courseId: '21001', authorId: '2', content: '关键区别是可变性：元组不可变，适合作为字典键或固定结构记录。', status: 'VISIBLE', createdAt: daysFromNow(-5), version: 0 },
      { replyId: '42002', topicId: '41001', courseId: '21001', authorId: '5', parentReplyId: '42001', content: '补充：元组还能明确表达“这组数据不该被修改”的意图。', status: 'VISIBLE', createdAt: daysFromNow(-5), version: 0 },
      { replyId: '42003', topicId: '41002', courseId: '21001', authorId: '2', content: '至少覆盖：默认值命中、显式传参覆盖默认值、可变默认参数陷阱。', status: 'VISIBLE', createdAt: daysFromNow(-3), version: 0 },
    ],
    warnings: [
      { warningId: '43001', courseId: '21001', studentId: '5', warningType: 'PROGRESS_LAG', warningLevel: 'MEDIUM', warningStatus: 'OPEN', summary: '近两周学习进度落后班级平均 35%。', suggestion: '建议安排一次章节复盘，并提醒学生按周完成课时。', generatedAt: daysFromNow(-2), evidences: [{ evidenceId: '44001', evidenceType: 'METRIC', metricCode: 'PROGRESS_GAP', metricValue: 35, description: '课程进度 20%，班级平均 55%。' }], version: 0 },
      { warningId: '43002', courseId: '21001', studentId: '6', warningType: 'MISSING_ASSIGNMENT', warningLevel: 'HIGH', warningStatus: 'OPEN', summary: '连续缺交 2 次作业。', suggestion: '优先联系学生确认情况，必要时安排补交。', generatedAt: daysFromNow(-1), evidences: [{ evidenceId: '44002', evidenceType: 'ASSIGNMENT', sourceId: '31001', description: '「第一章课后练习」未提交。' }, { evidenceId: '44003', evidenceType: 'ASSIGNMENT', sourceId: '31002', description: '「第二章编程作业」未提交。' }], version: 0 },
      { warningId: '43003', courseId: '21002', studentId: '4', warningType: 'LOW_SCORE', warningLevel: 'LOW', warningStatus: 'HANDLED', summary: '一次测验得分低于 60 分。', generatedAt: daysFromNow(-9), handledBy: '2', handleRemark: '已沟通，学生将参加助教答疑。', handledAt: daysFromNow(-7), evidences: [{ evidenceId: '44004', evidenceType: 'METRIC', metricCode: 'SCORE', metricValue: 52, description: '随堂测验得分 52。' }], version: 1 },
    ],
    questionBanks: [
      { bankId: '51001', courseId: '21001', name: 'Python 基础题库', description: '覆盖第一、二章知识点。', status: 'ACTIVE', source: 'HUMAN', version: 0 },
      { bankId: '51002', courseId: '21002', name: 'AI 导论题库', status: 'ACTIVE', source: 'HUMAN', version: 0 },
      { bankId: '51003', courseId: '21001', name: 'AI 生成·第二章测验（草稿）', description: 'AI 依据课程资料自动生成，待教师确认。', status: 'DRAFT', source: 'AI', version: 0 },
    ],
    questions: [
      { questionId: '52001', bankId: '51001', courseId: '21001', questionType: 'SINGLE_CHOICE', stem: '下列哪种类型在 Python 中是不可变的？', analysis: '元组创建后不可修改。', difficulty: 'EASY', score: 5, status: 'ACTIVE', options: [{ label: 'A', content: '列表', correct: false, sortOrder: 1 }, { label: 'B', content: '元组', correct: true, sortOrder: 2 }, { label: 'C', content: '字典', correct: false, sortOrder: 3 }, { label: 'D', content: '集合', correct: false, sortOrder: 4 }], version: 0 },
      { questionId: '52002', bankId: '51001', courseId: '21001', questionType: 'TRUE_FALSE', stem: '函数默认参数在每次调用时重新求值。', analysis: '默认参数在函数定义时求值一次。', difficulty: 'MEDIUM', score: 5, status: 'ACTIVE', options: [{ label: 'A', content: '正确', correct: false, sortOrder: 1 }, { label: 'B', content: '错误', correct: true, sortOrder: 2 }], version: 0 },
      { questionId: '52003', bankId: '51001', courseId: '21001', questionType: 'SHORT_ANSWER', stem: '简述列表推导式的优点与需要注意的场景。', difficulty: 'MEDIUM', score: 10, status: 'ACTIVE', options: [], version: 0 },
      { questionId: '52004', bankId: '51002', courseId: '21002', questionType: 'MULTIPLE_CHOICE', stem: '下列属于机器学习范式的有？', difficulty: 'MEDIUM', score: 5, status: 'ACTIVE', options: [{ label: 'A', content: '监督学习', correct: true, sortOrder: 1 }, { label: 'B', content: '无监督学习', correct: true, sortOrder: 2 }, { label: 'C', content: '编译原理', correct: false, sortOrder: 3 }, { label: 'D', content: '强化学习', correct: true, sortOrder: 4 }], version: 0 },
    ],
    exams: [
      { examId: '53001', courseId: '21001', title: 'Python 期中测验', description: '覆盖第一、二章。', status: 'PUBLISHED', startAt: daysFromNow(-1), endAt: daysFromNow(7), durationMinutes: 60, totalScore: 20, source: 'HUMAN', version: 1 },
      { examId: '53002', courseId: '21002', title: 'AI 导论期末考试', status: 'DRAFT', startAt: daysFromNow(30), endAt: daysFromNow(31), durationMinutes: 120, totalScore: 100, source: 'AI', version: 0 },
    ],
    papers: [
      { paperId: '54001', examId: '53001', courseId: '21001', title: '期中测验 A 卷', totalScore: 20, status: 'PUBLISHED', questions: [{ questionId: '52001', questionOrder: 1, score: 5 }, { questionId: '52002', questionOrder: 2, score: 5 }, { questionId: '52003', questionOrder: 3, score: 10 }], version: 1 },
    ],
    attempts: [
      { attemptId: '55001', examId: '53001', paperId: '54001', studentId: '5', status: 'SUBMITTED', startedAt: daysFromNow(-0.5), deadlineAt: daysFromNow(0.5), submittedAt: daysFromNow(-0.4), answers: [{ questionId: '52001', answerContent: 'B', score: 5 }, { questionId: '52002', answerContent: 'B', score: 5 }, { questionId: '52003', answerContent: '列表推导式简洁高效，但嵌套过深会降低可读性。' }], version: 1 },
    ],
    announcements: [
      { announcementId: '61001', scopeType: 'SYSTEM', title: '教学平台暑期维护通知', content: '平台将于 7 月 20 日 02:00-04:00 进行维护，期间暂停访问。', audience: 'ALL', status: 'PUBLISHED', publishedAt: daysFromNow(-4), publisherId: '1', source: 'HUMAN', version: 0 },
      { announcementId: '61002', scopeType: 'COURSE', courseId: '21001', title: '第一章作业已发布', content: '请在截止时间前完成「第一章课后练习」。', audience: 'STUDENT', status: 'PUBLISHED', publishedAt: daysFromNow(-20), publisherId: '2', source: 'HUMAN', version: 0 },
      { announcementId: '61003', scopeType: 'SYSTEM', title: '新学期教师培训报名', content: '面向全体教师的教学平台使用培训，欢迎报名。', audience: 'TEACHER', status: 'PUBLISHED', publishedAt: daysFromNow(-6), publisherId: '1', source: 'HUMAN', version: 0 },
      { announcementId: '61004', scopeType: 'COURSE', courseId: '21001', title: 'AI 生成·课程复习提醒', content: '请结合第二章资料完成本周复习。', audience: 'STUDENT', status: 'DRAFT', publisherId: '2', source: 'AI', version: 0 },
    ],
    notificationReads: [],
    notificationArchives: [],
    notificationPreferences: [],
    termEnrollmentWindows: [
      { windowId: '26001', term: '2026 秋季', enrollmentOpenAt: daysFromNow(-5), enrollmentCloseAt: daysFromNow(25), version: 0 },
    ],
  }
}

function load(): DemoDb {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const stored = JSON.parse(raw) as DemoDb
      stored.notificationReads ??= []
      stored.notificationArchives ??= []
      stored.notificationPreferences ??= []
      return stored
    }
  } catch {
    // 存储损坏时回到种子数据
  }
  return seed()
}

export const db: DemoDb = load()

export function persist(): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
}

/** 恢复演示数据到种子状态。 */
export function resetDemoData(): void {
  Object.assign(db, seed())
  persist()
}

export function nextId(): string {
  const id = String(db.seq)
  db.seq += 1
  return id
}

const labels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线',
  CLOSED: '已截止',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  OPEN: '开放中',
  UPCOMING: '未开放',
  NOT_SUBMITTED: '未提交',
  SUBMITTED: '已提交',
  RETURNED: '已退回',
  GRADED: '已评分',
  ENROLLED: '已选课',
  WITHDRAWN: '已退课',
  NOT_ENROLLED: '未选课',
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  VISIBLE: '可见',
  HIDDEN: '已隐藏',
  HANDLED: '已处理',
  IGNORED: '已忽略',
  ACTIVE: '启用',
  ARCHIVED: '已归档',
  ENABLED: '启用',
  DISABLED: '停用',
  TEXT: '图文',
  VIDEO: '视频',
  MIXED: '图文视频',
  DOCUMENT: '文档',
  LINK: '链接',
  IMMEDIATE: '立即解锁',
  SCHEDULED: '定时解锁',
  SEQUENTIAL: '顺序解锁',
  PUBLIC: '公开',
  ENROLLED_ONLY: '选课可见',
  OWNER: '主讲教师',
  COLLABORATOR: '协作教师',
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  SHORT_ANSWER: '简答题',
  EASY: '易',
  MEDIUM: '中',
  HARD: '难',
}

export function cl(code: string): CodeLabel {
  return { code, label: labels[code] ?? code }
}

export function paginate<T>(records: T[], query?: { page?: number; size?: number }): PageResponse<T> {
  const page = Math.max(1, query?.page ?? 1)
  const size = Math.min(100, Math.max(1, query?.size ?? 20))
  const start = (page - 1) * size
  return {
    records: records.slice(start, start + size),
    page,
    size,
    total: records.length,
    totalPages: Math.ceil(records.length / size),
  }
}

export function notFound(message = '资源不存在或已不可访问。'): never {
  throw new RuntimeError(message, undefined, 'RESOURCE_NOT_FOUND')
}

export function conflict(message: string, code = 'RESOURCE_CONFLICT'): never {
  throw new RuntimeError(message, undefined, code)
}

export function badRequest(message: string): never {
  throw new RuntimeError(message, undefined, 'PARAM_VALIDATION_ERROR')
}

/** 乐观锁校验：版本不一致时按契约返回 409 RESOURCE_CONFLICT。 */
export function assertVersion(row: { version: number }, version: number): void {
  if (row.version !== version) conflict('数据已被其他操作更新，请刷新后重试。')
}

export function userName(userId?: string | null): string {
  return db.users.find((user) => user.userId === userId)?.displayName ?? '未知用户'
}

/**
 * 演示模式的“当前用户”：优先取演示登录的会话用户；未登录时按角色回落到种子账号，
 * 保证三端页面在不走认证接口的情况下也能演示。
 */
export function currentUser(role: 'STUDENT' | 'TEACHER' | 'ADMIN'): UserRow {
  const sessionUser = db.users.find((user) => user.userId === db.session.userId)
  if (sessionUser && sessionUser.roles.includes(role)) return sessionUser
  const fallbackId = role === 'STUDENT' ? '4' : role === 'TEACHER' ? '2' : '1'
  return db.users.find((user) => user.userId === fallbackId) as UserRow
}
