import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = () => readFileSync(resolve(process.cwd(), 'src/layouts/AppShell.vue'), 'utf8')

describe('workspace sidebar (flat per-role nav)', () => {
  it('renders a flat nav of role route links', () => {
    expect(source()).toMatch(/<nav class="workspace-nav"[\s\S]*<RouterLink[\s\S]*v-for="item in navItems"/s)
  })

  it('drops the confusing multi-state drawer (secondary menu / collapse / route-synced detail)', () => {
    const s = source()
    expect(s).not.toContain('workspace-secondary-menu')
    expect(s).not.toContain('syncSidebarRoute')
    expect(s).not.toContain('collapseDetailSidebar')
    expect(s).not.toContain('收起')
  })

  it('highlights the active item, including matched sub-routes', () => {
    const s = source()
    expect(s).toMatch(/function isActive/)
    expect(s).toContain('matchPrefixes')
  })

  it('topbar 只保留个人中心入口，退出登录内置其中不再单列', () => {
    const s = source()
    expect(s).toContain('to="/account/profile"')
    expect(s).toContain('个人中心')
    // 退出登录已移入个人中心，顶栏不再有独立退出按钮
    expect(s).not.toContain('logout-link')
    expect(s).not.toContain('logoutRemote')
    expect(s).not.toContain('切换账号')
    expect(s).not.toContain('user-avatar')
  })

  it('keeps the account profile inside the same liquid-glass workspace shell', () => {
    expect(source()).toContain('student|teacher|admin|account')
  })

  it('groups identity, notification and account access in one glass status panel', () => {
    const s = source()
    expect(s).toContain('identity-glass-panel')
    expect(s).toContain('identity-role-card')
    expect(s).toContain('identity-notification-card')
    expect(s).toContain('identity-account-card')
    expect(s).toContain('<UserAvatar :file-id="session.backendUser?.avatarFileId"')
  })
})
