import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('教师正式业务课程范围', () => {
  it('作业批改、考试题库、学情预警和课程互动统一只加载正式课程', () => {
    for (const page of ['GradingWorkspacePage.vue', 'QuestionBankPage.vue', 'WarningsPage.vue', 'TeacherForumPage.vue']) {
      const source = readFileSync(resolve(process.cwd(), `src/domains/teacher/${page}`), 'utf8')
      expect(source, page).toContain('teacherCoursesApi.listFormal(')
    }
  })

  it('无可管理课程时明确显示空状态，而不是空白选择器', () => {
    for (const page of ['QuestionBankPage.vue', 'WarningsPage.vue', 'TeacherForumPage.vue']) {
      const source = readFileSync(resolve(process.cwd(), `src/domains/teacher/${page}`), 'utf8')
      expect(source, page).toContain('v-if="!courses.length"')
      expect(source, page).toContain('暂无可管理课程')
    }
  })
})
