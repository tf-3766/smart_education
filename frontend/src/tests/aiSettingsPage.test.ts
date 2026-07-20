import { describe, expect, it } from 'vitest'
import AiSettingsPage from '@/domains/admin/AiSettingsPage.vue'
import { freshDemo, mountPage, settle } from './pageTestUtils'

describe('管理员 AI 设置页', () => {
  it('展示后端真实服务状态与服务器端配置说明', async () => {
    freshDemo()
    const { wrapper } = await mountPage(AiSettingsPage, { path: '/admin/ai' })
    await settle(400)

    expect(wrapper.text()).toContain('服务状态')
    expect(wrapper.text()).toContain('未接入（框架回退）')
    expect(wrapper.text()).toContain('密钥由后端统一管理')
    expect(wrapper.text()).toContain('DASHSCOPE_API_KEY')
    expect(wrapper.find('#ai-key').exists()).toBe(false)
  })
})