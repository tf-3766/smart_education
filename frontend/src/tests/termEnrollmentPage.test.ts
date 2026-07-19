import { describe, expect, it } from 'vitest'
import TermEnrollmentPage from '@/domains/admin/TermEnrollmentPage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

describe('学期选课时间', () => {
  it('展示已配置学期，并可新增一个学期窗口', async () => {
    freshDemo()
    const { wrapper } = await mountPage(TermEnrollmentPage, { path: '/admin/term-enrollment' })
    await settle(500)
    expect(wrapper.text()).toContain('2026 秋季')

    await wrapper.get('#te-term').setValue('2027 春季')
    await wrapper.get('#te-open').setValue('2027-02-20T09:00')
    await wrapper.get('#te-close').setValue('2027-03-05T17:00')
    await clickByText(wrapper, 'button', '保存学期窗口')
    await settle()

    expect(wrapper.text()).toContain('2027 春季')
    expect(db.termEnrollmentWindows.some((w) => w.term === '2027 春季')).toBe(true)
  })

  it('截止早于开始时拦截保存', async () => {
    freshDemo()
    const { wrapper } = await mountPage(TermEnrollmentPage, { path: '/admin/term-enrollment' })
    await settle(500)
    await wrapper.get('#te-term').setValue('2027 秋季')
    await wrapper.get('#te-open').setValue('2027-09-10T09:00')
    await wrapper.get('#te-close').setValue('2027-09-01T09:00')
    await clickByText(wrapper, 'button', '保存学期窗口')
    await settle()
    expect(wrapper.text()).toContain('选课截止时间需晚于开始时间')
    expect(db.termEnrollmentWindows.some((w) => w.term === '2027 秋季')).toBe(false)
  })
})
