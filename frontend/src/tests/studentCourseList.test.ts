import { afterEach, describe, expect, it, vi } from 'vitest'
import StudentCourseListPage from '@/domains/student/StudentCourseListPage.vue'
import { db } from '@/services/api/demo/db'
import { freshDemo, mountPage, settle } from './pageTestUtils'

function envelope(data: unknown) {
  return new Response(JSON.stringify({ code: 'SUCCESS', message: 'OK', data, traceId: 't' }), {
    status: 200, headers: { 'Content-Type': 'application/json' },
  })
}

describe('学生「我的课程」页 · 仅在学课程', () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs() })

  it('只展示已选课程（含已结束/已下线），未选课程不出现', async () => {
    freshDemo()
    // 学生 4 已选 21001/21002；置 21001 已下线、21002 已结束。21004 未选。
    for (const course of db.courses) {
      if (course.courseId === '21001') course.status = 'OFFLINE'
      if (course.courseId === '21002') course.status = 'FINISHED'
    }
    const { wrapper } = await mountPage(StudentCourseListPage, { path: '/student/courses' })
    await settle(500)

    expect(wrapper.text()).toContain('Python 程序设计')
    expect(wrapper.text()).toContain('人工智能导论')
    expect(wrapper.text()).not.toContain('概率论与数理统计') // 21004 未选 → 不在我的课程

    const rows = wrapper.findAll('tbody tr')
    const offlineRow = rows.find((r) => r.text().includes('Python 程序设计'))!
    const finishedRow = rows.find((r) => r.text().includes('人工智能导论'))!
    expect(offlineRow.find('button').attributes('disabled')).toBeDefined()
    expect(offlineRow.text()).toContain('已下线')
    expect(finishedRow.text()).toContain('进入学习')
    expect(finishedRow.find('button').attributes('disabled')).toBeUndefined()
  })

  it('可退课：确认后课程从「我的课程」移除', async () => {
    freshDemo()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    try {
      const { wrapper } = await mountPage(StudentCourseListPage, { path: '/student/courses' })
      await settle(500)
      expect(wrapper.text()).toContain('人工智能导论')
      const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('人工智能导论'))!
      await row.findAll('button').find((b) => b.text() === '退课')!.trigger('click')
      await settle(800)
      expect(confirmSpy).toHaveBeenCalled()
      expect(wrapper.text()).not.toContain('人工智能导论')
      const enrollment = db.enrollments.find((e) => e.courseId === '21002' && e.studentId === '4')!
      expect(enrollment.status).toBe('WITHDRAWN')
    } finally { confirmSpy.mockRestore() }
  })

  it('退课确认弹窗取消时不发起退课', async () => {
    freshDemo()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    try {
      const { wrapper } = await mountPage(StudentCourseListPage, { path: '/student/courses' })
      await settle(500)
      const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('人工智能导论'))!
      await row.findAll('button').find((b) => b.text() === '退课')!.trigger('click')
      await settle(300)
      expect(wrapper.text()).toContain('人工智能导论')
      const enrollment = db.enrollments.find((e) => e.courseId === '21002' && e.studentId === '4')!
      expect(enrollment.status).toBe('ENROLLED')
    } finally { confirmSpy.mockRestore() }
  })

  it('无已选课程时提示前往选课中心', async () => {
    freshDemo()
    db.enrollments.length = 0 // 清空选课
    const { wrapper } = await mountPage(StudentCourseListPage, { path: '/student/courses' })
    await settle(500)
    expect(wrapper.text()).toContain('去选课中心加入')
  })

  it('real 模式：已选课程接口有数据即渲染，只请求 /student/courses', async () => {
    vi.stubEnv('VITE_API_MODE', 'real')
    sessionStorage.setItem('smart-education-token', 'token')
    const mine = {
      records: [
        { courseId: '2076677237032816641', courseCode: 'LD-x', name: '联调课程x', summary: 's', coverUrl: null, term: null, credit: 2, ownerTeacherName: '测试教师', status: { code: 'PUBLISHED', label: '已发布' }, enrollmentStatus: { code: 'ENROLLED', label: '已选课' }, enrollable: false, startAt: null, endAt: null },
      ],
      page: 1, size: 100, total: 1, totalPages: 1,
    }
    const fetchMock = vi.fn().mockResolvedValue(envelope(mine))
    vi.stubGlobal('fetch', fetchMock)

    const { wrapper } = await mountPage(StudentCourseListPage, { path: '/student/courses' })
    await settle(300)
    expect(wrapper.text()).toContain('联调课程x')
    expect(wrapper.text()).not.toContain('还没有选修')
    expect(fetchMock.mock.calls.every((call) => !String(call[0]).includes('/catalog'))).toBe(true)
  })
})
