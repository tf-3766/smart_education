// 3.5 学生课程与学习接口
import { demoDelay } from '../runtime'
import { get, isRealMode, post } from './client'
import { cl, conflict, currentUser, db, nextId, notFound, nowIso, paginate, persist, userName } from './demo/db'
import type { CourseRow, LessonRow, MaterialRow } from './demo/db'
import { toCourseDetailVO } from './teacherCourses'
import type {
  CourseDetailVO,
  CourseListQuery,
  CourseOutlineVO,
  CourseProgressVO,
  EnrollmentVO,
  LearningRecordVO,
  MaterialAccessVO,
  PageResponse,
  StudentCourseListItemVO,
  StudentLessonDetailVO,
} from './types'

function enrollmentOf(courseId: string, studentId: string) {
  return db.enrollments.find((item) => item.courseId === courseId && item.studentId === studentId)
}

function toEnrollmentVO(row: NonNullable<ReturnType<typeof enrollmentOf>>): EnrollmentVO {
  return { ...row, status: cl(row.status), enrolledAt: row.enrolledAt ?? null, withdrawnAt: row.withdrawnAt ?? null }
}

function toStudentItem(row: CourseRow, studentId: string): StudentCourseListItemVO {
  const enrollment = enrollmentOf(row.courseId, studentId)
  const enrolled = enrollment?.status === 'ENROLLED'
  const withinWindow = (!row.enrollmentOpenAt || row.enrollmentOpenAt <= nowIso()) && (!row.enrollmentCloseAt || row.enrollmentCloseAt >= nowIso())
  return {
    courseId: row.courseId,
    courseCode: row.courseCode,
    name: row.name,
    summary: row.summary ?? null,
    coverUrl: row.coverUrl ?? null,
    term: row.term ?? null,
    credit: row.credit ?? null,
    ownerTeacherName: userName(row.ownerTeacherId),
    status: cl(row.status),
    enrollmentStatus: cl(enrollment?.status ?? 'NOT_ENROLLED'),
    enrollable: row.status === 'PUBLISHED' && !enrolled && withinWindow,
    startAt: row.startAt ?? null,
    endAt: row.endAt ?? null,
  }
}

function requireEnrolledCourse(courseId: string, studentId: string): CourseRow {
  const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
  if (enrollmentOf(courseId, studentId)?.status !== 'ENROLLED') conflict('尚未选修该课程。', 'FORBIDDEN')
  return course
}

function lessonUnlocked(lesson: LessonRow): boolean {
  if (lesson.unlockType === 'SCHEDULED') return !lesson.unlockAt || lesson.unlockAt <= nowIso()
  return true
}

function recordOf(lessonId: string, studentId: string) {
  return db.learningRecords.find((item) => item.lessonId === lessonId && item.studentId === studentId)
}

function toRecordVO(row: NonNullable<ReturnType<typeof recordOf>>): LearningRecordVO {
  return { ...row, status: cl(row.status), studySeconds: row.studySeconds ?? 0, startedAt: row.startedAt ?? null, completedAt: row.completedAt ?? null, lastStudiedAt: row.lastStudiedAt ?? null }
}

function toMaterialAccess(row: MaterialRow): MaterialAccessVO {
  return {
    materialId: row.materialId,
    name: row.name,
    materialType: cl(row.materialType),
    fileSize: row.fileSize ?? null,
    mimeType: row.mimeType ?? null,
    accessMode: row.fileId ? 'MANAGED_FILE' : row.materialType === 'LINK' ? 'EXTERNAL_LINK' : 'MOCK_METADATA_ONLY',
    accessUrl: row.fileUrl ?? (row.fileId ? `/api/v1/files/${row.fileId}/content` : ''),
  }
}

function lessonMaterials(lesson: LessonRow): MaterialAccessVO[] {
  return db.materials
    .filter((item) => item.courseId === lesson.courseId && item.status === 'PUBLISHED')
    .filter((item) => !item.chapterId || item.chapterId === lesson.chapterId)
    .filter((item) => !item.lessonId || item.lessonId === lesson.lessonId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(toMaterialAccess)
}
function publishedLessons(courseId: string): LessonRow[] {
  const publishedChapters = new Set(db.chapters.filter((item) => item.courseId === courseId && item.status === 'PUBLISHED').map((item) => item.chapterId))
  return db.lessons
    .filter((item) => item.courseId === courseId && item.status === 'PUBLISHED' && publishedChapters.has(item.chapterId))
    .sort((a, b) => a.sortOrder - b.sortOrder)
}

export const studentLearningApi = {
  /** 全部可选课程目录（已发布）。 */
  async catalog(query: CourseListQuery = {}): Promise<PageResponse<StudentCourseListItemVO>> {
    if (isRealMode()) return get<PageResponse<StudentCourseListItemVO>>('/api/v1/student/courses/catalog', { ...query })
    const student = currentUser('STUDENT')
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.courses
      .filter((row) => row.status === 'PUBLISHED')
      .filter((row) => !query.categoryId || row.categoryId === String(query.categoryId))
      .filter((row) => !query.term || row.term === query.term)
      .filter((row) => !keyword || row.name.toLowerCase().includes(keyword) || row.courseCode.toLowerCase().includes(keyword))
    return demoDelay(paginate(rows.map((row) => toStudentItem(row, student.userId)), query))
  },

  /** 我的课程（已选）。 */
  async myCourses(query: CourseListQuery = {}): Promise<PageResponse<StudentCourseListItemVO>> {
    if (isRealMode()) return get<PageResponse<StudentCourseListItemVO>>('/api/v1/student/courses', { ...query })
    const student = currentUser('STUDENT')
    const enrolledIds = new Set(db.enrollments.filter((item) => item.studentId === student.userId && item.status === 'ENROLLED').map((item) => item.courseId))
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.courses
      .filter((row) => enrolledIds.has(row.courseId))
      .filter((row) => !keyword || row.name.toLowerCase().includes(keyword))
    return demoDelay(paginate(rows.map((row) => toStudentItem(row, student.userId)), query))
  },

  async getCourse(courseId: string): Promise<CourseDetailVO> {
    if (isRealMode()) return get<CourseDetailVO>(`/api/v1/student/courses/${courseId}`)
    const course = db.courses.find((item) => item.courseId === courseId && item.status === 'PUBLISHED') ?? notFound('课程不存在或未发布。')
    return demoDelay(toCourseDetailVO(course))
  },

  async enroll(courseId: string): Promise<EnrollmentVO> {
    if (isRealMode()) return post<EnrollmentVO>(`/api/v1/student/courses/${courseId}/enroll`)
    const student = currentUser('STUDENT')
    const course = db.courses.find((item) => item.courseId === courseId) ?? notFound('课程不存在。')
    if (course.status !== 'PUBLISHED') conflict('课程未发布，暂不能选课。', 'OPERATION_NOT_ALLOWED')
    let enrollment = enrollmentOf(courseId, student.userId)
    // 与真实后端一致：重复选课幂等返回既有选课记录，不报错。
    if (enrollment?.status === 'ENROLLED') return demoDelay(toEnrollmentVO(enrollment))
    if (enrollment) {
      Object.assign(enrollment, { status: 'ENROLLED', enrolledAt: nowIso(), withdrawnAt: null, version: enrollment.version + 1 })
    } else {
      enrollment = { enrollmentId: nextId(), courseId, studentId: student.userId, status: 'ENROLLED', enrolledAt: nowIso(), version: 0 }
      db.enrollments.push(enrollment)
    }
    persist()
    return demoDelay(toEnrollmentVO(enrollment))
  },

  async withdraw(courseId: string): Promise<EnrollmentVO> {
    if (isRealMode()) return post<EnrollmentVO>(`/api/v1/student/courses/${courseId}/withdraw`)
    const student = currentUser('STUDENT')
    const enrollment = enrollmentOf(courseId, student.userId)
    if (!enrollment || enrollment.status !== 'ENROLLED') conflict('尚未选修该课程，无法退课。', 'OPERATION_NOT_ALLOWED')
    Object.assign(enrollment, { status: 'WITHDRAWN', withdrawnAt: nowIso(), version: enrollment.version + 1 })
    persist()
    return demoDelay(toEnrollmentVO(enrollment))
  },

  async outline(courseId: string): Promise<CourseOutlineVO> {
    if (isRealMode()) return get<CourseOutlineVO>(`/api/v1/student/courses/${courseId}/outline`)
    const student = currentUser('STUDENT')
    const course = requireEnrolledCourse(courseId, student.userId)
    const chapters = db.chapters
      .filter((item) => item.courseId === courseId && item.status === 'PUBLISHED')
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map((chapter) => ({
        chapterId: chapter.chapterId,
        title: chapter.title,
        sortOrder: chapter.sortOrder,
        lessons: db.lessons
          .filter((lesson) => lesson.chapterId === chapter.chapterId && lesson.status === 'PUBLISHED')
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((lesson) => {
            const record = recordOf(lesson.lessonId, student.userId)
            return {
              lessonId: lesson.lessonId,
              title: lesson.title,
              sortOrder: lesson.sortOrder,
              contentType: cl(lesson.contentType),
              estimatedMinutes: lesson.estimatedMinutes ?? null,
              unlocked: lessonUnlocked(lesson),
              completed: record?.status === 'COMPLETED',
              learningStatus: cl(record?.status ?? 'NOT_STARTED'),
              materials: lessonMaterials(lesson),
            }
          }),
      }))
    return demoDelay({ courseId, courseName: course.name, status: cl(course.status), chapters })
  },

  async lessonDetail(lessonId: string): Promise<StudentLessonDetailVO> {
    if (isRealMode()) return get<StudentLessonDetailVO>(`/api/v1/student/lessons/${lessonId}`)
    const student = currentUser('STUDENT')
    const lesson = db.lessons.find((item) => item.lessonId === lessonId && item.status === 'PUBLISHED') ?? notFound('课时不存在或未发布。')
    requireEnrolledCourse(lesson.courseId, student.userId)
    if (!lessonUnlocked(lesson)) conflict('课时尚未解锁。', 'OPERATION_NOT_ALLOWED')
    const record = recordOf(lessonId, student.userId)
    return demoDelay({
      lessonId: lesson.lessonId,
      courseId: lesson.courseId,
      chapterId: lesson.chapterId,
      title: lesson.title,
      contentType: cl(lesson.contentType),
      content: lesson.content ?? null,
      videoUrl: lesson.videoUrl ?? null,
      estimatedMinutes: lesson.estimatedMinutes ?? null,
      status: cl(lesson.status),
      unlockAt: lesson.unlockAt ?? null,
      materials: lessonMaterials(lesson),
      learningRecord: record ? toRecordVO(record) : null,
    })
  },

  async startLesson(lessonId: string): Promise<LearningRecordVO> {
    if (isRealMode()) return post<LearningRecordVO>(`/api/v1/student/lessons/${lessonId}/start`)
    const student = currentUser('STUDENT')
    const lesson = db.lessons.find((item) => item.lessonId === lessonId && item.status === 'PUBLISHED') ?? notFound('课时不存在或未发布。')
    requireEnrolledCourse(lesson.courseId, student.userId)
    let record = recordOf(lessonId, student.userId)
    if (!record) {
      record = { recordId: nextId(), courseId: lesson.courseId, chapterId: lesson.chapterId, lessonId, studentId: student.userId, status: 'IN_PROGRESS', startedAt: nowIso(), lastStudiedAt: nowIso() }
      db.learningRecords.push(record)
    } else if (record.status !== 'COMPLETED') {
      Object.assign(record, { status: 'IN_PROGRESS', lastStudiedAt: nowIso() })
    } else {
      record.lastStudiedAt = nowIso()
    }
    persist()
    return demoDelay(toRecordVO(record))
  },

  async heartbeatLesson(lessonId: string): Promise<LearningRecordVO> {
    if (isRealMode()) return post<LearningRecordVO>(`/api/v1/student/lessons/${lessonId}/heartbeat`)
    const student = currentUser('STUDENT')
    const lesson = db.lessons.find((item) => item.lessonId === lessonId && item.status === 'PUBLISHED') ?? notFound('课时不存在或未发布。')
    requireEnrolledCourse(lesson.courseId, student.userId)
    let record = recordOf(lessonId, student.userId)
    const now = Date.now()
    if (!record) {
      record = { recordId: nextId(), courseId: lesson.courseId, chapterId: lesson.chapterId, lessonId, studentId: student.userId, status: 'IN_PROGRESS', studySeconds: 0, startedAt: nowIso(), lastStudiedAt: nowIso() }
      db.learningRecords.push(record)
    } else if (record.status !== 'COMPLETED') {
      const last = record.lastStudiedAt ? new Date(record.lastStudiedAt).getTime() : now
      const delta = Math.max(0, Math.min(30, Math.floor((now - last) / 1000)))
      record.studySeconds = (record.studySeconds ?? 0) + delta
      record.lastStudiedAt = new Date(now).toISOString()
      record.status = 'IN_PROGRESS'
    }
    persist()
    return demoDelay(toRecordVO(record))
  },

  async completeLesson(lessonId: string): Promise<LearningRecordVO> {
    if (isRealMode()) return post<LearningRecordVO>(`/api/v1/student/lessons/${lessonId}/complete`)
    const student = currentUser('STUDENT')
    const lesson = db.lessons.find((item) => item.lessonId === lessonId && item.status === 'PUBLISHED') ?? notFound('课时不存在或未发布。')
    requireEnrolledCourse(lesson.courseId, student.userId)
    const record = recordOf(lessonId, student.userId)
    if (!record) conflict('请先进入课时开始学习。', 'OPERATION_NOT_ALLOWED')
    if (record.status !== 'COMPLETED') {
      const required = Math.max(0, (lesson.estimatedMinutes ?? 0) * 60)
      if ((record.studySeconds ?? 0) < required) conflict(`还需有效学习 ${required - (record.studySeconds ?? 0)} 秒。`, 'OPERATION_NOT_ALLOWED')
      Object.assign(record, { status: 'COMPLETED', completedAt: record.completedAt ?? nowIso(), lastStudiedAt: nowIso() })
      persist()
    }
    return demoDelay(toRecordVO(record))
  },

  async progress(courseId: string): Promise<CourseProgressVO> {
    if (isRealMode()) return get<CourseProgressVO>(`/api/v1/student/courses/${courseId}/progress`)
    const student = currentUser('STUDENT')
    requireEnrolledCourse(courseId, student.userId)
    const lessons = publishedLessons(courseId)
    const records = db.learningRecords.filter((item) => item.courseId === courseId && item.studentId === student.userId)
    const completedIds = new Set(records.filter((item) => item.status === 'COMPLETED').map((item) => item.lessonId))
    const available = lessons.filter(lessonUnlocked)
    const lastRecord = [...records].sort((a, b) => (b.lastStudiedAt ?? '').localeCompare(a.lastStudiedAt ?? ''))[0]
    const next = lessons.find((lesson) => lessonUnlocked(lesson) && !completedIds.has(lesson.lessonId))
    return demoDelay({
      courseId,
      totalLessons: lessons.length,
      availableLessons: available.length,
      completedLessons: completedIds.size,
      progressPercent: lessons.length ? Math.round((completedIds.size / lessons.length) * 100) : 0,
      lastLessonId: lastRecord?.lessonId ?? null,
      nextLessonId: next?.lessonId ?? null,
    })
  },

  async materialAccess(materialId: string): Promise<MaterialAccessVO> {
    if (isRealMode()) return get<MaterialAccessVO>(`/api/v1/student/materials/${materialId}`)
    const student = currentUser('STUDENT')
    const material = db.materials.find((item) => item.materialId === materialId && item.status === 'PUBLISHED') ?? notFound('资料不存在或未发布。')
    if (material.visibility !== 'PUBLIC') requireEnrolledCourse(material.courseId, student.userId)
    return demoDelay({
      materialId: material.materialId,
      name: material.name,
      materialType: cl(material.materialType),
      fileSize: material.fileSize ?? null,
      mimeType: material.mimeType ?? null,
      accessMode: material.fileUrl ? 'LINK' : 'DOWNLOAD',
      accessUrl: material.fileUrl ?? (material.fileId ? `/api/v1/files/${material.fileId}/content` : ''),
    })
  },
}
