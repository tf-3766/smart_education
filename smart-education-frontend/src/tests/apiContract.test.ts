import { beforeEach, describe, expect, it } from 'vitest'
import {
  adminStatisticsApi,
  adminUsersApi,
  announcementsApi,
  assignmentsApi,
  authApi,
  courseContentApi,
  courseReviewsApi,
  examsApi,
  forumApi,
  resetDemoData,
  studentLearningApi,
  teacherCoursesApi,
  warningsApi,
  aiApi,
} from '@/services/api'
import { RuntimeError } from '@/services/runtime'

async function expectCode(promise: Promise<unknown>, code: string) {
  const error = await promise.then(() => null).catch((caught: unknown) => caught)
  expect(error).toBeInstanceOf(RuntimeError)
  expect((error as RuntimeError).code).toBe(code)
}

describe('契约 API（演示模式）', () => {
  beforeEach(() => {
    localStorage.clear()
    resetDemoData()
  })

  it('登录成功返回令牌并写入存储；密码错误与待审核教师被拒绝', async () => {
    const vo = await authApi.login({ username: 'teacher', password: '123456' })
    expect(vo.user.displayName).toBe('李明')
    expect(sessionStorage.getItem('smart-education-token')).toBe(vo.accessToken)
    await expectCode(authApi.login({ username: 'student', password: 'wrong' }), 'PARAM_VALIDATION_ERROR')
    await expectCode(authApi.login({ username: 'teacher.gao', password: '123456' }), 'TEACHER_REGISTRATION_NOT_PENDING')
  })

  it('学生注册立即可登录，教师注册进入待审核并可被批准', async () => {
    const student = await authApi.register({ username: 'new.student', password: 'Student2026', displayName: '新学生', role: 'STUDENT' })
    expect(student.userStatus).toBe('ENABLED')
    expect(student.login?.accessToken).toBeTruthy()
    const teacher = await authApi.register({ username: 'new.teacher', password: 'Teacher2026', displayName: '新教师', role: 'TEACHER' })
    expect(teacher.approvalRequired).toBe(true)
    expect(teacher.login).toBeNull()
    const approved = await adminUsersApi.approveTeacher(teacher.userId)
    expect(approved.userStatus).toBe('ENABLED')
  })

  it('课程走完 创建→提交审核→驳回（必填原因）→再审核→通过→发布', async () => {
    const course = await teacherCoursesApi.create({ courseCode: 'NEW101', name: '新课程' })
    expect(course.status.code).toBe('DRAFT')
    await teacherCoursesApi.submitReview(course.courseId)
    await expectCode(courseReviewsApi.reject(course.courseId, { reason: '' }), 'PARAM_VALIDATION_ERROR')
    await courseReviewsApi.reject(course.courseId, { reason: '缺少课程大纲' })
    const rejected = await teacherCoursesApi.getDetail(course.courseId)
    expect(rejected.reviewStatus.code).toBe('REJECTED')
    expect(rejected.latestReviewReason).toBe('缺少课程大纲')
    await expectCode(teacherCoursesApi.publish(course.courseId), 'OPERATION_NOT_ALLOWED')
    await teacherCoursesApi.submitReview(course.courseId)
    await courseReviewsApi.approve(course.courseId, { remark: '已补充' })
    const published = await teacherCoursesApi.publish(course.courseId)
    expect(published.status.code).toBe('PUBLISHED')
    const detail = await courseReviewsApi.detail(course.courseId)
    expect(detail.history).toHaveLength(2)
  })

  it('乐观锁：过期 version 更新章节返回 RESOURCE_CONFLICT', async () => {
    const chapter = await courseContentApi.createChapter('21001', { title: '新章节', sortOrder: 9 })
    await courseContentApi.updateChapter(chapter.chapterId, { title: '新章节 v2', sortOrder: 9, version: chapter.version })
    await expectCode(
      courseContentApi.updateChapter(chapter.chapterId, { title: '新章节 v3', sortOrder: 9, version: chapter.version }),
      'RESOURCE_CONFLICT',
    )
  })

  it('学生选课幂等约束与学习进度联动', async () => {
    // 与真实后端一致：重复选课幂等返回既有选课记录
    const repeat = await studentLearningApi.enroll('21001')
    expect(repeat.status.code).toBe('ENROLLED')
    const before = await studentLearningApi.progress('21001')
    await studentLearningApi.completeLesson('23002')
    const after = await studentLearningApi.progress('21001')
    expect(after.completedLessons).toBe(before.completedLessons + 1)
    expect(after.progressPercent).toBeGreaterThan(before.progressPercent)
    const outline = await studentLearningApi.outline('21001')
    const lesson = outline.chapters.flatMap((chapter) => chapter.lessons).find((item) => item.lessonId === '23002')
    expect(lesson?.completed).toBe(true)
  })

  it('作业：空提交被拒、提交后不可重复、批改发布后进入学生成绩单', async () => {
    await expectCode(assignmentsApi.submit('31002', {}), 'PARAM_VALIDATION_ERROR')
    const submission = await assignmentsApi.submit('31002', { content: '第二章作业已完成。' })
    expect(submission.submissionStatus.code).toBe('SUBMITTED')
    await expectCode(assignmentsApi.submit('31002', { content: '重复提交' }), 'RESOURCE_CONFLICT')
    const graded = await assignmentsApi.grade(submission.submissionId, { score: 92, maxScore: 100, teacherComment: '完成得很好。', version: submission.version })
    expect(graded.gradeStatus?.code).toBe('DRAFT')
    const published = await assignmentsApi.publishGrade(graded.gradeId as string, { version: graded.gradeVersion as number })
    expect(published.gradeStatus?.code).toBe('PUBLISHED')
    const grades = await assignmentsApi.studentGrades({})
    expect(grades.records.some((record) => record.assignmentId === '31002' && record.score === 92)).toBe(true)
  })

  it('考试：开始答题幂等，客观题自动判分，简答题经教师评分后合计', async () => {
    const attempt = await examsApi.startAttempt('53001')
    const again = await examsApi.startAttempt('53001')
    expect(again.attemptId).toBe(attempt.attemptId)
    const submitted = await examsApi.submitAttempt(attempt.attemptId, {
      answers: [
        { questionId: '52001', answerContent: 'B' },
        { questionId: '52002', answerContent: 'A' },
        { questionId: '52003', answerContent: '列表推导式简洁，但嵌套过深影响可读性。' },
      ],
      version: attempt.version,
    })
    expect(submitted.status).toBe('SUBMITTED')
    expect(submitted.answers.find((answer) => answer.questionId === '52001')?.score).toBe(5)
    expect(submitted.answers.find((answer) => answer.questionId === '52002')?.score).toBe(0)
    const gradedAttempt = await examsApi.gradeAttempt(attempt.attemptId, { answers: [{ questionId: '52003', score: 8 }], version: submitted.version })
    expect(gradedAttempt.status).toBe('GRADED')
    expect(gradedAttempt.score).toBe(13)
  })

  it('公告按受众过滤，撤回后不再对外可见', async () => {
    const created = await announcementsApi.adminCreate({ title: '仅教师可见', content: '教师会议通知', audience: 'TEACHER' })
    const studentFeed = await announcementsApi.studentList({})
    expect(studentFeed.records.some((record) => record.announcementId === created.announcementId)).toBe(false)
    const teacherFeed = await announcementsApi.teacherList({})
    expect(teacherFeed.records.some((record) => record.announcementId === created.announcementId)).toBe(true)
    await announcementsApi.adminWithdraw(created.announcementId, { version: created.version })
    const teacherFeedAfter = await announcementsApi.teacherList({})
    expect(teacherFeedAfter.records.some((record) => record.announcementId === created.announcementId)).toBe(false)
  })

  it('预警按缺交规则生成、跳过已有预警，并可人工处理', async () => {
    const preview = await warningsApi.generate('21001', { warningTypes: ['MISSING_ASSIGNMENT'], dryRun: true })
    expect(preview.createdCount).toBe(0)
    expect(preview.warnings.length).toBeGreaterThan(0)
    expect(preview.warnings.every((warning) => warning.studentId !== '6')).toBe(true)
    const result = await warningsApi.generate('21001', { warningTypes: ['MISSING_ASSIGNMENT'] })
    expect(result.createdCount).toBe(preview.warnings.length)
    const target = result.warnings[0]
    const handled = await warningsApi.handle(target.warningId, { action: 'HANDLED', remark: '已电话沟通。', version: target.version })
    expect(handled.warningStatus.code).toBe('HANDLED')
    expect(handled.handleRemark).toBe('已电话沟通。')
  })

  it('论坛发帖回帖与隐藏治理', async () => {
    const topic = await forumApi.createTopic('21001', { title: '如何调试递归函数？', content: '有没有推荐的方法？' })
    const reply = await forumApi.createReply(topic.topicId, { content: '可以画调用树并打印每层入参。' })
    expect(reply.status.code).toBe('VISIBLE')
    const hidden = await forumApi.teacherTopicVisibility(topic.topicId, { visible: false, reason: '与课程无关', version: topic.version })
    expect(hidden.status.code).toBe('HIDDEN')
    const list = await forumApi.studentTopics('21001', {})
    expect(list.records.some((record) => record.topicId === topic.topicId)).toBe(false)
  })

  it('AI 问答流按 meta/delta/citation/done 顺序产出且附引用', async () => {
    const events: string[] = []
    let text = ''
    await aiApi.qaStream('21001', { question: '元组和列表的区别？', lessonId: '23001' }, (event) => {
      events.push(event.type)
      if (event.type === 'delta') text += String(event.data)
    })
    expect(events[0]).toBe('meta')
    expect(events[events.length - 1]).toBe('done')
    expect(events).toContain('citation')
    expect(text).toContain('元组和列表的区别')
  })

  it('管理统计聚合演示数据', async () => {
    const stats = await adminStatisticsApi.get()
    expect(stats.totalUsers).toBeGreaterThan(0)
    expect(stats.pendingCourseReviews).toBe(1)
    expect(stats.publishedCourses).toBe(3)
  })
})
