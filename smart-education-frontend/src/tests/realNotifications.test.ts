import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { notificationsApi } from '@/services/api'

function success(data: unknown) {
  return new Response(JSON.stringify({ code: 'SUCCESS', message: 'OK', data, traceId: 'trace-notification' }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('real notifications API', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'real')
    localStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  it('uses the notification list and unread-count contracts', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(success({ records: [], page: 2, size: 10, total: 0, totalPages: 0 }))
      .mockResolvedValueOnce(success(7))
    vi.stubGlobal('fetch', fetchMock)

    await expect(notificationsApi.list({ page: 2, size: 10, category: 'COURSE', unread: true }))
      .resolves.toMatchObject({ page: 2, size: 10 })
    await expect(notificationsApi.unreadCount()).resolves.toBe(7)

    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/notifications?page=2&size=10&category=COURSE&unread=true')
    expect(fetchMock.mock.calls[1][0]).toContain('/api/v1/notifications/unread-count')
  })

  it('uses POST for single and batch read receipts', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(success(null))
      .mockResolvedValueOnce(success(null))
    vi.stubGlobal('fetch', fetchMock)

    await notificationsApi.markRead('991')
    await notificationsApi.markAllRead()

    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/notifications/991/read')
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(fetchMock.mock.calls[1][0]).toContain('/api/v1/notifications/read-all')
    expect(fetchMock.mock.calls[1][1].method).toBe('POST')
  })

  it('uses archive and preference persistence contracts', async () => {
    const preferences = { enabledCategories: ['COURSE', 'SYSTEM'] as ('COURSE' | 'SYSTEM')[] }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(success(null))
      .mockResolvedValueOnce(success(preferences))
      .mockResolvedValueOnce(success(preferences))
    vi.stubGlobal('fetch', fetchMock)

    await notificationsApi.archive('992')
    await expect(notificationsApi.preferences()).resolves.toEqual(preferences)
    await expect(notificationsApi.updatePreferences(preferences)).resolves.toEqual(preferences)

    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/notifications/992/archive')
    expect(fetchMock.mock.calls[1][0]).toContain('/api/v1/notifications/preferences')
    expect(fetchMock.mock.calls[2][1].method).toBe('PUT')
  })

  it('receives authenticated SSE notification events', async () => {
    sessionStorage.setItem('smart-education-token', 'stream-token')
    const payload = { type: 'created', notificationId: '993', timestamp: '2026-07-14T07:00:00Z' }
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode(`event: notification\ndata: ${JSON.stringify(payload)}\n\n`))
        controller.close()
      },
    })
    const fetchMock = vi.fn().mockResolvedValue(new Response(stream, {
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' },
    }))
    vi.stubGlobal('fetch', fetchMock)

    let stop: () => void = () => {}
    const event = await new Promise((resolve) => {
      stop = notificationsApi.subscribe((received) => {
        if (received.type === 'created') {
          resolve(received)
          stop()
        }
      })
    })

    expect(event).toEqual(payload)
    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/notifications/stream')
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer stream-token')
  })

  it('does not destroy a valid session when the SSE stream returns 401', async () => {
    sessionStorage.setItem('smart-education-token', 'stream-token')
    sessionStorage.setItem('smart-education-session', '{}')
    const authExpired = vi.fn()
    window.addEventListener('smart-education:auth-expired', authExpired)
    const fetchMock = vi.fn().mockResolvedValue(new Response('', { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    await new Promise<void>((resolve) => {
      const stop = notificationsApi.subscribe(
        () => undefined,
        () => { stop(); resolve() },
      )
    })

    expect(authExpired).not.toHaveBeenCalled()
    expect(sessionStorage.getItem('smart-education-token')).toBe('stream-token')
    window.removeEventListener('smart-education:auth-expired', authExpired)
  })
})
