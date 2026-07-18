// 4.3 课程分类接口（公开查询 + 管理员维护）
import { demoDelay } from '../runtime'
import { del, get, isRealMode, post, put } from './client'
import { assertVersion, conflict, db, nextId, notFound, persist } from './demo/db'
import type { CourseCategoryVO, CreateCourseCategoryRequest, UpdateCourseCategoryRequest } from './types'

const sortCategories = (a: CourseCategoryVO, b: CourseCategoryVO) => a.sortOrder - b.sortOrder

export const categoriesApi = {
  /** 已登录用户可见的启用分类。 */
  async list(): Promise<CourseCategoryVO[]> {
    if (isRealMode()) return get<CourseCategoryVO[]>('/api/v1/course-categories')
    return demoDelay(db.categories.filter((item) => item.enabled).sort(sortCategories))
  },

  async adminList(): Promise<CourseCategoryVO[]> {
    if (isRealMode()) return get<CourseCategoryVO[]>('/api/v1/admin/course-categories')
    return demoDelay([...db.categories].sort(sortCategories))
  },

  async create(body: CreateCourseCategoryRequest): Promise<CourseCategoryVO> {
    if (isRealMode()) return post<CourseCategoryVO>('/api/v1/admin/course-categories', body)
    const row = { categoryId: nextId(), name: body.name, sortOrder: body.sortOrder, enabled: body.enabled, version: 0 }
    db.categories.push(row)
    persist()
    return demoDelay(row)
  },

  async update(categoryId: string, body: UpdateCourseCategoryRequest): Promise<CourseCategoryVO> {
    if (isRealMode()) return put<CourseCategoryVO>(`/api/v1/admin/course-categories/${categoryId}`, body)
    const row = db.categories.find((item) => item.categoryId === categoryId) ?? notFound('分类不存在。')
    assertVersion(row, body.version)
    Object.assign(row, { name: body.name, sortOrder: body.sortOrder, enabled: body.enabled, version: row.version + 1 })
    persist()
    return demoDelay(row)
  },

  async remove(categoryId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/admin/course-categories/${categoryId}`)
    const index = db.categories.findIndex((item) => item.categoryId === categoryId)
    if (index < 0) notFound('分类不存在。')
    if (db.courses.some((course) => course.categoryId === categoryId)) {
      conflict('分类下仍有课程，暂不能删除。', 'OPERATION_NOT_ALLOWED')
    }
    db.categories.splice(index, 1)
    persist()
    return demoDelay(undefined)
  },
}
