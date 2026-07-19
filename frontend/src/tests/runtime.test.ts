import { describe, expect, it } from 'vitest'
import { getApiMode, RuntimeError } from '@/services/runtime'

describe('runtime mode', () => {
  it('uses demo data unless real mode is explicitly configured', () => {
    expect(getApiMode({})).toBe('demo')
    expect(getApiMode({ VITE_API_MODE: 'real' })).toBe('real')
  })

  it('exposes a trace id without leaking internal error detail', () => {
    const error = new RuntimeError('AI 服务暂不可用，请稍后重试。', 'trace-demo-1')
    expect(error.traceId).toBe('trace-demo-1')
    expect(error.message).toContain('暂不可用')
  })
})
