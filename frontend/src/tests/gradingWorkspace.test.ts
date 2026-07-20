import { describe, expect, it } from 'vitest'
import GradingWorkspacePage from '@/domains/teacher/GradingWorkspacePage.vue'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

const mountGrading = async () => {
  const mounted = await mountPage(GradingWorkspacePage, { path: '/teacher/assignments' })
  await mounted.wrapper.find('select').setValue('21001')
  await settle(700)
  await mounted.wrapper.findAll('select')[1].setValue('31001')
  await settle(700)
  return mounted
}

async function openRoster(wrapper: Awaited<ReturnType<typeof mountGrading>>['wrapper']) {
  const assignmentRow = wrapper.findAll('[data-test="assignment-row"]').find((row) => row.text().includes('第一章课后练习'))!
  await assignmentRow.findAll('button').find((button) => button.text() === '查看提交')!.trigger('click')
  await settle(700)
}

describe('作业批改工作台', () => {
  it('渲染课程作业与提交列表', async () => {
    freshDemo()
    const { wrapper } = await mountGrading()
    expect(wrapper.text()).toContain('第一章课后练习')
    await openRoster(wrapper)
    expect(wrapper.text()).toContain('王一诺')
  })

  it('可新建作业并发布', async () => {
    freshDemo()
    const { wrapper } = await mountGrading()
    await clickByText(wrapper, 'button', '新建作业')
    await wrapper.find('[data-test="assignment-title"]').setValue('联调新作业')
    await wrapper.find('[data-test="assignment-due"]').setValue('2026-08-01T12:00')
    await clickByText(wrapper, 'button', '保存作业')
    const row = wrapper.findAll('[data-test="assignment-row"]').find((tr) => tr.text().includes('联调新作业'))
    expect(row).toBeTruthy()
    expect(row!.text()).toContain('草稿')
    await row!.findAll('button').find((b) => b.text() === '发布')!.trigger('click')
    await settle(900)
    const published = wrapper.findAll('[data-test="assignment-row"]').find((tr) => tr.text().includes('联调新作业'))
    expect(published!.text()).toContain('已发布')
  })

  it('可编辑已有作业并保留其余字段', async () => {
    freshDemo()
    const { wrapper } = await mountGrading()
    const row = wrapper.findAll('[data-test="assignment-row"]').find((tr) => tr.text().includes('第一章课后练习'))!
    await row.findAll('button').find((b) => b.text() === '编辑')!.trigger('click')
    await settle(300)

    // 弹窗为编辑态并回填标题。
    expect(wrapper.text()).toContain('编辑作业')
    expect((wrapper.find('[data-test="assignment-title"]').element as HTMLInputElement).value).toBe('第一章课后练习')
    await wrapper.find('[data-test="assignment-title"]').setValue('第一章课后练习（修订）')
    await clickByText(wrapper, 'button', '保存修改')
    await settle(900)

    const updated = wrapper.findAll('[data-test="assignment-row"]').find((tr) => tr.text().includes('第一章课后练习（修订）'))
    expect(updated).toBeTruthy()
    expect(wrapper.text()).toContain('作业已更新')
  })

  it('批改提交并发布成绩', async () => {
    freshDemo()
    const { wrapper } = await mountGrading()
    await openRoster(wrapper)
    const row = wrapper.findAll('[data-test="roster-item"]').find((item) => item.text().includes('王一诺'))!
    await row.findAll('button').find((b) => b.text().includes('批改'))!.trigger('click')
    await settle()
    await wrapper.find('input[type="number"]').setValue(95)
    await wrapper.find('textarea').setValue('完成质量很好')
    await clickByText(wrapper, 'button', '评分并发布')
    await settle(900)
    const graded = wrapper.findAll('[data-test="roster-item"]').find((item) => item.text().includes('王一诺'))!
    expect(graded.text()).toContain('95')
    expect(graded.text()).toContain('已发布')
  })

  it('批改弹窗可生成 AI 评语草稿并采用到评语', async () => {
    freshDemo()
    const { wrapper } = await mountGrading()
    await openRoster(wrapper)
    const row = wrapper.findAll('[data-test="roster-item"]').find((item) => item.text().includes('王一诺'))!
    await row.findAll('button').find((b) => b.text().includes('批改'))!.trigger('click')
    await settle()

    await clickByText(wrapper, 'button', 'AI 评语草稿')
    await settle(400)
    expect(wrapper.text()).toContain('AI 评语草稿')

    await clickByText(wrapper, 'button', '采用到评语')
    await settle(100)
    const textarea = wrapper.get('#gw-grade-comment').element as HTMLTextAreaElement
    expect(textarea.value.length).toBeGreaterThan(0)
  })
})
