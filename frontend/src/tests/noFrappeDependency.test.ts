import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('frontend dependency boundary', () => {
  it('keeps frappe-ui out while preserving local font assets', () => {
    const root = process.cwd()
    const main = readFileSync(resolve(root, 'src/main.ts'), 'utf8')
    const packageJson = readFileSync(resolve(root, 'package.json'), 'utf8')
    const tailwind = readFileSync(resolve(root, 'tailwind.config.js'), 'utf8')

    expect(main).not.toContain('frappe-ui')
    expect(packageJson).not.toContain('"frappe-ui"')
    expect(tailwind).not.toContain('frappe-ui')
    expect(existsSync(resolve(root, 'src/assets/fonts/Inter.var.woff2'))).toBe(true)
    expect(existsSync(resolve(root, 'src/assets/fonts/Inter-Italic.var.woff2'))).toBe(true)
  })
})
