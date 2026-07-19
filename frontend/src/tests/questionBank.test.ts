import { describe, expect, it } from 'vitest'
import QuestionBankPage from '@/domains/teacher/QuestionBankPage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

const mountQB = async () => {
  const mounted = await mountPage(QuestionBankPage, { path: '/teacher/exams' })
  await mounted.wrapper.find('select').setValue('21001')
  await settle(700)
  return mounted
}
const rowWith = (wrapper: Awaited<ReturnType<typeof mountQB>>['wrapper'], text: string) =>
  wrapper.findAll('tbody tr').find((tr) => tr.text().includes(text))

describe('考试题库工作台', () => {
  it('题库/题目：选中题库看题、新建题库、新建单选题（含正确项校验）', async () => {
    freshDemo()
    const { wrapper } = await mountQB()
    expect(wrapper.text()).toContain('Python 基础题库')
    await rowWith(wrapper, 'Python 基础题库')!.trigger('click')
    await settle()
    expect(wrapper.text()).toContain('下列哪种类型在 Python 中是不可变的？')
    // 新建题库
    await clickByText(wrapper, 'button', '新建题库')
    await wrapper.find('[data-test="bank-name"]').setValue('联调题库')
    await clickByText(wrapper, 'button', '保存题库')
    expect(wrapper.text()).toContain('联调题库')
    // 新建单选题：勾两个正确项时保存禁用
    await clickByText(wrapper, 'button', '新建题目')
    await wrapper.find('[data-test="question-stem"]').setValue('联调单选题：正确项是？')
    const contents = wrapper.findAll('[data-test="option-content"]')
    await contents[0].setValue('甲选项')
    await contents[1].setValue('乙选项')
    await contents[2].setValue('丙选项')
    const corrects = wrapper.findAll('[data-test="option-correct"]')
    await corrects[0].setValue(true)
    await corrects[1].setValue(true)
    const saveBtn = wrapper.findAll('button').find((b) => b.text() === '保存题目')!
    expect(saveBtn.attributes('disabled')).toBeDefined()
    await corrects[0].setValue(false)
    await clickByText(wrapper, 'button', '保存题目')
    expect(wrapper.text()).toContain('联调单选题：正确项是？')
  })

  it('考试：创建、组卷（总分校验）、发布', async () => {
    freshDemo()
    const { wrapper } = await mountQB()
    await clickByText(wrapper, 'button', '新建考试')
    await wrapper.find('[data-test="exam-title"]').setValue('联调考试')
    await wrapper.find('[data-test="exam-start"]').setValue('2026-08-01T09:00')
    await wrapper.find('[data-test="exam-end"]').setValue('2026-08-01T11:00')
    await wrapper.find('[data-test="exam-total"]').setValue(5)
    await clickByText(wrapper, 'button', '保存考试')
    const examRow = rowWith(wrapper, '联调考试')!
    expect(examRow.text()).toContain('DRAFT')
    // 组卷：选 52001（5 分）
    await examRow.findAll('button').find((b) => b.text() === '组卷')!.trigger('click')
    await settle()
    const pick = wrapper.findAll('[data-test="paper-pick"]').find((c) => c.element.closest('tr')!.textContent!.includes('不可变'))!
    await pick.setValue(true)
    await clickByText(wrapper, 'button', '保存试卷')
    // 发布试卷 → 考试进入 PUBLISHED
    const afterPaper = rowWith(wrapper, '联调考试')!
    await afterPaper.findAll('button').find((b) => b.text() === '发布试卷')!.trigger('click')
    await settle(900)
    expect(rowWith(wrapper, '联调考试')!.text()).toContain('PUBLISHED')
  })

  it('答卷：查看提交并为简答题评分', async () => {
    freshDemo()
    const { wrapper } = await mountQB()
    await rowWith(wrapper, 'Python 期中测验')!.findAll('button').find((b) => b.text() === '答卷')!.trigger('click')
    await settle()
    expect(wrapper.text()).toContain('刘子涵')
    await clickByText(wrapper, 'button', '阅卷')
    expect(wrapper.text()).toContain('列表推导式简洁高效')
    await wrapper.find('[data-test="grade-score"]').setValue(8)
    await wrapper.find('[data-test="grade-comment"]').setValue('思路正确，注意补充适用场景。')
    await clickByText(wrapper, 'button', '提交评分')
    await settle(900)
    expect(wrapper.text()).toContain('18')
    // 评语随评分落库，学生端按题展示。
    const attempt = db.attempts.find((item) => item.attemptId === '55001')!
    const shortAnswer = attempt.answers.find((answer) => answer.teacherComment)
    expect(shortAnswer?.teacherComment).toBe('思路正确，注意补充适用场景。')
  })
})
