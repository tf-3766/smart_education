import { afterEach, describe, expect, it, vi } from 'vitest'
import EnrollCenterPage from '@/domains/student/EnrollCenterPage.vue'
import { freshDemo, mountPage, settle } from './pageTestUtils'

function envelope(data: unknown) {
  return new Response(JSON.stringify({ code: 'SUCCESS', message: 'OK', data, traceId: 't' }), {
    status: 200, headers: { 'Content-Type': 'application/json' },
  })
}

describe('学生「选课中心」页 · 可选目录', () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs() })

  it('展示已发布课程，未选课程可加入，加入后转为可去学习', async () => {
    freshDemo()
    const { wrapper } = await mountPage(EnrollCenterPage, { path: '/student/enroll' })
    await settle(500)

    // 21004 概率论与数理统计：已发布、学生 4 未选 → 加入学习
    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('概率论与数理统计'))!
    expect(row).toBeTruthy()
    expect(row.text()).toContain('加入学习')

    await row.findAll('button').find((b) => b.text() === '加入学习')!.trigger('click')
    await settle(700)

    const after = wrapper.findAll('tbody tr').find((r) => r.text().includes('概率论与数理统计'))!
    expect(after.text()).toContain('去学习')
  })

  it('real 模式：目录课程 enrollmentStatus 为 null 时显示未选课且不崩', async () => {
    vi.stubEnv('VITE_API_MODE', 'real')
    sessionStorage.setItem('smart-education-token', 'token')
    const catalog = {
      records: [
        { courseId: '21003', courseCode: 'C-OTHER', name: '其他教师课程', summary: 's', coverUrl: null, term: null, credit: 2, ownerTeacherName: '测试教师二', status: { code: 'PUBLISHED', label: '已发布' }, enrollmentStatus: null, enrollable: true, startAt: null, endAt: null },
      ],
      page: 1, size: 100, total: 1, totalPages: 1,
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(envelope(catalog)))

    const { wrapper } = await mountPage(EnrollCenterPage, { path: '/student/enroll' })
    await settle(300)
    expect(wrapper.text()).toContain('其他教师课程')
    expect(wrapper.text()).toContain('未选课')
    expect(wrapper.text()).toContain('加入学习')
  })
})
