import { RuntimeError, createTraceId } from './runtime'

// VITE_GATEWAY_URL 置空字符串时走同源相对路径（由 Vite 代理转发到网关），
// 供局域网多机共享；未设置时回退到本机网关地址。用 ?? 以区分“空串”与“未设置”。
export const gateway = import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:18080'
// 认证态（token + 缓存用户）存于 sessionStorage 而非 localStorage：
// sessionStorage 按标签页隔离，同一浏览器可在不同标签页登录不同用户互不干扰。
// 代价：关闭标签页即登出；同标签页内刷新（F5）仍保留。
export const TOKEN_STORAGE_KEY = 'smart-education-token'

// 「容错请求」计数：>0 期间收到的 403 视为业务可容忍（例如首页并发聚合里访问了已下线/越权
// 课程的子资源），不广播全局 forbidden 跳转，避免个别后台请求把整页跳飞到「无权访问」。
// 计数在请求发起前同步 +1、结算后 -1；fetch 必为异步，故 403 dispatch 时计数一定已就位。
let tolerantDepth = 0
export function beginTolerant(): void { tolerantDepth += 1 }
export function endTolerant(): void { tolerantDepth = Math.max(0, tolerantDepth - 1) }

/**
 * 以「容错」方式执行一个（或一组）请求：期间的 403 不触发全局跳转，异常时返回兜底值。
 * 用于首页概览等 best-effort 聚合——个别课程/资源越权不应让整页失败或跳飞。
 * 注意：传入的 promise 应在调用点就地创建（如 tolerant(api.x(), fb)），以保证计数早于响应结算。
 */
export async function tolerant<T>(promise: Promise<T>, fallback: T): Promise<T> {
  beginTolerant()
  try {
    return await promise
  } catch {
    return fallback
  } finally {
    endTolerant()
  }
}

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
      ...init.headers,
    },
  }).catch(() => { throw new RuntimeError('服务连接失败，请确认网关正在运行。', traceId, 'NETWORK_ERROR') })
  const payload = await response.json().catch(() => undefined) as { data?: T; message?: string; code?: string; traceId?: string } | undefined
  if (response.status === 401 || payload?.code === 'TOKEN_EXPIRED') {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    sessionStorage.removeItem('smart-education-session')
    window.dispatchEvent(new CustomEvent('smart-education:auth-expired', { detail: payload }))
  }
  if (response.status === 403 && tolerantDepth === 0) window.dispatchEvent(new CustomEvent('smart-education:forbidden', { detail: payload }))
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
