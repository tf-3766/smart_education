import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('application routing shell', () => {
  it('lets the router own role shells instead of nesting AppShell twice', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/App.vue'), 'utf8')

    expect(source).toContain('<RouterView')
    expect(source).not.toContain('<AppShell')
  })

  it('exposes a signed-in account profile route inside the shared shell', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/router/index.ts'), 'utf8')

    expect(source).toContain("path: '/account'")
    expect(source).toContain("path: 'profile'")
    expect(source).toContain("@/domains/account/ProfilePage.vue")
  })
})
