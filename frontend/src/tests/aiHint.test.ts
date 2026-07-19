import { describe, expect, it } from 'vitest'
import { aiErrorMessage } from '@/services/aiHint'

describe('aiErrorMessage', () => {
  it('透出后端返回的具体原因', () => {
    expect(aiErrorMessage(new Error('AI 模型尚未配置'))).toBe('AI 模型尚未配置')
  })

  it('未知错误回退通用提示', () => {
    expect(aiErrorMessage('weird')).toContain('暂不可用')
  })
})