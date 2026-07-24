// 3.3 教师课程管理接口
import { demoDelay } from '../runtime'
import { del, get, isRealMode, post, put } from './client'
import { assertVersion, cl, conflict, currentUser, db, nextId, notFound, nowIso, persist, userName } from './demo/db'
import type { CourseRow } from './demo/db'
import type {
  CourseTemplateVO,
  AddCourseTeacherRequest,
  CollabInvitationVO,
  CourseDetailVO,
  CourseListQuery,
  CourseTeacherVO,
  CreateCourseRequest,
  PageResponse,
  TeacherCourseListItemVO,
  TeacherOptionVO,
  TermEnrollmentWindowVO,
  UpdateCourseRequest,
} from './types'
import { paginate } from './demo/db'

export function toCourseDetailVO(row: CourseRow): CourseDetailVO {
  return {
    courseId: row.courseId,
    courseCode: row.courseCode,
    name: row.name,
    summary: row.summary ?? null,
    coverUrl: row.coverUrl ?? null,
    categoryId: row.categoryId ?? null,
    term: row.term ?? null,
    department: row.department ?? null,
    credit: row.credit ?? null,
    ownerTeacherId: row.ownerTeacherId,
    ownerTeacherName: userName(row.ownerTeacherId),
    status: cl(row.status),
    reviewStatus: cl(row.reviewStatus),
    enrollmentOpenAt: row.enrollmentOpenAt ?? null,
    enrollmentCloseAt: row.enrollmentCloseAt ?? null,
    startAt: row.startAt ?? null,
    endAt: row.endAt ?? null,
    latestReviewReason: row.latestReviewReason ?? null,
    version: row.version,
  }
}

function toListItem(row: CourseRow): TeacherCourseListItemVO {
  return {
    courseId: row.courseId,
    courseCode: row.courseCode,
    name: row.name,
    term: row.term ?? null,
    ownerTeacherId: row.ownerTeacherId,
    ownerTeacherName: userName(row.ownerTeacherId),
    status: cl(row.status),
    reviewStatus: cl(row.reviewStatus),
    startAt: row.startAt ?? null,
    endAt: row.endAt ?? null,
    updatedAt: row.updatedAt,
  }
}

// 团队成员状态标签：与后端 CourseTeacherStatus 一致；不用通用 cl()，避免与课程审核 PENDING「待审核」撞标签。
const teacherStatusLabel = (status: string) => (status === 'ACTIVE' ? '已加入' : '待确认')
const teacherStatusCl = (status: string) => ({ code: status, label: teacherStatusLabel(status) })

/** 校验并返回当前教师可管理的课程（须属于课程教师团队）。 */
export function requireTeacherCourse(courseId: string): CourseRow {
  const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
  const teacher = currentUser('TEACHER')
  // 仅正式成员（ACTIVE）可管理课程；被邀请未接受（PENDING）不授予任何权限。
  const inTeam = db.courseTeachers.some((item) => item.courseId === courseId && item.teacherId === teacher.userId && item.status === 'ACTIVE')
  if (!inTeam) conflict('您不在该课程的教师团队中。', 'FORBIDDEN')
  return course
}

function applyCourseFields(row: CourseRow, body: CreateCourseRequest | UpdateCourseRequest): void {
  Object.assign(row, {
    name: body.name,
    summary: body.summary ?? null,
    coverUrl: body.coverUrl ?? null,
    categoryId: body.categoryId == null ? null : String(body.categoryId),
    term: body.term ?? null,
    department: body.department ?? null,
    credit: body.credit ?? null,
    enrollmentOpenAt: body.enrollmentOpenAt ?? null,
    enrollmentCloseAt: body.enrollmentCloseAt ?? null,
    startAt: body.startAt ?? null,
    endAt: body.endAt ?? null,
    updatedAt: nowIso(),
  })
}

export const teacherCoursesApi = {
  /** 内置课程模板（历年课程沉淀），建课时可直接选用。 */
  async templates(): Promise<CourseTemplateVO[]> {
    if (isRealMode()) return get<CourseTemplateVO[]>('/api/v1/teacher/courses/templates')
    return demoDelay([...db.courseTemplates].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')))
  },

  /** 管理员按学期设置的统一选课窗口（教师建课时只读参考，用于自动套用选课时间）。 */
  async termWindows(): Promise<TermEnrollmentWindowVO[]> {
    if (isRealMode()) return get<TermEnrollmentWindowVO[]>('/api/v1/teacher/courses/term-windows')
    return demoDelay([...db.termEnrollmentWindows].sort((a, b) => a.term.localeCompare(b.term)))
  },

  /** 可选协作教师目录（全部启用中的教师），供课程团队下拉选择。 */
  async teacherDirectory(): Promise<TeacherOptionVO[]> {
    if (isRealMode()) return get<TeacherOptionVO[]>('/api/v1/teacher/courses/teacher-directory')
    return demoDelay(db.users
      .filter((user) => user.roles.includes('TEACHER') && user.userStatus === 'ENABLED')
      .map((user) => ({ teacherId: user.userId, teacherName: user.displayName }))
      .sort((a, b) => a.teacherName.localeCompare(b.teacherName, 'zh-CN')))
  },

  async create(body: CreateCourseRequest): Promise<CourseDetailVO> {
    if (isRealMode()) return post<CourseDetailVO>('/api/v1/teacher/courses', body)
    const definition = db.courseTemplates.find((item) => item.courseCode === body.courseCode)
    const teacher = currentUser('TEACHER')
    const row: CourseRow = {
      courseId: nextId(),
      courseCode: definition?.courseCode ?? body.courseCode,
      name: definition?.name ?? body.name,
      ownerTeacherId: teacher.userId,
      status: 'DRAFT',
      reviewStatus: 'NOT_SUBMITTED',
      createdAt: nowIso(),
      updatedAt: nowIso(),
      version: 0,
    }
    applyCourseFields(row, body)
    if (definition) {
      row.name = definition.name
      row.summary = definition.summary ?? null
    }
    db.courses.push(row)
    db.courseTeachers.push({ relationId: nextId(), courseId: row.courseId, teacherId: teacher.userId, role: 'OWNER', status: 'ACTIVE', version: 0 })
    persist()
    return demoDelay(toCourseDetailVO(row))
  },

  async list(query: CourseListQuery = {}): Promise<PageResponse<TeacherCourseListItemVO>> {
    if (isRealMode()) return get<PageResponse<TeacherCourseListItemVO>>('/api/v1/teacher/courses', { ...query })
    const teacher = currentUser('TEACHER')
    // 只列出正式成员（ACTIVE）的课程；被邀请未接受的课程不进本人课程列表。
    const mine = new Set(db.courseTeachers.filter((item) => item.teacherId === teacher.userId && item.status === 'ACTIVE').map((item) => item.courseId))
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.courses
      .filter((row) => mine.has(row.courseId))
      .filter((row) => !query.status || row.status === query.status)
      .filter((row) => !query.reviewStatus || row.reviewStatus === query.reviewStatus)
      .filter((row) => !query.formalOnly || (
        row.reviewStatus === 'APPROVED' && ['PUBLISHED', 'ONGOING', 'FINISHED'].includes(row.status)
      ))
      .filter((row) => !query.term || row.term === query.term)
      .filter((row) => !keyword || row.name.toLowerCase().includes(keyword) || row.courseCode.toLowerCase().includes(keyword))
      .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    return demoDelay(paginate(rows.map(toListItem), query))
  },

  /** 作业、考试、预警和互动等正式业务只允许使用已审核且已经进入教学生命周期的课程。 */
  async listFormal(query: CourseListQuery = {}): Promise<PageResponse<TeacherCourseListItemVO>> {
    return this.list({ ...query, formalOnly: true })
  },

  async getDetail(courseId: string): Promise<CourseDetailVO> {
    if (isRealMode()) return get<CourseDetailVO>(`/api/v1/teacher/courses/${courseId}`)
    return demoDelay(toCourseDetailVO(requireTeacherCourse(courseId)))
  },

  async update(courseId: string, body: UpdateCourseRequest): Promise<CourseDetailVO> {
    if (isRealMode()) return put<CourseDetailVO>(`/api/v1/teacher/courses/${courseId}`, body)
    const row = requireTeacherCourse(courseId)
    assertVersion(row, body.version)
    applyCourseFields(row, body)
    row.version += 1
    persist()
    return demoDelay(toCourseDetailVO(row))
  },

  async deleteDraft(courseId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/courses/${courseId}`)
    const row = requireTeacherCourse(courseId)
    if (row.status !== 'DRAFT' || !['NOT_SUBMITTED', 'REJECTED'].includes(row.reviewStatus)) {
      conflict('仅未提交或已驳回的课程草稿可以删除。', 'OPERATION_NOT_ALLOWED')
    }
    db.courseTeachers = db.courseTeachers.filter((relation) => relation.courseId !== courseId)
    db.courses.splice(db.courses.findIndex((course) => course.courseId === courseId), 1)
    persist()
    return demoDelay(undefined)
  },

  async submitReview(courseId: string): Promise<CourseDetailVO> {
    if (isRealMode()) return post<CourseDetailVO>(`/api/v1/teacher/courses/${courseId}/submit-review`)
    const row = requireTeacherCourse(courseId)
    if (row.reviewStatus === 'PENDING') conflict('课程已在审核队列中。', 'OPERATION_NOT_ALLOWED')
    if (row.status === 'PUBLISHED') conflict('已发布课程无需再次提交审核。', 'OPERATION_NOT_ALLOWED')
    row.reviewStatus = 'PENDING'
    row.updatedAt = nowIso()
    row.version += 1
    persist()
    return demoDelay(toCourseDetailVO(row))
  },

  async publish(courseId: string): Promise<CourseDetailVO> {
    if (isRealMode()) return post<CourseDetailVO>(`/api/v1/teacher/courses/${courseId}/publish`)
    const row = requireTeacherCourse(courseId)
    if (row.reviewStatus !== 'APPROVED') conflict('课程未通过审核，不能发布。', 'OPERATION_NOT_ALLOWED')
    row.status = 'PUBLISHED'
    row.updatedAt = nowIso()
    row.version += 1
    persist()
    return demoDelay(toCourseDetailVO(row))
  },

  async offline(courseId: string): Promise<CourseDetailVO> {
    if (isRealMode()) return post<CourseDetailVO>(`/api/v1/teacher/courses/${courseId}/offline`)
    const row = requireTeacherCourse(courseId)
    if (row.status !== 'PUBLISHED') conflict('仅已发布课程可以下线。', 'OPERATION_NOT_ALLOWED')
    row.status = 'OFFLINE'
    row.updatedAt = nowIso()
    row.version += 1
    persist()
    return demoDelay(toCourseDetailVO(row))
  },

  async listTeachers(courseId: string): Promise<CourseTeacherVO[]> {
    if (isRealMode()) return get<CourseTeacherVO[]>(`/api/v1/teacher/courses/${courseId}/teachers`)
    requireTeacherCourse(courseId)
    return demoDelay(db.courseTeachers
      .filter((item) => item.courseId === courseId)
      .map((item) => ({ relationId: item.relationId, courseId: item.courseId, teacherId: item.teacherId, teacherName: userName(item.teacherId), role: cl(item.role), status: teacherStatusCl(item.status), version: item.version })))
  },

  /** 邀请协作教师：建 PENDING 记录，需被邀请人接受后才生效。 */
  async addTeacher(courseId: string, body: AddCourseTeacherRequest): Promise<CourseTeacherVO> {
    if (isRealMode()) return post<CourseTeacherVO>(`/api/v1/teacher/courses/${courseId}/teachers`, body)
    requireTeacherCourse(courseId)
    const teacherId = String(body.teacherId)
    if (teacherId === currentUser('TEACHER').userId) conflict('无需邀请课程负责人本人。', 'OPERATION_NOT_ALLOWED')
    const target = db.users.find((user) => user.userId === teacherId && user.roles.includes('TEACHER') && user.userStatus === 'ENABLED')
    if (!target) notFound('教师不存在或不可用。')
    const existing = db.courseTeachers.find((item) => item.courseId === courseId && item.teacherId === teacherId)
    if (existing) conflict(existing.status === 'PENDING' ? '已向该教师发出邀请，等待对方确认。' : '该教师已在课程团队中。')
    const row = { relationId: nextId(), courseId, teacherId, role: 'COLLABORATOR', status: 'PENDING', version: 0 }
    db.courseTeachers.push(row)
    persist()
    return demoDelay({ ...row, teacherName: target.displayName, role: cl(row.role), status: teacherStatusCl(row.status) })
  },

  /** 我收到的待确认协作邀请。 */
  async listInvitations(): Promise<CollabInvitationVO[]> {
    if (isRealMode()) return get<CollabInvitationVO[]>('/api/v1/teacher/courses/collab-invitations')
    const teacher = currentUser('TEACHER')
    return demoDelay(db.courseTeachers
      .filter((item) => item.teacherId === teacher.userId && item.status === 'PENDING')
      .map((item) => {
        const course = db.courses.find((c) => c.courseId === item.courseId)
        return {
          courseId: item.courseId,
          courseName: course?.name ?? '—',
          inviterId: course?.ownerTeacherId ?? '',
          inviterName: userName(course?.ownerTeacherId ?? ''),
          invitedAt: null,
        }
      }))
  },

  async acceptInvitation(courseId: string): Promise<void> {
    if (isRealMode()) return post(`/api/v1/teacher/courses/collab-invitations/${courseId}/accept`)
    const teacher = currentUser('TEACHER')
    const row = db.courseTeachers.find((item) => item.courseId === courseId && item.teacherId === teacher.userId && item.status === 'PENDING') ?? notFound('邀请不存在或已处理。')
    row.status = 'ACTIVE'; row.version += 1
    persist()
    return demoDelay(undefined)
  },

  async rejectInvitation(courseId: string): Promise<void> {
    if (isRealMode()) return post(`/api/v1/teacher/courses/collab-invitations/${courseId}/reject`)
    const teacher = currentUser('TEACHER')
    const index = db.courseTeachers.findIndex((item) => item.courseId === courseId && item.teacherId === teacher.userId && item.status === 'PENDING')
    if (index < 0) notFound('邀请不存在或已处理。')
    db.courseTeachers.splice(index, 1)
    persist()
    return demoDelay(undefined)
  },

  async removeTeacher(courseId: string, teacherId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/courses/${courseId}/teachers/${teacherId}`)
    requireTeacherCourse(courseId)
    const index = db.courseTeachers.findIndex((item) => item.courseId === courseId && item.teacherId === teacherId)
    if (index < 0) notFound('该教师不在课程团队中。')
    if (db.courseTeachers[index].role === 'OWNER') conflict('不能移除课程主讲教师。', 'OPERATION_NOT_ALLOWED')
    db.courseTeachers.splice(index, 1)
    persist()
    return demoDelay(undefined)
  },
}
