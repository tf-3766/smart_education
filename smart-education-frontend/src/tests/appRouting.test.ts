import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('application routing shell', () => {
  it('lets the router own role shells instead of nesting AppShell twice', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/App.vue'), 'utf8')

    expect(source).toContain('<RouterView')
    expect(source).not.toContain('<AppShell')
  })
})
