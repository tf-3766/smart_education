import { describe, expect, it } from 'vitest'
import StudentForumPage from '@/domains/student/StudentForumPage.vue'
import { db } from '@/services/api/demo/db'
import { freshDemo, mountPage, settle } from './pageTestUtils'

describe('学生课程讨论', () => {
  it('按课程展示教师、主题和主题下回复，并可继续回复', async () => {
    freshDemo()
    const { wrapper } = await mountPage(StudentForumPage, { path: '/student/forum' })
    await settle(900)

    expect(wrapper.text()).toContain('Python 程序设计')
    expect(wrapper.text()).toContain('李明老师')
    expect(wrapper.text()).toContain('列表和元组的区别是什么？')
    expect(wrapper.text()).toContain('元组不可变')

    const topic = wrapper.findAll('article.discussion-card').find((item) => item.text().includes('列表和元组的区别'))!
    await topic.find('input.input').setValue('我理解了，元组更适合表达固定结构。')
    await topic.findAll('button').find((button) => button.text() === '回复')!.trigger('click')
    await settle(800)

    expect(db.forumReplies.some((reply) => reply.topicId === '41001' && reply.content.includes('固定结构'))).toBe(true)
  })
})