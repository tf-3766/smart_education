import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('square corner policy', () => {
  it('forces every element and pseudo-element to use square corners', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toMatch(/\*,\s*\*::before,\s*\*::after,/s)
    expect(source).toMatch(/border-radius:\s*0\s*!important;/)
  })

  it('outprioritizes component utility classes rendered inside the page body', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(source).toMatch(/:root\s+body\s+\*,\s*:root\s+body\s+\*::before,\s*:root\s+body\s+\*::after\s*\{/s)
  })
})
