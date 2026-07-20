import { afterEach, describe, expect, it, vi } from 'vitest'
import { AI_KEY_HEADER, AI_MODEL_HEADER, aiKeyHeader, clearAiKey, getAiKey, getAiModel, setAiKey, setAiModel } from '@/services/aiKey'
import { aiApi, notificationsApi } from '@/services/api'

describe('aiKey 存储与请求头', () => {
  afterEach(() => { localStorage.clear() })

  it('保存/读取/清除，去空白', () => {
    setAiKey('  sk-abc  ')
    expect(getAiKey()).toBe('sk-abc')
    clearAiKey()
    expect(getAiKey()).toBe('')
  })

  it('保存空白等同清除', () => {
    setAiKey('sk-x')
    setAiKey('   ')
    expect(getAiKey()).toBe('')
  })

  it('仅对 /api/v1/ai/ 路径注入请求头', () => {
    setAiKey('sk-1')
    expect(aiKeyHeader('/api/v1/ai/admin/status')).toEqual({ [AI_KEY_HEADER]: 'sk-1' })
    expect(aiKeyHeader('/api/v1/notifications/unread-count')).toEqual({})
  })

  it('无密钥时不注入任何头', () => {
    expect(aiKeyHeader('/api/v1/ai/admin/status')).toEqual({})
  })

  it('设置模型后 AI 路径附加 X-AI-Model；清空后不再附加', () => {
    setAiKey('sk-1')
    setAiModel('qwen-max')
    expect(getAiModel()).toBe('qwen-max')
    expect(aiKeyHeader('/api/v1/ai/admin/status')).toEqual({ [AI_KEY_HEADER]: 'sk-1', [AI_MODEL_HEADER]: 'qwen-max' })
    expect(aiKeyHeader('/api/v1/notifications')).toEqual({})
    setAiModel('')
    expect(aiKeyHeader('/api/v1/ai/admin/status')).toEqual({ [AI_KEY_HEADER]: 'sk-1' })
  })
})

describe('AI 请求头注入 · 真实模式', () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); localStorage.clear() })

  function ok(data: unknown) {
    return new Response(JSON.stringify({ code: 'SUCCESS', message: 'OK', data, traceId: 't' }), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    })
  }

  it('AI 请求携带 X-AI-Api-Key，非 AI 请求不携带', async () => {
    vi.stubEnv('VITE_API_MODE', 'real')
    setAiKey('sk-secret')
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(ok({ provider: 'dashscope', model: 'qwen-plus', available: true, mode: 'LLM', checkedAt: null }))
      .mockResolvedValueOnce(ok(0))
    vi.stubGlobal('fetch', fetchMock)

    await aiApi.adminStatus()
    await notificationsApi.unreadCount()

    const aiHeaders = fetchMock.mock.calls[0][1].headers as Record<string, string>
    const notifHeaders = fetchMock.mock.calls[1][1].headers as Record<string, string>
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/ai/admin/status')
    expect(aiHeaders[AI_KEY_HEADER]).toBe('sk-secret')
    expect(notifHeaders[AI_KEY_HEADER]).toBeUndefined()
  })
})
