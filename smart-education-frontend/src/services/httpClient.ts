import { RuntimeError, createTraceId } from './runtime'
import { aiKeyHeader } from './aiKey'

// VITE_GATEWAY_URL 置空字符串时走同源相对路径（由 Vite 代理转发到网关），
// 供局域网多机共享；未设置时回退到本机网关地址。用 ?? 以区分“空串”与“未设置”。
export const gateway = import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:18080'
// 认证态（token + 缓存用户）存于 sessionStorage 而非 localStorage：
// sessionStorage 按标签页隔离，同一浏览器可在不同标签页登录不同用户互不干扰。
// 代价：关闭标签页即登出；同标签页内刷新（F5）仍保留。
export const TOKEN_STORAGE_KEY = 'smart-education-token'

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const traceId = createTraceId()
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)
  const isForm = init.body instanceof FormData
  const response = await fetch(`${gateway}${path}`, {
    ...init,
    headers: {
      ...(isForm ? {} : { 'Content-Type': 'application/json' }),
      'X-Trace-Id': traceId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...aiKeyHeader(path),
      ...init.headers,
    },
  }).catch(() => { throw new RuntimeError('服务连接失败，请确认网关正在运行。', traceId, 'NETWORK_ERROR') })
  const payload = await response.json().catch(() => undefined) as { data?: T; message?: string; code?: string; traceId?: string } | undefined
  if (response.status === 401 || payload?.code === 'TOKEN_EXPIRED') {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    sessionStorage.removeItem('smart-education-session')
    window.dispatchEvent(new CustomEvent('smart-education:auth-expired', { detail: payload }))
  }
  if (response.status === 403) window.dispatchEvent(new CustomEvent('smart-education:forbidden', { detail: payload }))
  if (!response.ok) {
    throw new RuntimeError(
      payload?.message || '请求失败，请稍后重试。',
      payload?.traceId || traceId,
      payload?.code || `HTTP_${response.status}`,
      response.status,
    )
  }
  return (payload?.data ?? payload) as T
}
