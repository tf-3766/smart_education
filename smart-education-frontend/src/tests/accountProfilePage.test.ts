import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = () => readFileSync(resolve(process.cwd(), 'src/domains/account/ProfilePage.vue'), 'utf8')

describe('account profile page', () => {
  it('provides a real profile surface without fake avatar or password forms', () => {
    const s = source()

    expect(s).toContain('个人中心')
    expect(s).toContain('账号状态')
    expect(s).toContain('权限信息')
    expect(s).toContain('返回工作台')
    expect(s).toContain('退出登录')
    expect(s).toContain('未开放')
    expect(s).not.toContain('type="file"')
    expect(s).not.toContain('type="password"')
    expect(s).not.toContain('updateAvatar')
  })
})
