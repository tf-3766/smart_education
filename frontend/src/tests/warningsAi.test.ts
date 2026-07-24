import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('teacher warning AI intervention flow', () => {
  it('offers explanation and intervention plan backed by distinct endpoints', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/teacher/WarningsPage.vue'), 'utf8')

    expect(source).toContain('生成风险解读')
    expect(source).toContain('生成干预计划')
    expect(source).toContain('aiApi.warningInterventionPlan')
    expect(source).toContain('AI 学习干预计划草稿')
    expect(source).toContain('regenerateWarningDraft')
    expect(source).toContain(':maxlength="4000"')
    expect(source).toContain('最多 4000 字')
  })
})
