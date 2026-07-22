import { describe, expect, it, vi } from 'vitest'
import CourseContentPage from '@/domains/teacher/CourseContentPage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

const mountContent = () =>
  mountPage(CourseContentPage, { routePath: '/teacher/courses/:courseId/content', path: '/teacher/courses/21001/content' })

function rowContaining(wrapper: Awaited<ReturnType<typeof mountContent>>['wrapper'], text: string) {
  const row = wrapper.findAll('tbody tr').find((tr) => tr.text().includes(text))
  if (!row) throw new Error(`找不到含「${text}」的表格行`)
  return row
}

describe('教师课程内容页：章节', () => {
  it('渲染课程与章节列表', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    expect(wrapper.text()).toContain('Python 程序设计')
    expect(wrapper.text()).toContain('第一章 Python 基础语法')
    expect(wrapper.text()).toContain('第二章 函数与模块')
  })

  it('可新建章节并出现在列表', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    await clickByText(wrapper, 'button', '新建章节')
    await wrapper.find('[data-test="chapter-title"]').setValue('第三章 联调新增')
    await clickByText(wrapper, 'button', '保存章节')
    expect(wrapper.text()).toContain('第三章 联调新增')
  })

  it('选中章节可管理课时：渲染、新建、发布', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    await rowContaining(wrapper, '第一章 Python 基础语法').trigger('click')
    await settle()
    expect(wrapper.text()).toContain('变量与数据类型')
    await clickByText(wrapper, 'button', '新建课时')
    await wrapper.find('[data-test="lesson-title"]').setValue('联调新课时')
    await wrapper.find('[data-test="lesson-content"]').setValue('# 内容')
    await clickByText(wrapper, 'button', '保存课时')
    const lessonRow = rowContaining(wrapper, '联调新课时')
    expect(lessonRow.text()).toContain('草稿')
    await lessonRow.findAll('button').find((b) => b.text() === '发布')!.trigger('click')
    await settle()
    expect(rowContaining(wrapper, '联调新课时').text()).toContain('已发布')
  })

  it('资料面板：渲染、新增外链、删除', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    expect(wrapper.text()).toContain('讲义《Python 基础》')
    await clickByText(wrapper, 'button', '新增资料')
    await wrapper.find('[data-test="material-name"]').setValue('参考链接')
    await wrapper.find('[data-test="material-url"]').setValue('https://example.com/ref')
    await clickByText(wrapper, 'button', '保存资料')
    expect(wrapper.text()).toContain('参考链接')
    // 回归防护：归属范围须为后端 MaterialVisibility 合法枚举（COURSE），
    // 不能再发送导致 PARAM_VALIDATION_ERROR 的 ENROLLED_ONLY。
    expect(db.materials.find((m) => m.name === '参考链接')?.visibility).toBe('COURSE')
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const row = rowContaining(wrapper, '参考链接')
    await row.findAll('button').find((b) => b.text() === '删除')!.trigger('click')
    await settle(900)
    confirmSpy.mockRestore()
    expect(wrapper.text()).not.toContain('参考链接')
  })

  it('可编辑已发布资料并重新发布', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    const publishedRow = rowContaining(wrapper, '讲义《Python 基础》')
    await publishedRow.findAll('button').find((button) => button.text() === '编辑')!.trigger('click')
    await settle()
    await wrapper.find('[data-test="material-name"]').setValue('讲义《Python 基础》修订版')
    await wrapper.find('[data-test="material-visibility"]').setValue('COURSE')
    await clickByText(wrapper, 'button', '保存资料')

    expect(rowContaining(wrapper, '讲义《Python 基础》修订版').text()).toContain('草稿')
    const draftRow = rowContaining(wrapper, '讲义《Python 基础》修订版')
    await draftRow.findAll('button').find((button) => button.text() === '发布')!.trigger('click')
    await settle()
    expect(rowContaining(wrapper, '讲义《Python 基础》修订版').text()).toContain('已发布')
  })

  it('可下线已发布章节', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    const row = rowContaining(wrapper, '第一章 Python 基础语法')
    const offline = row.findAll('button').find((b) => b.text() === '下线')
    expect(offline).toBeTruthy()
    await offline!.trigger('click')
    await settle()
    expect(rowContaining(wrapper, '第一章 Python 基础语法').text()).toContain('已下线')
  })

  it('AI 摘要位于已保存课时旁，并基于课时说明和资料生成独立草稿', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    await rowContaining(wrapper, '第一章 Python 基础语法').trigger('click')
    await settle()

    await clickByText(wrapper, 'button', '新建课时')
    expect(wrapper.get('[role="dialog"]').text()).not.toContain('AI 摘要')
    await clickByText(wrapper, 'button', '取消')

    const lessonRow = rowContaining(wrapper, '变量与数据类型')
    await lessonRow.findAll('button').find((button) => button.text() === 'AI 摘要')!.trigger('click')
    await settle(500)
    expect(wrapper.text()).toContain('AI 课时摘要草稿')
    expect(wrapper.text()).toContain('课时说明和已发布资料')
    expect(wrapper.text()).not.toContain('采用为课时内容')
  })

  it('生成六步骤教学包计划并交给全局助手逐步执行', async () => {
    freshDemo()
    const { wrapper } = await mountContent()
    expect(wrapper.text()).toContain('一键规划课程教学包')
    expect(wrapper.text()).toContain('题库题目')

    await clickByText(wrapper, 'button', '生成执行计划')
    await settle(500)
    expect(wrapper.text()).toContain('AI 教学包执行计划')

    const received = vi.fn()
    window.addEventListener('smart-education:ai-compose', received, { once: true })
    await clickByText(wrapper, 'button', '交给 AI 助手逐步执行')
    expect(received).toHaveBeenCalledOnce()
  })
})
