// 3.6 管理员课程审核接口
import { demoDelay } from '../runtime'
import { get, isRealMode, post } from './client'
import { badRequest, cl, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist, userName } from './demo/db'
import type { ReviewRow } from './demo/db'
import { toCourseDetailVO } from './teacherCourses'
import type {
  CourseListQuery,
  CourseReviewDetailVO,
  CourseReviewListItemVO,
  CourseReviewVO,
  PageResponse,
  RejectCourseRequest,
  ReviewCourseRequest,
} from './types'

function toReviewVO(row: ReviewRow): CourseReviewVO {
  return {
    reviewId: row.reviewId,
    courseId: row.courseId,
    reviewStatus: cl(row.reviewStatus),
    reviewerId: row.reviewerId,
    reviewerName: userName(row.reviewerId),
    reason: row.reason ?? null,
    remark: row.remark ?? null,
    reviewedAt: row.reviewedAt,
  }
}

function requirePendingCourse(courseId: string) {
  const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
  if (course.reviewStatus !== 'PENDING') conflict('课程不在待审核状态。', 'OPERATION_NOT_ALLOWED')
  return course
}

export const courseReviewsApi = {
  async list(query: CourseListQuery = {}): Promise<PageResponse<CourseReviewListItemVO>> {
    if (isRealMode()) return get<PageResponse<CourseReviewListItemVO>>('/api/v1/admin/course-reviews', { ...query })
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.courses
      .filter((row) => (query.reviewStatus ? row.reviewStatus === query.reviewStatus : row.reviewStatus !== 'NOT_SUBMITTED'))
      .filter((row) => !keyword || row.name.toLowerCase().includes(keyword) || row.courseCode.toLowerCase().includes(keyword))
      .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
      .map((row) => ({
        courseId: row.courseId,
        courseCode: row.courseCode,
        name: row.name,
        ownerTeacherId: row.ownerTeacherId,
        ownerTeacherName: userName(row.ownerTeacherId),
        term: row.term ?? null,
        courseStatus: cl(row.status),
        reviewStatus: cl(row.reviewStatus),
        updatedAt: row.updatedAt,
      }))
    return demoDelay(paginate(rows, query))
  },

  async detail(courseId: string): Promise<CourseReviewDetailVO> {
    if (isRealMode()) return get<CourseReviewDetailVO>(`/api/v1/admin/course-reviews/${courseId}`)
    const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
    const history = db.courseReviews
      .filter((item) => item.courseId === courseId)
      .sort((a, b) => b.reviewedAt.localeCompare(a.reviewedAt))
      .map(toReviewVO)
    return demoDelay({ course: toCourseDetailVO(course), history })
  },

  async approve(courseId: string, body: ReviewCourseRequest = {}): Promise<CourseReviewVO> {
    if (isRealMode()) return post<CourseReviewVO>(`/api/v1/admin/course-reviews/${courseId}/approve`, body)
    const course = requirePendingCourse(courseId)
    const admin = currentUser('ADMIN')
    Object.assign(course, { reviewStatus: 'APPROVED', latestReviewReason: null, updatedAt: nowIso(), version: course.version + 1 })
    const row: ReviewRow = { reviewId: nextId(), courseId, reviewStatus: 'APPROVED', reviewerId: admin.userId, remark: body.remark ?? null, reviewedAt: nowIso() }
    db.courseReviews.push(row)
    persist()
    return demoDelay(toReviewVO(row))
  },

  async reject(courseId: string, body: RejectCourseRequest): Promise<CourseReviewVO> {
    if (isRealMode()) return post<CourseReviewVO>(`/api/v1/admin/course-reviews/${courseId}/reject`, body)
    if (!body.reason?.trim()) badRequest('驳回必须填写原因。')
    const course = requirePendingCourse(courseId)
    const admin = currentUser('ADMIN')
    Object.assign(course, { reviewStatus: 'REJECTED', latestReviewReason: body.reason, updatedAt: nowIso(), version: course.version + 1 })
    const row: ReviewRow = { reviewId: nextId(), courseId, reviewStatus: 'REJECTED', reviewerId: admin.userId, reason: body.reason, reviewedAt: nowIso() }
    db.courseReviews.push(row)
    persist()
    return demoDelay(toReviewVO(row))
  },
}
