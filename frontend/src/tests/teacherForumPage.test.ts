// 回归：主题状态是 {code,label} 对象，页面曾误与字符串比较导致所有主题显示「已隐藏」、
// 且可见性请求体误传 status 字段（契约为 visible: boolean），隐藏/恢复整体失效。
import { describe, expect, it, vi } from 'vitest'
import TeacherForumPage from '@/domains/teacher/TeacherForumPage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

async function mountForum() {
  const mounted = await mountPage(TeacherForumPage, { path: '/teacher/forum' })
  await mounted.wrapper.find('select').setValue('21001')
  await settle(700)
  return mounted
}

describe('教师课程互动页 · 主题可见性', () => {
  it('可见主题显示「可见」徽标与「隐藏」操作', async () => {
    freshDemo()
    const { wrapper } = await mountForum()

    const row = wrapper.findAll('[data-test="topic-row"]').find((r) => r.text().includes('列表和元组的区别'))!
    expect(row.text()).toContain('可见')
    expect(row.text()).not.toContain('已隐藏')
    expect(row.findAll('button').some((b) => b.text() === '隐藏')).toBe(true)
  })

  it('隐藏主题后状态变为已隐藏，且演示库落库 visible=false', async () => {
    freshDemo()
    const { wrapper } = await mountForum()

    const row = wrapper.findAll('[data-test="topic-row"]').find((r) => r.text().includes('列表和元组的区别'))!
    await row.findAll('button').find((b) => b.text() === '隐藏')!.trigger('click')
    await settle(800)

    const topic = db.forumTopics.find((item) => item.topicId === '41001')!
    expect(topic.status).toBe('HIDDEN')
    const updatedRow = wrapper.findAll('[data-test="topic-row"]').find((r) => r.text().includes('列表和元组的区别'))!
    expect(updatedRow.text()).toContain('已隐藏')
    expect(updatedRow.findAll('button').some((b) => b.text() === '恢复')).toBe(true)
  })

  it('回复管理弹窗可隐藏回复并以教师身份发表回复', async () => {
    freshDemo()
    const { wrapper } = await mountForum()

    const row = wrapper.findAll('[data-test="topic-row"]').find((r) => r.text().includes('列表和元组的区别'))!
    await row.findAll('button')[0].trigger('click')
    await settle(700)

    // 主题内容与既有回复渲染。
    expect(wrapper.text()).toContain('看完第一课还是不太理解什么场景该用元组。')
    const replyItem = wrapper.findAll('[data-test="reply-item"]').find((li) => li.text().includes('元组不可变'))!
    await replyItem.findAll('button').find((b) => b.text() === '隐藏')!.trigger('click')
    await settle(800)
    expect(db.forumReplies.find((item) => item.replyId === '42001')!.status).toBe('HIDDEN')

    await wrapper.get('#tf-reply-content').setValue('补充：元组还能做字典的键。')
    await clickByText(wrapper, 'button', '发表回复')
    await settle(800)
    expect(db.forumReplies.some((item) => item.topicId === '41001' && item.content.includes('元组还能做字典的键'))).toBe(true)
  })
})

describe('教师课程互动页 · 课程公告', () => {
  it('展示课程公告并可发布新公告', async () => {
    freshDemo()
    const { wrapper } = await mountForum()

    // 种子公告 61002 属于 21001。
    expect(wrapper.text()).toContain('第一章作业已发布')

    await clickByText(wrapper, 'button', '发布课程公告')
    await wrapper.get('#tf-ann-title').setValue('期中安排')
    await wrapper.get('#tf-ann-content').setValue('第 10 周随堂进行期中测验，请提前复习。')
    await wrapper.findAll('button').find((b) => b.text() === '发布')!.trigger('click')
    await settle(800)

    expect(wrapper.text()).toContain('期中安排')
    expect(db.announcements.some((item) => item.courseId === '21001' && item.title === '期中安排')).toBe(true)
  })

  it('可撤回已发布的课程公告', async () => {
    freshDemo()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    try {
      const { wrapper } = await mountForum()
      const row = wrapper.findAll('[data-test="announcement-row"]').find((r) => r.text().includes('第一章作业已发布'))!
      await row.findAll('button').find((b) => b.text() === '撤回')!.trigger('click')
      await settle(800)
      expect(db.announcements.find((item) => item.announcementId === '61002')!.status).toBe('WITHDRAWN')
      const updatedRow = wrapper.findAll('[data-test="announcement-row"]').find((r) => r.text().includes('第一章作业已发布'))!
      expect(updatedRow.text()).toContain('已撤回')
    } finally { confirmSpy.mockRestore() }
  })

  it('AI 草稿公告显示来源并可确认发布', async () => {
    freshDemo()
    const { wrapper } = await mountForum()
    const row = wrapper.findAll('[data-test="announcement-row"]').find((r) => r.text().includes('AI 生成·课程复习提醒'))!

    expect(row.text()).toContain('AI 草稿')
    expect(row.text()).toContain('尚未发布')
    await row.findAll('button').find((b) => b.text() === '确认发布')!.trigger('click')
    await settle(800)

    expect(db.announcements.find((item) => item.announcementId === '61004')!.status).toBe('PUBLISHED')
    const updated = wrapper.findAll('[data-test="announcement-row"]').find((r) => r.text().includes('AI 生成·课程复习提醒'))!
    expect(updated.text()).toContain('已发布')
  })
})
