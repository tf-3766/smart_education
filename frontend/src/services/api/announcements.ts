// 4.3 公告接口（课程公告 + 系统公告，按受众过滤）
import { demoDelay } from '../runtime'
import { get, isRealMode, post } from './client'
import { assertVersion, badRequest, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist } from './demo/db'
import type { AnnouncementRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type { AnnouncementListQuery, AnnouncementVO, CreateAnnouncementRequest, PageResponse, WithdrawAnnouncementRequest } from './types'

function toVO(row: AnnouncementRow): AnnouncementVO {
  return { ...row, courseId: row.courseId ?? null, withdrawnAt: row.withdrawnAt ?? null }
}

function byPublishedDesc(a: AnnouncementRow, b: AnnouncementRow) {
  return (b.publishedAt ?? '').localeCompare(a.publishedAt ?? '')
}

function withdraw(row: AnnouncementRow, body: WithdrawAnnouncementRequest): AnnouncementVO {
  assertVersion(row, body.version)
  if (row.status === 'WITHDRAWN') conflict('公告已撤回。', 'OPERATION_NOT_ALLOWED')
  Object.assign(row, { status: 'WITHDRAWN', withdrawnAt: nowIso(), version: row.version + 1 })
  persist()
  return toVO(row)
}

export const announcementsApi = {
  // —— 教师端 ——
  async teacherCourseList(courseId: string, query: AnnouncementListQuery = {}): Promise<PageResponse<AnnouncementVO>> {
    if (isRealMode()) return get<PageResponse<AnnouncementVO>>(`/api/v1/teacher/courses/${courseId}/announcements`, { ...query })
    requireTeacherCourse(courseId)
    const rows = db.announcements.filter((row) => row.scopeType === 'COURSE' && row.courseId === courseId).sort(byPublishedDesc)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  async createCourseAnnouncement(courseId: string, body: CreateAnnouncementRequest): Promise<AnnouncementVO> {
    if (isRealMode()) return post<AnnouncementVO>(`/api/v1/teacher/courses/${courseId}/announcements`, body)
    requireTeacherCourse(courseId)
    if (body.audience === 'TEACHER') badRequest('课程公告的受众不能是 TEACHER。')
    const teacher = currentUser('TEACHER')
    const row: AnnouncementRow = { announcementId: nextId(), scopeType: 'COURSE', courseId, title: body.title, content: body.content, audience: body.audience, status: 'PUBLISHED', publishedAt: nowIso(), publisherId: teacher.userId, source: 'HUMAN', version: 0 }
    db.announcements.push(row)
    persist()
    return demoDelay(toVO(row))
  },

  async teacherWithdraw(announcementId: string, body: WithdrawAnnouncementRequest): Promise<AnnouncementVO> {
    if (isRealMode()) return post<AnnouncementVO>(`/api/v1/teacher/announcements/${announcementId}/withdrawal`, body)
    const row = db.announcements.find((item) => item.announcementId === announcementId) ?? notFound('公告不存在。')
    if (row.scopeType === 'COURSE' && row.courseId) requireTeacherCourse(row.courseId)
    return demoDelay(withdraw(row, body))
  },

  async confirmCourseAnnouncement(announcementId: string): Promise<AnnouncementVO> {
    if (isRealMode()) return post<AnnouncementVO>(`/api/v1/teacher/announcements/${announcementId}/confirm`)
    const row = db.announcements.find((item) => item.announcementId === announcementId) ?? notFound('公告不存在。')
    if (row.scopeType !== 'COURSE' || !row.courseId) conflict('该公告不是课程公告。', 'OPERATION_NOT_ALLOWED')
    requireTeacherCourse(row.courseId)
    if (row.status !== 'DRAFT') conflict('只有 AI 草稿公告可以确认发布。', 'OPERATION_NOT_ALLOWED')
    Object.assign(row, { status: 'PUBLISHED', publishedAt: nowIso(), version: row.version + 1 })
    persist()
    return demoDelay(toVO(row))
  },

  /** 教师可见的公告流：负责课程的课程公告 + 面向 ALL/TEACHER 的系统公告。 */
  async teacherList(query: AnnouncementListQuery = {}): Promise<PageResponse<AnnouncementVO>> {
    if (isRealMode()) return get<PageResponse<AnnouncementVO>>('/api/v1/teacher/announcements', { ...query })
    const teacher = currentUser('TEACHER')
    const myCourses = new Set(db.courseTeachers.filter((item) => item.teacherId === teacher.userId).map((item) => item.courseId))
    const rows = db.announcements
      .filter((row) => row.status === 'PUBLISHED')
      .filter((row) => (row.scopeType === 'SYSTEM' ? row.audience === 'ALL' || row.audience === 'TEACHER' : Boolean(row.courseId && myCourses.has(row.courseId))))
      .sort(byPublishedDesc)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  /** 学生可见的公告流：已选课程的课程公告 + 面向 ALL/STUDENT 的系统公告。 */
  async studentList(query: AnnouncementListQuery = {}): Promise<PageResponse<AnnouncementVO>> {
    if (isRealMode()) return get<PageResponse<AnnouncementVO>>('/api/v1/student/announcements', { ...query })
    const student = currentUser('STUDENT')
    const myCourses = new Set(db.enrollments.filter((item) => item.studentId === student.userId && item.status === 'ENROLLED').map((item) => item.courseId))
    const rows = db.announcements
      .filter((row) => row.status === 'PUBLISHED')
      .filter((row) => {
        if (row.audience === 'TEACHER') return false
        if (row.scopeType === 'SYSTEM') return row.audience === 'ALL' || row.audience === 'STUDENT'
        return Boolean(row.courseId && myCourses.has(row.courseId))
      })
      .sort(byPublishedDesc)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  // —— 管理员端 ——
  async adminList(query: AnnouncementListQuery = {}): Promise<PageResponse<AnnouncementVO>> {
    if (isRealMode()) return get<PageResponse<AnnouncementVO>>('/api/v1/admin/announcements', { ...query })
    const rows = db.announcements.filter((row) => row.scopeType === 'SYSTEM').sort(byPublishedDesc)
    return demoDelay(paginate(rows.map(toVO), query))
  },

  async adminCreate(body: CreateAnnouncementRequest): Promise<AnnouncementVO> {
    if (isRealMode()) return post<AnnouncementVO>('/api/v1/admin/announcements', body)
    const admin = currentUser('ADMIN')
    const row: AnnouncementRow = { announcementId: nextId(), scopeType: 'SYSTEM', title: body.title, content: body.content, audience: body.audience, status: 'PUBLISHED', publishedAt: nowIso(), publisherId: admin.userId, source: 'HUMAN', version: 0 }
    db.announcements.push(row)
    persist()
    return demoDelay(toVO(row))
  },

  async adminWithdraw(announcementId: string, body: WithdrawAnnouncementRequest): Promise<AnnouncementVO> {
    if (isRealMode()) return post<AnnouncementVO>(`/api/v1/admin/announcements/${announcementId}/withdrawal`, body)
    const row = db.announcements.find((item) => item.announcementId === announcementId) ?? notFound('公告不存在。')
    return demoDelay(withdraw(row, body))
  },
}
