// 与 api-reference.md 一一对应的前后端契约类型。
// 响应中的业务 ID 一律为字符串；时间为 RFC 3339 字符串；请求体中标注 number 的 ID 按文档传数字。

export interface FieldError {
  field: string
  reason: string
  rejectedValue?: string
}

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  errors: FieldError[]
  traceId: string
  timestamp: string
}

export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface CodeLabel {
  code: string
  label: string
}

// —— 3.1 认证 ——

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName: string
  role: 'STUDENT' | 'TEACHER'
}

export interface CurrentUserVO {
  userId: string
  username: string
  displayName: string
  avatarFileId?: string | null
  avatarUrl?: string | null
  activeRole: string
  roles: string[]
  permissions: string[]
  version: number
}

export interface LoginVO {
  accessToken: string
  tokenType: string
  expiresIn: number
  expiresAt: string
  user: CurrentUserVO
  roles: string[]
  permissions: string[]
}

export interface RegistrationVO {
  userId: string
  username: string
  displayName: string
  role: string
  userStatus: 'ENABLED' | 'PENDING'
  approvalRequired: boolean
  login?: LoginVO | null
}

export interface LogoutVO {
  mode: string
  serverSideRevoked: boolean
}

export interface UpdateAvatarRequest {
  // 文件 ID 为雪花号，字符串直传避免精度丢失（后端 Jackson 可将字符串强转 Long）。
  fileId?: string | number | null
  version: number
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

// —— 3.2 文件 ——

export type FilePurpose = 'AVATAR' | 'COURSE_MATERIAL' | 'ASSIGNMENT_ATTACHMENT' | 'SUBMISSION' | 'GENERAL'

export interface FileTextPreviewVO {
  text: string
  status: 'EXTRACTED' | 'TRUNCATED' | 'EMPTY' | 'OCR_UNAVAILABLE' | 'FAILED' | 'NO_FILE' | string
  message: string
  truncated: boolean
}

export interface StoredFileVO {
  fileId: string
  originalName: string
  objectKey: string
  accessUrl: string
  fileSize: number
  mimeType: string
  sha256: string
  purpose: FilePurpose
  uploadedAt: string
  version: number
}

// —— 3.3/3.4 课程与内容 ——

export interface CreateCourseRequest {
  courseCode: string
  name: string
  summary?: string | null
  coverUrl?: string | null
  // 分类为雪花 ID，用字符串透传避免 Number() 丢精度（编辑时会静默改错所属分类）。
  categoryId?: string | null
  term?: string | null
  department?: string | null
  credit?: number | null
  enrollmentOpenAt?: string | null
  enrollmentCloseAt?: string | null
  startAt?: string | null
  endAt?: string | null
}

export interface UpdateCourseRequest extends Omit<CreateCourseRequest, 'courseCode'> {
  version: number
}

export interface CourseDetailVO {
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
  ownerTeacherName: string
  status: CodeLabel
  reviewStatus: CodeLabel
  enrollmentOpenAt?: string | null
  enrollmentCloseAt?: string | null
  startAt?: string | null
  endAt?: string | null
  latestReviewReason?: string | null
  version: number
}

export interface TeacherCourseListItemVO {
  courseId: string
  courseCode: string
  name: string
  term?: string | null
  ownerTeacherId: string
  ownerTeacherName: string
  status: CodeLabel
  reviewStatus: CodeLabel
  startAt?: string | null
  endAt?: string | null
  updatedAt: string
}

export interface StudentCourseListItemVO {
  courseId: string
  courseCode: string
  name: string
  summary?: string | null
  coverUrl?: string | null
  term?: string | null
  credit?: number | null
  ownerTeacherName: string
  status: CodeLabel
  enrollmentStatus: CodeLabel | null
  enrollable: boolean
  startAt?: string | null
  endAt?: string | null
}

export interface AddCourseTeacherRequest {
  // 教师用户 ID 为雪花 ID，用字符串透传避免 Number() 丢精度。
  teacherId: string
}

export interface CourseTeacherVO {
  relationId: string
  courseId: string
  teacherId: string
  teacherName: string
  role: CodeLabel
  // 成员状态：ACTIVE 已加入 / PENDING 待确认（被邀请未接受）。
  status: CodeLabel
  version: number
}

/** 我收到的协作邀请（待本人接受/拒绝）。 */
export interface CollabInvitationVO {
  courseId: string
  courseName: string
  inviterId: string
  inviterName: string
  invitedAt?: string | null
}

export interface CreateChapterRequest {
  title: string
  description?: string | null
  sortOrder: number
}

export interface UpdateChapterRequest extends CreateChapterRequest {
  version: number
}

export interface ChapterDetailVO {
  chapterId: string
  courseId: string
  title: string
  description?: string | null
  sortOrder: number
  status: CodeLabel
  publishedAt?: string | null
  version: number
}

export interface CreateLessonRequest {
  courseId?: number | null
  title: string
  contentType: string
  content?: string | null
  videoUrl?: string | null
  estimatedMinutes?: number | null
  sortOrder: number
  unlockType: string
  unlockAt?: string | null
}

export interface UpdateLessonRequest extends CreateLessonRequest {
  version: number
}

export interface LessonDetailVO {
  lessonId: string
  courseId: string
  chapterId: string
  title: string
  contentType: CodeLabel
  content?: string | null
  videoUrl?: string | null
  estimatedMinutes?: number | null
  sortOrder: number
  status: CodeLabel
  unlockType: CodeLabel
  unlockAt?: string | null
  publishedAt?: string | null
  version: number
}

export interface CreateCourseMaterialRequest {
  // 章节/课时/文件 ID 均为后端雪花 ID（19 位），超出 JS 安全整数范围，必须以字符串透传，
  // 否则 Number() 会丢精度导致后端按错误 ID 查库（表现为上传成功却 FILE_NOT_FOUND）。
  chapterId?: string | null
  lessonId?: string | null
  name: string
  materialType: string
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  fileSize?: number | null
  mimeType?: string | null
  visibility: string
  status?: string | null
  sortOrder: number
}

export interface UpdateCourseMaterialRequest extends Omit<CreateCourseMaterialRequest, 'status'> {
  status: string
  version: number
}

export interface CourseMaterialVO {
  materialId: string
  courseId: string
  chapterId?: string | null
  lessonId?: string | null
  name: string
  materialType: CodeLabel
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  fileSize?: number | null
  mimeType?: string | null
  visibility: CodeLabel
  status: CodeLabel
  sortOrder: number
  version: number
}

// —— 3.5 学生课程与学习 ——

export interface EnrollmentVO {
  enrollmentId: string
  courseId: string
  studentId: string
  status: CodeLabel
  enrolledAt?: string | null
  withdrawnAt?: string | null
  version: number
}

export interface LessonOutlineVO {
  lessonId: string
  title: string
  sortOrder: number
  contentType: CodeLabel
  estimatedMinutes?: number | null
  unlocked: boolean
  completed: boolean
  learningStatus: CodeLabel
  materials: MaterialAccessVO[]
}

export interface ChapterOutlineVO {
  chapterId: string
  title: string
  sortOrder: number
  lessons: LessonOutlineVO[]
}

export interface CourseOutlineVO {
  courseId: string
  courseName: string
  status: CodeLabel
  chapters: ChapterOutlineVO[]
}

export interface LearningRecordVO {
  recordId: string
  courseId: string
  chapterId: string
  lessonId: string
  studentId: string
  status: CodeLabel
  studySeconds: number
  startedAt?: string | null
  completedAt?: string | null
  lastStudiedAt?: string | null
}

export interface StudentLessonDetailVO {
  lessonId: string
  courseId: string
  chapterId: string
  title: string
  contentType: CodeLabel
  content?: string | null
  videoUrl?: string | null
  estimatedMinutes?: number | null
  status: CodeLabel
  unlockAt?: string | null
  materials: MaterialAccessVO[]
  learningRecord: LearningRecordVO | null
}

export interface CourseProgressVO {
  courseId: string
  totalLessons: number
  availableLessons: number
  completedLessons: number
  progressPercent: number
  lastLessonId?: string | null
  nextLessonId?: string | null
}

export interface MaterialAccessVO {
  materialId: string
  name: string
  materialType: CodeLabel
  fileSize?: number | null
  mimeType?: string | null
  accessMode: string
  accessUrl: string
}

// —— 3.6 课程审核 ——

export interface ReviewCourseRequest {
  remark?: string | null
}

export interface RejectCourseRequest {
  reason: string
}

export interface CourseReviewListItemVO {
  courseId: string
  courseCode: string
  name: string
  ownerTeacherId: string
  ownerTeacherName: string
  term?: string | null
  courseStatus: CodeLabel
  reviewStatus: CodeLabel
  updatedAt: string
}

export interface CourseReviewVO {
  reviewId: string
  courseId: string
  reviewStatus: CodeLabel
  reviewerId: string
  reviewerName: string
  reason?: string | null
  remark?: string | null
  reviewedAt: string
}

export interface CourseReviewDetailVO {
  course: CourseDetailVO
  history: CourseReviewVO[]
}

// —— 3.7 超级管理员用户授权 ——

export interface AdminUserVO {
  userId: string
  username: string
  displayName: string
  avatarFileId?: string | null
  userStatus: 'PENDING' | 'ENABLED' | 'DISABLED' | 'REJECTED'
  roles: string[]
  superAdministrator: boolean
  createdAt: string
  version: number
}

// —— 查询参数 ——

export interface PageQuery {
  page?: number
  size?: number
}

export interface AdminUserQuery extends PageQuery {
  keyword?: string
  status?: string
}

export interface CourseListQuery extends PageQuery {
  keyword?: string
  status?: string
  reviewStatus?: string
  enrollmentStatus?: string
  formalOnly?: boolean
  term?: string
  categoryId?: number
  sort?: string
}

export interface CourseMaterialListQuery extends PageQuery {
  keyword?: string
  status?: string
  visibility?: string
}

// —— 4.1 作业、提交与成绩 ——

export interface AssignmentAttachmentRequest {
  name: string
  fileId?: number | null
  fileKey?: string | null
  fileUrl?: string | null
  fileSize?: number | null
  mimeType?: string | null
  sortOrder: number
}

export type AssignmentResponseMode = 'MIXED' | 'TEXT' | 'CODE' | 'QUIZ'

export type AssignmentQuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER'

export interface AssignmentQuestion {
  questionId: string
  questionType: AssignmentQuestionType
  stem: string
  options: string[]
  score: number
  correctAnswers: string[]
}

export interface AssignmentCreateRequest {
  lessonId?: string | null
  title: string
  description?: string | null
  responseMode?: AssignmentResponseMode
  questions?: AssignmentQuestion[]
  maxScore: number
  openAt?: string | null
  dueAt: string
  attachments?: AssignmentAttachmentRequest[]
}

export interface AssignmentUpdateRequest extends AssignmentCreateRequest {
  version: number
}

export interface AssignmentAttachmentVO {
  attachmentId: string
  name: string
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  fileSize?: number | null
  mimeType?: string | null
  sortOrder: number
}

export interface AssignmentDetailVO {
  assignmentId: string
  courseId: string
  lessonId?: string | null
  title: string
  description?: string | null
  responseMode?: AssignmentResponseMode
  questions?: AssignmentQuestion[]
  maxScore: number
  assignmentStatus: CodeLabel
  availabilityStatus: CodeLabel
  openAt?: string | null
  dueAt: string
  publishedAt?: string | null
  attachments: AssignmentAttachmentVO[]
  source: 'AI' | 'HUMAN'
  version: number
}

export interface StudentAssignmentListItemVO {
  assignmentId: string
  courseId: string
  lessonId?: string | null
  title: string
  maxScore: number
  availabilityStatus: CodeLabel
  dueAt: string
  submissionStatus?: CodeLabel | null
  submittedAt?: string | null
  graded: boolean
}

export interface SubmissionSaveRequest {
  content?: string | null
  answers?: Record<string, string[]>
  fileId?: string | number | null
  fileKey?: string | null
  fileUrl?: string | null
  version?: number | null
}

export type SubmissionSubmitRequest = SubmissionSaveRequest

export interface SubmissionDetailVO {
  submissionId: string
  assignmentId: string
  courseId: string
  studentId: string
  attemptNo: number
  content?: string | null
  answers: Record<string, string[]>
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  submissionStatus: CodeLabel
  submittedAt?: string | null
  score?: number | null
  teacherComment?: string | null
  aiCommentDraftId?: string | null
  gradedBy?: string | null
  gradedAt?: string | null
  publishedAt?: string | null
  version: number
}

export interface StudentAssignmentDetailVO {
  assignment: AssignmentDetailVO
  submission?: SubmissionDetailVO | null
}

export interface GradeSubmissionRequest {
  score: number
  maxScore: number
  teacherComment?: string | null
  aiCommentDraftId?: string | null
  publishNow?: boolean
  version: number
}

export interface PublishGradeRequest {
  version: number
}

export interface TeacherSubmissionGradeVO {
  submissionId: string
  assignmentId: string
  courseId: string
  studentId: string
  studentName?: string | null
  submissionStatus: CodeLabel
  submittedAt?: string | null
  content?: string | null
  answers: Record<string, string[]>
  fileId?: string | null
  fileKey?: string | null
  fileUrl?: string | null
  score?: number | null
  maxScore: number
  teacherComment?: string | null
  aiCommentDraftId?: string | null
  gradedBy?: string | null
  gradedAt?: string | null
  gradeId?: string | null
  gradeStatus?: CodeLabel | null
  publishedAt?: string | null
  version: number
  gradeVersion?: number | null
}

export interface TeacherSubmissionRosterVO {
  studentId: string
  studentName?: string | null
  submitted: boolean
  submission?: TeacherSubmissionGradeVO | null
}
export interface StudentGradeVO {
  gradeId: string
  courseId: string
  assignmentId: string
  assignmentTitle: string
  score: number
  maxScore: number
  scoreRate: number
  teacherComment?: string | null
  publishedAt: string
}

export interface GradeListQuery extends PageQuery {
  courseId?: string
  sourceType?: string
  status?: string
}

export interface AssignmentStatisticsVO {
  assignmentId: string
  courseId: string
  totalStudentCount: number
  submittedCount: number
  missingCount: number
  gradedCount: number
  publishedGradeCount: number
  averageScore?: number | null
  lowScoreCount: number
}

export interface CourseGradeStatisticsVO {
  courseId: string
  assignmentCount: number
  publishedAssignmentCount: number
  enrolledStudentCount: number
  gradedRecordCount: number
  publishedGradeCount: number
  averageScoreRate?: number | null
  passRate?: number | null
  lowScoreCount: number
}

// —— 4.2 论坛与预警 ——

export interface ForumTopicCreateRequest {
  title: string
  content: string
}

export interface ForumReplyCreateRequest {
  parentReplyId?: string | null
  content: string
}

export interface ForumVisibilityRequest {
  visible: boolean
  reason?: string | null
  version: number
}

export interface ForumTopicListItemVO {
  topicId: string
  courseId: string
  title: string
  authorId: string
  authorName?: string | null
  authorAvatarFileId?: string | null
  status: CodeLabel
  pinned: boolean
  replyCount: number
  lastRepliedAt?: string | null
  createdAt: string
  version: number
}

export interface ForumTopicDetailVO {
  topicId: string
  courseId: string
  title: string
  content: string
  authorId: string
  authorName?: string | null
  authorAvatarFileId?: string | null
  status: CodeLabel
  moderationReason?: string | null
  moderatedBy?: string | null
  moderatedAt?: string | null
  createdAt: string
  version: number
}

export interface ForumReplyVO {
  replyId: string
  topicId: string
  courseId: string
  authorId: string
  authorName?: string | null
  authorAvatarFileId?: string | null
  parentReplyId?: string | null
  content: string
  status: CodeLabel
  moderationReason?: string | null
  moderatedBy?: string | null
  moderatedAt?: string | null
  createdAt: string
  version: number
}

export interface ForumTopicListQuery extends PageQuery {
  status?: string
  keyword?: string
}

export type WarningType = 'PROGRESS_LAG' | 'MISSING_ASSIGNMENT' | 'LOW_SCORE'

export interface GenerateCourseWarningsRequest {
  warningTypes?: WarningType[]
  studentId?: string | null
  dryRun?: boolean
}

export interface WarningHandleRequest {
  action: 'HANDLED' | 'IGNORED'
  remark?: string | null
  version: number
}

export interface WarningListQuery extends PageQuery {
  courseId?: string
  studentId?: string
  warningType?: string
  warningLevel?: string
  warningStatus?: string
}

export interface WarningEvidenceVO {
  evidenceId: string
  evidenceType: string
  sourceId?: string | null
  metricCode?: string | null
  metricValue?: string | null
  description: string
}

export interface LearningWarningVO {
  warningId: string
  courseId: string
  courseName: string
  teacherName?: string | null
  studentId: string
  studentName?: string | null
  warningType: CodeLabel
  warningLevel: CodeLabel
  warningStatus: CodeLabel
  summary: string
  suggestion?: string | null
  aiExplanationDraftId?: string | null
  generatedAt: string
  handledBy?: string | null
  handleRemark?: string | null
  handledAt?: string | null
  evidences: WarningEvidenceVO[]
  version: number
}

export interface WarningGenerationResultVO {
  createdCount: number
  skippedCount: number
  warnings: LearningWarningVO[]
}

// —— 4.3 题库、考试、公告、分类与统计 ——

export interface QuestionBankListQuery extends PageQuery {
  keyword?: string
  status?: string
}

export interface CreateQuestionBankRequest {
  name: string
  description?: string | null
}

export interface UpdateQuestionBankRequest extends CreateQuestionBankRequest {
  status: 'ACTIVE' | 'ARCHIVED'
  version: number
}

export interface QuestionBankVO {
  bankId: string
  courseId: string
  name: string
  description?: string | null
  status: string
  source: string
  version: number
}

export interface QuestionListQuery extends PageQuery {
  keyword?: string
  questionType?: string
  difficulty?: string
  status?: string
}

export interface QuestionOptionRequest {
  label: string
  content: string
  correct: boolean
  sortOrder: number
}

export interface CreateQuestionRequest {
  questionType: string
  stem: string
  analysis?: string | null
  difficulty: string
  score: number
  options?: QuestionOptionRequest[]
}

export interface UpdateQuestionRequest extends CreateQuestionRequest {
  status?: string | null
  version: number
}

export interface QuestionVO {
  questionId: string
  bankId: string
  courseId: string
  questionType: string
  stem: string
  analysis?: string | null
  difficulty: string
  score: number
  status: string
  options: (QuestionOptionRequest & { optionId?: string })[]
  version: number
}

export interface ExamListQuery extends PageQuery {
  keyword?: string
  status?: string
}

export interface CreateExamRequest {
  title: string
  description?: string | null
  startAt: string
  endAt: string
  durationMinutes: number
  totalScore: number
}

export interface UpdateExamRequest extends CreateExamRequest {
  version: number
}

export interface ExamVO {
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

export interface ExamPaperQuestionRequest {
  questionId: string
  questionOrder: number
  score: number
}

export interface CreateExamPaperRequest {
  title: string
  questions: ExamPaperQuestionRequest[]
}

export interface UpdateExamPaperRequest extends CreateExamPaperRequest {
  version: number
}

export interface ExamPaperVO {
  paperId: string
  examId: string
  courseId: string
  title: string
  totalScore: number
  status: string
  questions: ExamPaperQuestionRequest[]
  version: number
}

export interface StudentExamListItemVO {
  examId: string
  courseId: string
  title: string
  description?: string | null
  startAt: string
  endAt: string
  durationMinutes: number
  totalScore: number
}

export interface ExamAttemptListQuery extends PageQuery {
  status?: 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED'
}

export interface ExamAnswerSubmitRequest {
  questionId: string
  answerContent: string
}

export interface SubmitExamAttemptRequest {
  answers: ExamAnswerSubmitRequest[]
  version: number
}

export interface GradeExamAnswerRequest {
  questionId: string
  score: number
  teacherComment?: string | null
}

export interface GradeExamAttemptRequest {
  answers: GradeExamAnswerRequest[]
  version: number
}

export interface StudentExamOptionVO {
  label: string
  content: string
  sortOrder: number
}

export interface StudentExamQuestionVO {
  questionId: string
  questionOrder: number
  score: number
  questionType: string
  stem: string
  options: StudentExamOptionVO[]
}

export interface ExamAnswerVO {
  questionId: string
  answerContent: string
  score?: number | null
  teacherComment?: string | null
}

export interface ExamAttemptVO {
  attemptId: string
  examId: string
  paperId: string
  studentId: string
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED'
  startedAt: string
  deadlineAt?: string | null
  submittedAt?: string | null
  gradedAt?: string | null
  score?: number | null
  questions: StudentExamQuestionVO[]
  answers: ExamAnswerVO[]
  version: number
}

export type AnnouncementAudience = 'ALL' | 'STUDENT' | 'TEACHER'

export interface CreateAnnouncementRequest {
  title: string
  content: string
  audience: AnnouncementAudience
}

export interface WithdrawAnnouncementRequest {
  version: number
}

export type AnnouncementListQuery = PageQuery

export interface AnnouncementVO {
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

export type NotificationCategory = 'COURSE' | 'ASSIGNMENT' | 'EXAM' | 'WARNING' | 'SYSTEM'

export interface NotificationListQuery extends PageQuery {
  category?: NotificationCategory
  unread?: boolean
}

export interface NotificationVO {
  notificationId: string
  title: string
  content: string
  category: NotificationCategory
  categoryLabel: string
  status: 'PUBLISHED' | 'WITHDRAWN' | 'ARCHIVED'
  sourceType: string
  announcementId?: string | null
  courseId?: string | null
  assignmentId?: string | null
  examId?: string | null
  warningId?: string | null
  createdAt: string
  read: boolean
  readAt?: string | null
}

export interface NotificationPreferencesVO {
  enabledCategories: NotificationCategory[]
}

export interface UpdateNotificationPreferencesRequest {
  enabledCategories: NotificationCategory[]
}

export interface NotificationStreamEvent {
  type: 'connected' | 'heartbeat' | 'created' | 'state-changed' | 'preferences-changed'
  notificationId?: string | null
  timestamp: string
}

export interface CreateCourseCategoryRequest {
  name: string
  sortOrder: number
  enabled: boolean
}

export interface UpdateCourseCategoryRequest extends CreateCourseCategoryRequest {
  version: number
}

export interface CourseCategoryVO {
  categoryId: string
  name: string
  sortOrder: number
  enabled: boolean
  version: number
}

export interface AdminStatisticsVO {
  totalUsers: number
  enabledUsers: number
  students: number
  teachers: number
  administrators: number
  totalCourses: number
  publishedCourses: number
  pendingCourseReviews: number
  activeEnrollments: number
  publishedAssignments: number
  submittedAssignments: number
  publishedExams: number
  openWarnings: number
  publishedAnnouncements: number
}

// —— 4.4 AI ——

export interface AssistantChatRequest {
  question: string
  courseId?: string | null
  lessonId?: string | null
  pagePath?: string | null
  pageTitle?: string | null
  conversationId?: string | null
}
export interface CourseQaRequest {
  question: string
  lessonId?: string | null
  /** 会话 ID：同一会话内连续提问以启用后端「对话记忆」多轮上下文。字符串直传，避免精度问题。 */
  conversationId?: string | null
}

export interface LessonSummaryRequest {
  courseId: string
}

export interface AiCitationVO {
  resourceType: string
  resourceId: string
  title: string
  locator?: string | null
}

export interface AiDraftVO {
  requestId: string
  draftType: string
  businessId: string
  content: string
  provider: string
  model?: string | null
  status: 'DRAFT' | 'FRAMEWORK_ONLY'
  citations: AiCitationVO[]
  createdAt: string
}

export interface BatchGradingDraftRequest {
  submissionIds: string[]
  rubric: string
  reviewThreshold?: number
  instruction?: string | null
}

export interface BatchGradingDraftItemVO {
  submissionId: string
  assignmentId: string
  maxScore: number
  suggestedScore?: number | null
  comment: string
  confidence: number
  reviewRequired: boolean
  anomalyCodes: string[]
  reviewReasons: string[]
  citations: AiCitationVO[]
}

export interface BatchGradingDraftVO {
  requestId: string
  rubric: string
  reviewThreshold: number
  totalCount: number
  reviewCount: number
  status: 'DRAFT' | 'FRAMEWORK_ONLY'
  items: BatchGradingDraftItemVO[]
  createdAt: string
}

export interface AdminGovernanceDraftRequest {
  teacherUserIds?: string[]
  courseIds?: string[]
  criteria?: string | null
}

export interface AdminTeacherReviewItemVO {
  userId: string
  targetVersion: number | null
  username: string | null
  displayName: string | null
  registeredAt: string | null
  candidate: string
  recommendation: 'MANUAL_REVIEW' | 'NOT_ELIGIBLE'
  confidence: number
  reviewRequired: boolean
  riskCodes: string[]
  reasons: string[]
  evidence: string[]
}

export interface AdminCourseComplianceItemVO {
  courseId: string
  targetVersion: number | null
  courseCode: string
  courseName: string
  courseStatus: string
  reviewStatus: string
  summary: string | null
  categoryId: string | null
  term: string | null
  department: string | null
  credit: number | null
  enrollmentOpenAt: string | null
  enrollmentCloseAt: string | null
  startAt: string | null
  endAt: string | null
  lessonCount: number
  materialCount: number
  readinessScore: number
  recommendation: 'READY_FOR_ADMIN_REVIEW' | 'REMEDIATE_AND_REVIEW' | 'UNAVAILABLE'
  failed: boolean
  reviewRequired: boolean
  issueCodes: string[]
  reasons: string[]
  evidence: string[]
}

export interface AdminGovernanceDraftVO {
  requestId: string
  status: 'DRAFT' | 'FRAMEWORK_ONLY'
  totalCount: number
  successCount: number
  failureCount: number
  reviewCount: number
  teacherReviews: AdminTeacherReviewItemVO[]
  courseCompliance: AdminCourseComplianceItemVO[]
  createdAt: string
}

export interface AiStreamEvent {
  // capability：当前实际能力；tool：调用进度；action：结构化业务结果；citation：引用。
  type: 'meta' | 'capability' | 'tool' | 'action' | 'delta' | 'citation' | 'done' | 'error'
  requestId: string
  data: unknown
  timestamp: string
}

/** tool 事件 data 结构：AI 进入模型前/中的检索与工具调用进度。 */
export interface AiToolEvent {
  toolName: string
  status: string
  input?: string | null
  summary?: string | null
  result?: AiCitationVO[] | null
}

export interface AiCapabilityEvent {
  capabilityId: string
  name: string
  description?: string | null
  roles?: string[]
  mode: 'ANSWER' | 'DRAFT' | 'ACTION' | 'BATCH_ACTION'
  riskLevel: 'READ_ONLY' | 'LOW' | 'MEDIUM' | 'HIGH'
  requiredContext?: string[]
  confirmationPolicy?: 'NONE' | 'DRAFT_REVIEW' | 'EXPLICIT_CONFIRM' | 'STRONG_CONFIRM'
  deepLinkTemplate?: string | null
  requiresCourseContext: boolean
  enabled: boolean
  unavailableReason?: string | null
}

export interface AiActionEvent {
  actionId: string
  capabilityId: string
  status: 'PLANNED' | 'WAITING_CONFIRMATION' | 'DRAFT_CREATED' | 'EXECUTING' | 'SUCCEEDED' | 'PARTIAL_SUCCESS' | 'FAILED' | 'CANCELLED' | 'EXPIRED'
  riskLevel?: 'READ_ONLY' | 'LOW' | 'MEDIUM' | 'HIGH' | null
  confirmationPolicy?: 'NONE' | 'DRAFT_REVIEW' | 'EXPLICIT_CONFIRM' | 'STRONG_CONFIRM' | null
  targetType?: string | null
  targetId?: string | null
  targetVersion?: number | null
  resourceType?: string | null
  resourceId?: string | null
  title: string
  summary: string
  preview?: Record<string, string> | null
  href?: string | null
  requiresConfirmation: boolean
  errorCode?: string | null
  errorMessage?: string | null
  expiresAt?: string | null
  confirmedAt?: string | null
  executedAt?: string | null
  createdAt?: string | null
}

export interface AiKnowledgeBaseStatusVO {
  courseId: string
  vectorStoreConfigured: boolean
  indexedChunks: number
  lastSyncedAt?: string | null
}
export interface CourseTemplateVO {
  templateId: string
  courseCode: string
  name: string
  summary: string | null
}

export interface TermEnrollmentWindowVO {
  windowId: string
  term: string
  enrollmentOpenAt?: string | null
  enrollmentCloseAt?: string | null
  version: number
}

/** 教师目录选项：用于协作教师下拉选择。 */
export interface TeacherOptionVO {
  teacherId: string
  teacherName: string
}

export interface UpsertTermEnrollmentWindowRequest {
  term: string
  enrollmentOpenAt?: string | null
  enrollmentCloseAt?: string | null
}

export interface PaperSuggestionRequest {
  courseId: string
  questionCount: number
  totalScore: number
  requirements?: string | null
}

export interface AiServiceStatusVO {
  serviceStatus: string
  framework: string
  frameworkVersion: string
  provider: string
  model: string | null
  modelConfigured: boolean
  vectorStoreConfigured: boolean
  checkedAt: string
}
