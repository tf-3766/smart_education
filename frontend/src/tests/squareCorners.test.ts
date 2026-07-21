import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('liquid-glass corner policy', () => {
  it('keeps box sizing global without forcing every component back to square corners', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toMatch(/\*,\s*\*::before,\s*\*::after,/s)
    expect(source).not.toMatch(/border-radius:\s*0\s*!important;/)
  })

  it('defines a rounded glass radius scale for workspace surfaces and controls', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toMatch(/--glass-radius-shell:\s*26px/)
    expect(source).toMatch(/--glass-radius-card:\s*18px/)
  })
})
