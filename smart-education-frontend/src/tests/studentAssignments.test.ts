import { describe, expect, it } from 'vitest'
import StudentAssignmentsPage from '@/domains/student/StudentAssignmentsPage.vue'
import { freshDemo, mountPage, settle } from './pageTestUtils'

describe('学生学习任务', () => {
  it('渲染任务、草稿可编辑、已提交作业锁定', async () => {
    freshDemo()
    const { wrapper } = await mountPage(StudentAssignmentsPage, { path: '/student/assignments' })
    await settle(700)
    expect(wrapper.text()).toContain('第一章课后练习')
    expect(wrapper.text()).toContain('第二章编程作业')
    const panels = wrapper.findAll('section.panel')
    const submitted = panels.find((p) => p.text().includes('第一章课后练习'))!
    expect(submitted.find('textarea').attributes('disabled')).toBeDefined()
    const draft = panels.find((p) => p.text().includes('第二章编程作业'))!
    expect((draft.find('textarea').element as HTMLTextAreaElement).value).toContain('函数骨架')
  })

  it('草稿作业可正式提交并锁定', async () => {
    freshDemo()
    const { wrapper } = await mountPage(StudentAssignmentsPage, { path: '/student/assignments' })
    await settle(700)
    const draft = wrapper.findAll('section.panel').find((p) => p.text().includes('第二章编程作业'))!
    await draft.find('textarea').setValue('已完成：函数与测试均通过。')
    await draft.findAll('button').find((b) => b.text() === '确认提交')!.trigger('click')
    await settle(1200)
    const after = wrapper.findAll('section.panel').find((p) => p.text().includes('第二章编程作业'))!
    expect(after.text()).toContain('已提交')
    expect(after.find('textarea').attributes('disabled')).toBeDefined()
  })
})
