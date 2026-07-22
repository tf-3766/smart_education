import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearRequestCache, TOKEN_STORAGE_KEY, request, tolerant } from '@/services/httpClient'
import { RuntimeError } from '@/services/runtime'

describe('real HTTP client', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    clearRequestCache()
    vi.stubEnv('VITE_GATEWAY_URL', 'http://localhost:18080')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  it('sends JWT and a trace id and unwraps ApiResponse data', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'SUCCESS',
      message: 'OK',
      data: { value: 7 },
      traceId: 'server-trace',
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(request<{ value: number }>('/api/v1/test')).resolves.toEqual({ value: 7 })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers.Authorization).toBe('Bearer jwt-token')
    expect(init.headers['X-Trace-Id']).toMatch(/^trace-/)
  })

  it('returns cached real GET data immediately and refreshes it in the background', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ data: { value: 1 } }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ data: { value: 2 } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(request<{ value: number }>('/api/v1/student/courses')).resolves.toEqual({ value: 1 })
    await expect(request<{ value: number }>('/api/v1/student/courses')).resolves.toEqual({ value: 1 })
    await vi.waitFor(async () => {
      await expect(request<{ value: number }>('/api/v1/student/courses')).resolves.toEqual({ value: 2 })
    })
  })

  it('invalidates cached reads after a successful write', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ data: { value: 1 } }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ data: { ok: true } }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ data: { value: 2 } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await request('/api/v1/student/courses')
    await request('/api/v1/student/courses/1/enroll', { method: 'POST' })
    await expect(request<{ value: number }>('/api/v1/student/courses')).resolves.toEqual({ value: 2 })
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('clears auth and emits an expiry event on 401', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'expired-token')
    const listener = vi.fn()
    window.addEventListener('smart-education:auth-expired', listener)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'TOKEN_EXPIRED', message: '登录已过期', traceId: 'trace-expired',
    }), { status: 401, headers: { 'Content-Type': 'application/json' } })))

    await expect(request('/api/v1/auth/me')).rejects.toMatchObject({ code: 'TOKEN_EXPIRED', traceId: 'trace-expired' })
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener('smart-education:auth-expired', listener)
  })

  it('emits a forbidden event on 403', async () => {
    const listener = vi.fn()
    window.addEventListener('smart-education:forbidden', listener)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'FORBIDDEN', message: '无权访问', traceId: 'trace-forbidden',
    }), { status: 403, headers: { 'Content-Type': 'application/json' } })))

    await expect(request('/api/v1/admin/users')).rejects.toBeInstanceOf(RuntimeError)
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener('smart-education:forbidden', listener)
  })

  it('tolerant() 包裹的 403 不广播 forbidden，且返回兜底值（不把整页跳飞）', async () => {
    const listener = vi.fn()
    window.addEventListener('smart-education:forbidden', listener)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'FORBIDDEN', message: '无权访问', traceId: 'trace-tolerant',
    }), { status: 403, headers: { 'Content-Type': 'application/json' } })))

    // 就地创建请求再交给 tolerant——与适配器里 settled(api.x(), fb) 的用法一致
    const value = await tolerant(request('/api/v1/student/courses/9/progress'), { fallback: true })
    expect(value).toEqual({ fallback: true })
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener('smart-education:forbidden', listener)
  })

  it('preserves conflict code and trace id for reload handling', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'RESOURCE_CONFLICT', message: '版本已变化', traceId: 'trace-conflict',
    }), { status: 409, headers: { 'Content-Type': 'application/json' } })))

    await expect(request('/api/v1/teacher/courses/101', { method: 'PUT' })).rejects.toMatchObject({
      code: 'RESOURCE_CONFLICT',
      traceId: 'trace-conflict',
      status: 409,
    })
  })

  it('wraps network failures without falling back to demo data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('connection refused')))
    await expect(request('/api/v1/student/courses')).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
  })
})
