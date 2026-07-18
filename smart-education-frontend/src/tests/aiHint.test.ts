import { afterEach, describe, expect, it } from 'vitest'
import { aiErrorMessage } from '@/services/aiHint'

const AI_KEY = 'smart-education-ai-key'

describe('aiErrorMessage', () => {
  afterEach(() => localStorage.clear())

  it('未配置密钥时引导去 AI 设置', () => {
    localStorage.removeItem(AI_KEY)
    expect(aiErrorMessage(new Error('boom'))).toContain('AI 设置')
  })

  it('已配置密钥时透出后端具体原因', () => {
    localStorage.setItem(AI_KEY, 'sk-xxx')
    expect(aiErrorMessage(new Error('限流：请稍后再试'))).toBe('限流：请稍后再试')
  })

  it('已配置密钥但无消息时回退通用提示', () => {
    localStorage.setItem(AI_KEY, 'sk-xxx')
    expect(aiErrorMessage('weird')).toContain('暂不可用')
  })
})
