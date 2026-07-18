// 回归：预警的 warningStatus/warningLevel 是 {code,label} 对象，页面曾误与字符串比较，
// 导致「待处理预警」恒为 0、预警事项从教学事项列表消失。
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import TeacherDashboardPage from '@/domains/teacher/TeacherDashboardPage.vue'
import { freshDemo, settle } from './pageTestUtils'

describe('教学工作台 · 预警统计', () => {
  it('待处理预警计数与高风险事项按状态码正确渲染', async () => {
    freshDemo()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: TeacherDashboardPage }],
    })
    await router.push('/teacher/dashboard')
    await router.isReady()
    const wrapper = mount(TeacherDashboardPage, { global: { plugins: [router, createPinia()], stubs: { teleport: true } } })
    await settle(800)

    // 演示库：43001（OPEN/MEDIUM）与 43002（OPEN/HIGH）待处理，43003 已处理不计入。
    const metric = wrapper.findAll('.kpi').find((node) => node.text().includes('待处理预警'))!
    expect(metric.find('.kpi-value').text()).toBe('2')
    expect(wrapper.text()).toContain('连续缺交 2 次作业')
    expect(wrapper.text()).toContain('高风险')
    expect(wrapper.text()).not.toContain('一次测验得分低于 60 分')
  })
})
