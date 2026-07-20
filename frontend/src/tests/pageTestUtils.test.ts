import { describe, expect, it } from 'vitest'
import CourseManagePage from '@/domains/teacher/CourseManagePage.vue'
import { freshDemo, mountPage } from './pageTestUtils'

describe('页面测试基建', () => {
  it('可挂载课程管理页并渲染演示数据', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    expect(wrapper.text()).toContain('Python 程序设计')
  })
})
