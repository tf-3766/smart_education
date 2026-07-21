import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('notification drawer glass treatment', () => {
  it('styles the independent inbox overlay as a rounded liquid glass surface', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('.inbox-drawer {')
    expect(source).toContain('border-radius: 28px')
    expect(source).toContain('backdrop-filter: blur(20px) saturate(132%)')
    expect(source).toContain('.inbox-segmented {')
    expect(source).toContain('.inbox-message {')
    expect(source).toContain('border-radius: 16px')
  })
})
