import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('管理端 AI P1 治理预审', () => {
  it('教师注册页批量预审但保留逐项审批入口', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/admin/UserManagementPage.vue'), 'utf8')
    expect(source).toContain('aiApi.adminGovernanceDraft')
    expect(source).toContain('教师注册批量预审')
    expect(source).toContain('不会自动通过或驳回')
    expect(source).toContain('adminUsersApi.approveTeacher')
    expect(source).toContain('adminUsersApi.rejectTeacher')
    expect(source).toContain('item.targetVersion')
    expect(source).toContain('item.registeredAt')
  })

  it('课程审核页展示规则证据和准备度，不直接代替审批', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/admin/CourseReviewPage.vue'), 'utf8')
    expect(source).toContain('aiApi.adminGovernanceDraft')
    expect(source).toContain('课程合规检查')
    expect(source).toContain('readinessScore')
    expect(source).toContain('courseReviewsApi.approve')
    expect(source).toContain('courseReviewsApi.reject')
    expect(source).toContain('item.targetVersion')
    expect(source).toContain('item.categoryId')
    expect(source).toContain('item.enrollmentOpenAt')
    expect(source).toContain('item.summary')
  })
})
