import { afterEach } from 'vitest'

// 认证态改存 sessionStorage（按标签页隔离）。逐用例清理，防止上一个用例
// 种下的 token/session 泄漏到下一个用例。localStorage 由各用例自行清理。
afterEach(() => {
  sessionStorage.clear()
})
