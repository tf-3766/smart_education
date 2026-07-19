// 3.4 教师课程内容管理接口（章节 / 课时 / 资料）
import { demoDelay } from '../runtime'
import { del, get, isRealMode, post, put } from './client'
import { assertVersion, cl, conflict, db, nextId, notFound, nowIso, paginate, persist } from './demo/db'
import type { ChapterRow, LessonRow, MaterialRow } from './demo/db'
import { requireTeacherCourse } from './teacherCourses'
import type {
  ChapterDetailVO,
  CourseMaterialListQuery,
  CourseMaterialVO,
  CreateChapterRequest,
  CreateCourseMaterialRequest,
  CreateLessonRequest,
  LessonDetailVO,
  PageResponse,
  UpdateChapterRequest,
  UpdateCourseMaterialRequest,
  UpdateLessonRequest,
} from './types'

function toChapterVO(row: ChapterRow): ChapterDetailVO {
  return { ...row, description: row.description ?? null, status: cl(row.status), publishedAt: row.publishedAt ?? null }
}

export function toLessonVO(row: LessonRow): LessonDetailVO {
  return {
    ...row,
    content: row.content ?? null,
    videoUrl: row.videoUrl ?? null,
    estimatedMinutes: row.estimatedMinutes ?? null,
    unlockAt: row.unlockAt ?? null,
    publishedAt: row.publishedAt ?? null,
    contentType: cl(row.contentType),
    status: cl(row.status),
    unlockType: cl(row.unlockType),
  }
}

function toMaterialVO(row: MaterialRow): CourseMaterialVO {
  return {
    ...row,
    chapterId: row.chapterId ?? null,
    lessonId: row.lessonId ?? null,
    fileId: row.fileId ?? null,
    fileKey: row.fileKey ?? null,
    fileUrl: row.fileUrl ?? null,
    fileSize: row.fileSize ?? null,
    mimeType: row.mimeType ?? null,
    materialType: cl(row.materialType),
    visibility: cl(row.visibility),
    status: cl(row.status),
  }
}

function requireChapter(chapterId: string): ChapterRow {
  const row = db.chapters.find((item) => item.chapterId === chapterId) ?? notFound('章节不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requireLesson(lessonId: string): LessonRow {
  const row = db.lessons.find((item) => item.lessonId === lessonId) ?? notFound('课时不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function requireMaterial(materialId: string): MaterialRow {
  const row = db.materials.find((item) => item.materialId === materialId) ?? notFound('资料不存在。')
  requireTeacherCourse(row.courseId)
  return row
}

function applyMaterialFields(row: MaterialRow, courseId: string, body: CreateCourseMaterialRequest | UpdateCourseMaterialRequest): void {
  if (body.fileId != null && (body.fileKey || body.fileUrl)) conflict('托管文件 fileId 与外部 fileKey/fileUrl 只能二选一。', 'PARAM_VALIDATION_ERROR')
  Object.assign(row, {
    courseId,
    chapterId: body.chapterId == null ? null : String(body.chapterId),
    lessonId: body.lessonId == null ? null : String(body.lessonId),
    name: body.name,
    materialType: body.materialType,
    fileId: body.fileId == null ? null : String(body.fileId),
    fileKey: body.fileKey ?? null,
    fileUrl: body.fileUrl ?? null,
    fileSize: body.fileSize ?? null,
    mimeType: body.mimeType ?? null,
    visibility: body.visibility,
    sortOrder: body.sortOrder,
  })
}

export const courseContentApi = {
  // —— 章节 ——
  async listChapters(courseId: string): Promise<ChapterDetailVO[]> {
    if (isRealMode()) return get<ChapterDetailVO[]>(`/api/v1/teacher/courses/${courseId}/chapters`)
    requireTeacherCourse(courseId)
    return demoDelay(db.chapters.filter((item) => item.courseId === courseId).sort((a, b) => a.sortOrder - b.sortOrder).map(toChapterVO))
  },

  async createChapter(courseId: string, body: CreateChapterRequest): Promise<ChapterDetailVO> {
    if (isRealMode()) return post<ChapterDetailVO>(`/api/v1/teacher/courses/${courseId}/chapters`, body)
    requireTeacherCourse(courseId)
    const row: ChapterRow = { chapterId: nextId(), courseId, title: body.title, description: body.description ?? null, sortOrder: body.sortOrder, status: 'DRAFT', version: 0 }
    db.chapters.push(row)
    persist()
    return demoDelay(toChapterVO(row))
  },

  async updateChapter(chapterId: string, body: UpdateChapterRequest): Promise<ChapterDetailVO> {
    if (isRealMode()) return put<ChapterDetailVO>(`/api/v1/teacher/chapters/${chapterId}`, body)
    const row = requireChapter(chapterId)
    assertVersion(row, body.version)
    Object.assign(row, { title: body.title, description: body.description ?? null, sortOrder: body.sortOrder, version: row.version + 1 })
    persist()
    return demoDelay(toChapterVO(row))
  },

  async deleteChapter(chapterId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/chapters/${chapterId}`)
    const row = requireChapter(chapterId)
    if (db.lessons.some((lesson) => lesson.chapterId === chapterId)) conflict('章节下仍有课时，先删除课时。', 'OPERATION_NOT_ALLOWED')
    db.chapters.splice(db.chapters.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  async publishChapter(chapterId: string): Promise<ChapterDetailVO> {
    if (isRealMode()) return post<ChapterDetailVO>(`/api/v1/teacher/chapters/${chapterId}/publish`)
    const row = requireChapter(chapterId)
    Object.assign(row, { status: 'PUBLISHED', publishedAt: nowIso(), version: row.version + 1 })
    persist()
    return demoDelay(toChapterVO(row))
  },

  async offlineChapter(chapterId: string): Promise<ChapterDetailVO> {
    if (isRealMode()) return post<ChapterDetailVO>(`/api/v1/teacher/chapters/${chapterId}/offline`)
    const row = requireChapter(chapterId)
    Object.assign(row, { status: 'OFFLINE', version: row.version + 1 })
    persist()
    return demoDelay(toChapterVO(row))
  },

  // —— 课时 ——
  async listLessons(chapterId: string): Promise<LessonDetailVO[]> {
    if (isRealMode()) return get<LessonDetailVO[]>(`/api/v1/teacher/chapters/${chapterId}/lessons`)
    requireChapter(chapterId)
    return demoDelay(db.lessons.filter((item) => item.chapterId === chapterId).sort((a, b) => a.sortOrder - b.sortOrder).map(toLessonVO))
  },

  async createLesson(chapterId: string, body: CreateLessonRequest): Promise<LessonDetailVO> {
    if (isRealMode()) return post<LessonDetailVO>(`/api/v1/teacher/chapters/${chapterId}/lessons`, body)
    const chapter = requireChapter(chapterId)
    const row: LessonRow = {
      lessonId: nextId(),
      courseId: chapter.courseId,
      chapterId,
      title: body.title,
      contentType: body.contentType,
      content: body.content ?? null,
      videoUrl: body.videoUrl ?? null,
      estimatedMinutes: body.estimatedMinutes ?? null,
      sortOrder: body.sortOrder,
      status: 'DRAFT',
      unlockType: body.unlockType,
      unlockAt: body.unlockAt ?? null,
      version: 0,
    }
    db.lessons.push(row)
    persist()
    return demoDelay(toLessonVO(row))
  },

  async getLesson(lessonId: string): Promise<LessonDetailVO> {
    if (isRealMode()) return get<LessonDetailVO>(`/api/v1/teacher/lessons/${lessonId}`)
    return demoDelay(toLessonVO(requireLesson(lessonId)))
  },

  async updateLesson(lessonId: string, body: UpdateLessonRequest): Promise<LessonDetailVO> {
    if (isRealMode()) return put<LessonDetailVO>(`/api/v1/teacher/lessons/${lessonId}`, body)
    const row = requireLesson(lessonId)
    assertVersion(row, body.version)
    Object.assign(row, {
      title: body.title,
      contentType: body.contentType,
      content: body.content ?? null,
      videoUrl: body.videoUrl ?? null,
      estimatedMinutes: body.estimatedMinutes ?? null,
      sortOrder: body.sortOrder,
      unlockType: body.unlockType,
      unlockAt: body.unlockAt ?? null,
      version: row.version + 1,
    })
    persist()
    return demoDelay(toLessonVO(row))
  },

  async deleteLesson(lessonId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/lessons/${lessonId}`)
    const row = requireLesson(lessonId)
    db.lessons.splice(db.lessons.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },

  async publishLesson(lessonId: string): Promise<LessonDetailVO> {
    if (isRealMode()) return post<LessonDetailVO>(`/api/v1/teacher/lessons/${lessonId}/publish`)
    const row = requireLesson(lessonId)
    Object.assign(row, { status: 'PUBLISHED', publishedAt: nowIso(), version: row.version + 1 })
    persist()
    return demoDelay(toLessonVO(row))
  },

  async offlineLesson(lessonId: string): Promise<LessonDetailVO> {
    if (isRealMode()) return post<LessonDetailVO>(`/api/v1/teacher/lessons/${lessonId}/offline`)
    const row = requireLesson(lessonId)
    Object.assign(row, { status: 'OFFLINE', version: row.version + 1 })
    persist()
    return demoDelay(toLessonVO(row))
  },

  // —— 资料 ——
  async listMaterials(courseId: string, query: CourseMaterialListQuery = {}): Promise<PageResponse<CourseMaterialVO>> {
    if (isRealMode()) return get<PageResponse<CourseMaterialVO>>(`/api/v1/teacher/courses/${courseId}/materials`, { ...query })
    requireTeacherCourse(courseId)
    const keyword = query.keyword?.trim().toLowerCase()
    const rows = db.materials
      .filter((item) => item.courseId === courseId)
      .filter((item) => !query.status || item.status === query.status)
      .filter((item) => !query.visibility || item.visibility === query.visibility)
      .filter((item) => !keyword || item.name.toLowerCase().includes(keyword))
      .sort((a, b) => a.sortOrder - b.sortOrder)
    return demoDelay(paginate(rows.map(toMaterialVO), query))
  },

  async createMaterial(courseId: string, body: CreateCourseMaterialRequest): Promise<CourseMaterialVO> {
    if (isRealMode()) return post<CourseMaterialVO>(`/api/v1/teacher/courses/${courseId}/materials`, body)
    requireTeacherCourse(courseId)
    const row = { materialId: nextId(), status: body.status ?? 'DRAFT', version: 0 } as MaterialRow
    applyMaterialFields(row, courseId, body)
    db.materials.push(row)
    persist()
    return demoDelay(toMaterialVO(row))
  },

  async updateMaterial(materialId: string, body: UpdateCourseMaterialRequest): Promise<CourseMaterialVO> {
    if (isRealMode()) return put<CourseMaterialVO>(`/api/v1/teacher/materials/${materialId}`, body)
    const row = requireMaterial(materialId)
    assertVersion(row, body.version)
    applyMaterialFields(row, row.courseId, body)
    row.status = body.status
    row.version += 1
    persist()
    return demoDelay(toMaterialVO(row))
  },

  async deleteMaterial(materialId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/teacher/materials/${materialId}`)
    const row = requireMaterial(materialId)
    db.materials.splice(db.materials.indexOf(row), 1)
    persist()
    return demoDelay(undefined)
  },
}
