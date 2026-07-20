import { describe, expect, it } from 'vitest'
import StudentAssignmentsPage from '@/domains/student/StudentAssignmentsPage.vue'
import { freshDemo, mountPage, settle } from './pageTestUtils'

describe('学生学习任务', () => {
  it('已提交作业可展开查看，草稿作业可继续编辑', async () => {
    freshDemo()
    const { wrapper } = await mountPage(StudentAssignmentsPage, { path: '/student/assignments' })
    await settle(700)
    expect(wrapper.text()).toContain('第一章课后练习')
    expect(wrapper.text()).toContain('第二章编程作业')

    const submitted = wrapper.findAll('section.panel').find((panel) => panel.text().includes('第一章课后练习'))!
    await submitted.findAll('button').find((button) => button.text() === '查看作业')!.trigger('click')
    expect(submitted.text()).toContain('我的提交')
    expect(submitted.find('textarea').exists()).toBe(false)

    const draft = wrapper.findAll('section.panel').find((panel) => panel.text().includes('第二章编程作业'))!
    await draft.findAll('button').find((button) => button.text() === '查看作业')!.trigger('click')
    expect((draft.find('textarea').element as HTMLTextAreaElement).value).toContain('函数骨架')
  })

  it('草稿作业可正式提交，提交后仍能查看自己的回答', async () => {
    freshDemo()
    const { wrapper } = await mountPage(StudentAssignmentsPage, { path: '/student/assignments' })
    await settle(700)
    const draft = wrapper.findAll('section.panel').find((panel) => panel.text().includes('第二章编程作业'))!
    await draft.findAll('button').find((button) => button.text() === '查看作业')!.trigger('click')
    await draft.find('textarea').setValue('已完成：函数与测试均通过。')
    const submitButton = draft.findAll('button').find((button) => button.text() === '确认提交')!
    expect(submitButton.attributes('disabled')).toBeUndefined()
    await submitButton.trigger('click')
    await settle(1200)


    const after = wrapper.findAll('section.panel').find((panel) => panel.text().includes('第二章编程作业'))!
    expect(after.text()).toContain('已提交')
    expect(after.text()).toContain('已完成：函数与测试均通过。')
    expect(after.find('textarea').exists()).toBe(false)
  })
})