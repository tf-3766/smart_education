import { describe, expect, it } from 'vitest'
import AiSettingsPage from '@/domains/admin/AiSettingsPage.vue'
import { getAiKey } from '@/services/aiKey'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

describe('管理员 AI 设置页', () => {
  it('展示服务状态、保存与清除密钥', async () => {
    freshDemo()
    const { wrapper } = await mountPage(AiSettingsPage, { path: '/admin/ai' })
    await settle(400)
    expect(wrapper.text()).toContain('服务状态')
    expect(wrapper.text()).toContain('大模型密钥')

    await wrapper.get('#ai-key').setValue('sk-my-key-1234')
    await clickByText(wrapper, 'button', '保存')
    await settle(300)
    expect(getAiKey()).toBe('sk-my-key-1234')
    expect(wrapper.text()).toContain('已保存')

    await clickByText(wrapper, 'button', '清除')
    await settle(200)
    expect(getAiKey()).toBe('')
  })

  it('密钥输入框默认脱敏（password）', async () => {
    freshDemo()
    const { wrapper } = await mountPage(AiSettingsPage, { path: '/admin/ai' })
    await settle(300)
    expect(wrapper.get('#ai-key').attributes('type')).toBe('password')
  })

  it('服务状态的提供方/模式显示为中文而非原始枚举码', async () => {
    freshDemo()
    const { wrapper } = await mountPage(AiSettingsPage, { path: '/admin/ai' })
    await settle(400)
    // demo adminStatus 返回 provider=fallback, mode=FRAMEWORK_ONLY
    expect(wrapper.text()).toContain('未接入（占位回退）')
    expect(wrapper.text()).toContain('仅框架（未接模型）')
    expect(wrapper.text()).not.toContain('FRAMEWORK_ONLY')
  })
})
