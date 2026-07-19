import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = () => readFileSync(resolve(process.cwd(), 'src/domains/account/ProfilePage.vue'), 'utf8')

describe('account profile page', () => {
  it('提供真实的个人中心：头像上传、修改密码，且退出登录内置于此', () => {
    const s = source()

    expect(s).toContain('个人中心')
    expect(s).not.toContain('权限信息')
    expect(s).not.toContain('移除头像')
    expect(s).toContain('返回工作台')
    // 退出登录内置在个人中心
    expect(s).toContain('退出登录')
    // 真实头像上传（接入 PUT /me/avatar）
    expect(s).toContain('type="file"')
    expect(s).toContain('updateAvatar')
    // 真实修改密码
    expect(s).toContain('修改密码')
    expect(s).toContain('changePassword')
  })

  it('去除冗余与内部字段：不再出现当前身份/账号来源/资料版本/未开放占位', () => {
    const s = source()
    expect(s).not.toContain('当前身份')
    expect(s).not.toContain('拥有角色')
    expect(s).not.toContain('账号来源')
    expect(s).not.toContain('资料版本')
    expect(s).not.toContain('未开放')
  })
})
