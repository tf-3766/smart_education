import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('FrappeUI runtime plugin', () => {
  it('does not install the unused plugin that starts socket.io', () => {
    const main = readFileSync(resolve(process.cwd(), 'src/main.ts'), 'utf8')
    expect(main).not.toContain("from 'frappe-ui'")
    expect(main).not.toContain('app.use(FrappeUI')
  })
})