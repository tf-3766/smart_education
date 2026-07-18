import { describe, expect, it, vi } from 'vitest'
import ExamAttemptPage from '@/domains/student/ExamAttemptPage.vue'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

const mountAttempt = () =>
  mountPage(ExamAttemptPage, { routePath: '/student/exams/:examId/attempt', path: '/student/exams/53001/attempt?title=Python 期中测验' })

describe('学生在线答题', () => {
  it('进入考试渲染题目并可作答交卷', async () => {
    freshDemo()
    const { wrapper } = await mountAttempt()
    await settle(700)
    expect(wrapper.text()).toContain('Python 期中测验')
    expect(wrapper.text()).toContain('下列哪种类型在 Python 中是不可变的？')
    // 单选 52001 选 B（元组）
    const radios = wrapper.findAll('input[type="radio"]')
    const optionB = radios.find((r) => (r.element as HTMLInputElement).value === 'B' && r.element.closest('section')!.textContent!.includes('不可变'))!
    await optionB.setValue(true)
    // 判断 52002 选 B（错误）
    const tfB = radios.find((r) => (r.element as HTMLInputElement).value === 'B' && r.element.closest('section')!.textContent!.includes('默认参数'))!
    await tfB.setValue(true)
    // 简答 52003
    await wrapper.find('textarea').setValue('列表推导式简洁，但嵌套过深影响可读性。')
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    await clickByText(wrapper, 'button', '交卷')
    await settle(900)
    confirmSpy.mockRestore()
    // 客观题即时判分：5 + 5 = 10；简答待教师评分
    expect(wrapper.text()).toContain('已提交')
    expect(wrapper.text()).toContain('10')
  })
})
