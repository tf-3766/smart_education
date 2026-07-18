import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// Every visible form control needs a programmatically associated label so that
// clicking the label focuses the control and screen readers announce it.
// The convention here: any `<label class="field-label ...">` labels exactly one
// control and MUST carry a `for=` attribute. Group headings use `<p class="field-label">`.
const formPages = [
  'src/domains/teacher/QuestionBankPage.vue',
  'src/domains/teacher/CourseContentPage.vue',
  'src/domains/teacher/GradingWorkspacePage.vue',
  'src/domains/teacher/CourseManagePage.vue',
  'src/domains/admin/ContentGovernancePage.vue',
  'src/domains/admin/CourseReviewPage.vue',
]

describe('form label association', () => {
  it.each(formPages)('every field-label carries a for attribute in %s', (file) => {
    const source = readFileSync(resolve(process.cwd(), file), 'utf8')
    const labels = source.match(/<label class="field-label[^"]*"[^>]*>/g) ?? []
    const dangling = labels.filter((tag) => !tag.includes('for='))
    expect(dangling, `dangling field-label(s) without for=: ${dangling.join(' | ')}`).toEqual([])
  })

  it('associates ids uniquely per page (no duplicate for targets)', () => {
    for (const file of formPages) {
      const source = readFileSync(resolve(process.cwd(), file), 'utf8')
      const fors = [...source.matchAll(/<label class="field-label[^"]*"[^>]*\bfor="([^"]+)"/g)].map((m) => m[1])
      expect(new Set(fors).size, `duplicate for targets in ${file}`).toBe(fors.length)
    }
  })
})
