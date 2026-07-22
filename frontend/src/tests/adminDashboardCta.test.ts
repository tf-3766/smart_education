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

  it('renders the operations brief and hands confirmed planning to the assistant', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/admin/AdminDashboardPage.vue'), 'utf8')

    expect(source).toContain('aiApi.adminOperationsBrief')
    expect(source).toContain('每日运营简报')
    expect(source).toContain("smart-education:ai-compose")
  })
})
