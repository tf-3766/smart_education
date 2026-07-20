// 契约 API 的请求助手：查询串拼接、JSON 动词、multipart 与 SSE。
// 真实模式经 httpClient 走网关；演示模式由各域模块直接落到本地演示库。
import { TOKEN_STORAGE_KEY, gateway, request } from '../httpClient'
import { RuntimeError, createTraceId, getApiMode } from '../runtime'
import type { AiStreamEvent } from './types'

export { TOKEN_STORAGE_KEY }

export function isRealMode(): boolean {
  return getApiMode() === 'real'
}

export function buildQuery(params?: Record<string, unknown>): string {
  if (!params) return ''
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    if (Array.isArray(value)) value.forEach((item) => search.append(key, String(item)))
    else search.set(key, String(value))
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

export function get<T>(path: string, params?: Record<string, unknown>, headers?: Record<string, string>): Promise<T> {
  return request<T>(`${path}${buildQuery(params)}`, headers ? { headers } : undefined)
}

export function post<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
}

export function put<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) })
}

export function patch<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, { method: 'PATCH', body: body === undefined ? undefined : JSON.stringify(body) })
}

export function del<T = void>(path: string): Promise<T> {
  return request<T>(path, { method: 'DELETE' })
}

export function upload<T>(path: string, form: FormData): Promise<T> {
  return request<T>(path, { method: 'POST', body: form })
}

/** 文件流地址（GET /files/{id}/content 不用 ApiResponse 包裹，直接作为链接使用）。 */
export function fileContentUrl(fileId: string): string {
  return `${gateway}/api/v1/files/${fileId}/content`
}

/**
 * 解析 text/event-stream。事件固定为 meta/delta/citation/done/error，
 * 数据体为 AiStreamEvent JSON；收到 done/error 或流结束时返回。
 */
export async function postEventStream(path: string, body: unknown, onEvent: (event: AiStreamEvent) => void): Promise<void> {
  const traceId = createTraceId()
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)
  const response = await fetch(`${gateway}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      'X-Trace-Id': traceId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  }).catch(() => { throw new RuntimeError('服务连接失败，请确认网关正在运行。', traceId, 'NETWORK_ERROR') })
  if (!response.ok || !response.body) {
    const payload = await response.json().catch(() => undefined) as { message?: string; code?: string; traceId?: string } | undefined
    throw new RuntimeError(payload?.message || 'AI 服务暂不可用，请稍后重试。', payload?.traceId || traceId, payload?.code || `HTTP_${response.status}`)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const chunk = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const dataLines = chunk.split('\n').filter((line) => line.startsWith('data:'))
      if (dataLines.length) {
        const raw = dataLines.map((line) => line.slice(5).trim()).join('\n')
        try {
          onEvent(JSON.parse(raw) as AiStreamEvent)
        } catch {
          // 忽略无法解析的心跳片段
        }
      }
      boundary = buffer.indexOf('\n\n')
    }
  }
}
