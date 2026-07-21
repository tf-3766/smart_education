import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('liquid-glass action buttons', () => {
  it('upgrades reusable text-link actions into compact floating glass controls', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('.liquid-glass-shell .text-link:not([aria-label="关闭"])')
    expect(source).toContain('min-height: 32px')
    expect(source).toContain('border-radius: 10px')
    expect(source).toContain('background: linear-gradient(145deg, rgba(248, 253, 255, .7)')
    expect(source).toContain('.liquid-glass-shell .cell-actions .text-link + .text-link')
    expect(source).toContain('.liquid-glass-shell .cell-actions { width: 1%; white-space: nowrap; }')
    expect(source).toContain('white-space: nowrap')
  })

  it('lets shared buttons and count chips grow to their full text width', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('.liquid-glass-shell .app-button:not(.announcement-cta)')
    expect(source).toContain('min-width: max-content')
    expect(source).toContain('min-height: 40px')
    expect(source).toContain('.liquid-glass-shell .panel-head .count,')
    expect(source).toContain('min-height: 30px')
  })

  it('renders user and review filters as clear nested glass panels', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('.liquid-glass-shell .tab-bar,')
    expect(source).toContain('.liquid-glass-shell .seg {')
    expect(source).toContain('border-radius: 18px !important')
    expect(source).toContain('.liquid-glass-shell .tab-bar .tab,')
    expect(source).toContain('.liquid-glass-shell .seg button.active')
    expect(source).toContain('min-height: 40px')
  })

  it('uses a full-width segmented glass control for user role filters', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toContain('width: min(100%, 520px)')
    expect(source).toContain('flex: 1 1 0')
    expect(source).toContain('.liquid-glass-shell .tab-bar .tab + .tab::before')
    expect(source).toContain('min-height: 38px')
    expect(source).toContain('.liquid-glass-shell .tab-bar .tab-active')
  })
})
