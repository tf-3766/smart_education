import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('admin dashboard announcement call to action', () => {
  it('uses the dedicated blue announcement call-to-action with icon and arrow', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/admin/AdminDashboardPage.vue'), 'utf8')

    expect(source).toContain('class="announcement-cta"')
    expect(source).toContain('class="announcement-cta-label"')
    expect(source).toContain('<Megaphone')
    expect(source).toContain('<ArrowRight')
    expect(source).toContain("router.push('/admin/content')")
  })
})
