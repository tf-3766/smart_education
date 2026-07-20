// 本项目后端是自研网关（:18080），没有 Frappe 服务，
// FrappeUI 插件默认会 initSocket 连 :9000/socket.io 并无限重试轮询。
// 约束：安装 FrappeUI 时必须显式关闭 socketio。
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('FrappeUI socketio', () => {
  it('main.ts 安装 FrappeUI 时禁用 socketio', () => {
    const main = readFileSync(resolve(process.cwd(), 'src/main.ts'), 'utf8')
    expect(main).toMatch(/app\.use\(FrappeUI,\s*\{[^}]*socketio:\s*false/)
  })
})
