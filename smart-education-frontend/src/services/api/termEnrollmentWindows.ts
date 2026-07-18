// 学期统一选课窗口：管理员按学期维护；课程未单独设置选课时间时继承本窗口。
import { demoDelay } from '../runtime'
import { get, isRealMode, put } from './client'
import { db, nextId, persist } from './demo/db'
import type { TermEnrollmentWindowVO, UpsertTermEnrollmentWindowRequest } from './types'

const sortByTerm = (a: TermEnrollmentWindowVO, b: TermEnrollmentWindowVO) => a.term.localeCompare(b.term)

export const termEnrollmentWindowsApi = {
  async list(): Promise<TermEnrollmentWindowVO[]> {
    if (isRealMode()) return get<TermEnrollmentWindowVO[]>('/api/v1/admin/term-enrollment-windows')
    return demoDelay([...db.termEnrollmentWindows].sort(sortByTerm))
  },

  /** 按学期新增或更新（term 唯一）。 */
  async upsert(body: UpsertTermEnrollmentWindowRequest): Promise<TermEnrollmentWindowVO> {
    if (isRealMode()) return put<TermEnrollmentWindowVO>('/api/v1/admin/term-enrollment-windows', body)
    const term = body.term.trim()
    const existing = db.termEnrollmentWindows.find((item) => item.term === term)
    if (existing) {
      Object.assign(existing, {
        enrollmentOpenAt: body.enrollmentOpenAt ?? null,
        enrollmentCloseAt: body.enrollmentCloseAt ?? null,
        version: existing.version + 1,
      })
      persist()
      return demoDelay(existing)
    }
    const row = {
      windowId: nextId(),
      term,
      enrollmentOpenAt: body.enrollmentOpenAt ?? null,
      enrollmentCloseAt: body.enrollmentCloseAt ?? null,
      version: 0,
    }
    db.termEnrollmentWindows.push(row)
    persist()
    return demoDelay(row)
  },
}
