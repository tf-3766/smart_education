import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('student dashboard course layout', () => {
  it('keeps progress, status, and entry action in their intended card positions', () => {
    const page = readFileSync(resolve(process.cwd(), 'src/domains/student/StudentDashboardPage.vue'), 'utf8')
    const styles = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

    expect(page).toContain('class="notice continue-course"')
    expect(page).toContain('class="continue-course-status"')
    expect(page).toContain('class="continue-course-enter"')
    expect(styles).toContain('grid-template-columns: minmax(0, 1fr) auto auto')
    expect(styles).toContain('width: 190px')
    expect(styles).toContain('min-width: 88px !important')
  })
})
