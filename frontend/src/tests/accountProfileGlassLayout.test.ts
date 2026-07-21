import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('account profile glass layout', () => {
  it('uses an identity card beside independent profile detail cards', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('.liquid-glass-shell .account-profile-layout')
    expect(source).toContain('grid-template-columns: minmax(230px, .78fr) minmax(0, 1.45fr)')
    expect(source).toContain('.liquid-glass-shell .account-identity-card')
    expect(source).toContain('.liquid-glass-shell .account-info-card')
    expect(source).toContain('border-radius: 28px')
  })
})
