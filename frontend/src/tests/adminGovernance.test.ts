import { describe, expect, it } from 'vitest'
import ContentGovernancePage from '@/domains/admin/ContentGovernancePage.vue'
import CourseReviewPage from '@/domains/admin/CourseReviewPage.vue'
import UserManagementPage from '@/domains/admin/UserManagementPage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

describe('管理员治理', () => {
  it('课程审核：驳回必填原因，填写后可驳回', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseReviewPage, { path: '/admin/course-reviews' })
    await settle(700)
    expect(wrapper.text()).toContain('数据结构与算法')
    const rejectBtn = wrapper.findAll('button').find((b) => b.text() === '驳回')!
    expect(rejectBtn.attributes('disabled')).toBeDefined()
    await wrapper.find('textarea').setValue('课程大纲不完整，请补充。')
    await clickByText(wrapper, 'button', '驳回')
    await settle(900)
    expect(wrapper.text()).toContain('该状态下暂无课程')
  })

  it('课程治理：可按状态浏览全部课程，已通过课程只读无审核操作', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseReviewPage, { path: '/admin/course-reviews' })
    await settle(700)
    // 默认待审核：只见待审核课程。
    expect(wrapper.text()).toContain('数据结构与算法')

    // 切到“已通过”，可见已发布/已通过的课程。
    await clickByText(wrapper, 'button', '已通过')
    await settle(700)
    const approved = wrapper.findAll('.list-nav').find((n) => n.text().includes('Python 程序设计'))!
    expect(approved).toBeTruthy()
    await approved.trigger('click')
    await settle(500)
    // 已通过课程只读：无“通过/驳回”操作。
    expect(wrapper.text()).toContain('该课程当前无待审核操作')
    expect(wrapper.findAll('button').some((b) => b.text() === '通过')).toBe(false)
  })

  it('用户管理：教师审批通过、授予管理员', async () => {
    freshDemo()
    const { wrapper } = await mountPage(UserManagementPage, { path: '/admin/users' })
    await settle(700)
    expect(wrapper.findComponent({ name: 'UserAvatar' }).exists()).toBe(true)
    const pendingRow = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('高翔'))!
    expect(pendingRow.text()).toContain('待审核')
    await pendingRow.findAll('button').find((b) => b.text() === '通过教师申请')!.trigger('click')
    await settle(900)
    expect(wrapper.findAll('tbody tr').find((tr) => tr.text().includes('高翔'))!.text()).toContain('已启用')
    const teacherRow = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('李明'))!
    await teacherRow.findAll('button').find((b) => b.text() === '授予管理员')!.trigger('click')
    await settle(900)
    expect(wrapper.findAll('tbody tr').find((tr) => tr.text().includes('李明'))!.text()).toContain('ADMIN')
  })

  it('论坛治理：可查看全局主题与回复，并记录原因后隐藏内容', async () => {
    freshDemo()
    const { wrapper } = await mountPage(ContentGovernancePage, { path: '/admin/content' })
    await settle(700)
    expect(wrapper.text()).toContain('列表和元组的区别是什么？')
    expect(wrapper.text()).toContain('关键区别是可变性')

    const topicRow = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('列表和元组的区别是什么？'))!
    await topicRow.findAll('button').find((button) => button.text() === '隐藏')!.trigger('click')
    await wrapper.get('#cg-mod-reason').setValue('偏离课程讨论范围')
    await clickByText(wrapper, 'button', '确认处理')
    await settle(700)

    expect(db.forumTopics.find((item) => item.topicId === '41001')!.status).toBe('HIDDEN')
    expect(db.forumTopics.find((item) => item.topicId === '41001')!.moderationReason).toBe('偏离课程讨论范围')
  })
  it('公告管理：发布教师公告并撤回', async () => {
    freshDemo()
    const { wrapper } = await mountPage(ContentGovernancePage, { path: '/admin/content' })
    await settle(700)
    await clickByText(wrapper, 'button', '发布系统公告')
    await wrapper.find('input.input').setValue('教研会议通知')
    await wrapper.find('select').setValue('TEACHER')
    await wrapper.find('textarea').setValue('本周五下午教研会议，请全体教师参加。')
    await wrapper.findAll('button').find((b) => b.text() === '发布')!.trigger('click')
    await settle(900)
    const row = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('教研会议通知'))!
    expect(row.text()).toContain('已发布')
    await row.findAll('button').find((b) => b.text() === '撤回')!.trigger('click')
    await settle(900)
    expect(wrapper.findAll('tbody tr').find((tr) => tr.text().includes('教研会议通知'))!.text()).toContain('已撤回')
  })
})
