import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('global AI assistant mascot', () => {
  it('uses the book glass icon and drag feedback', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain('<BookOpen :size="30"')
    expect(source).toContain('class="ai-mascot-sparkle"')
    expect(source).toContain('opacity: .88')
    expect(source).toContain('transform: scale(1.06)')
    expect(source).toContain('opacity: 1')
  })

  it('snaps a dragged collapsed mascot to the nearest horizontal edge', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain('function snapMascotToEdge()')
    expect(source).toContain("? edgeInset")
    expect(source).toContain('snapMascotToEdge()')
  })

  it('renders backend capabilities, tool history and structured action results', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain("event.type === 'capability'")
    expect(source).toContain("event.type === 'action'")
    expect(source).toContain('class="action-result"')
    expect(source).toContain('class="citation-list"')
    expect(source).toContain('aiApi.capabilities(courseId.value)')
    expect(source).toContain('watch([courseId, () => session.currentRole]')
  })

  it('resolves the real course id from a student lesson route before asking the global assistant', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain('studentLearningApi.lessonDetail(lessonId.value)')
    expect(source).toContain('resolvedCourseId.value = detail.courseId')
    expect(source).toContain('courseId: courseId.value')
  })

  it('makes the student boundary read-only and exposes cross-page teacher authoring', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')
    expect(source).toContain('学生助手仅提供授权问答，不会创建、修改或发布任何业务数据。')
    expect(source).toContain('根据我负责课程的资料和章节生成一套题，并创建 AI 草稿题库')
    expect(source).toContain("session.isSuperAdmin ? ['批量预审待审核教师")
  })

  it('renders formal action preview with confirm and cancel controls', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain('class="action-preview"')
    expect(source).toContain('@click="confirmAction(action)"')
    expect(source).toContain('@click="cancelAction(action)"')
    expect(source).toContain('aiApi.confirmAction(action, strongConfirmInputs.value[action.actionId])')
    expect(source).toContain('aiApi.cancelAction(action)')
    expect(source).toContain("action.status === 'WAITING_CONFIRMATION'")
  })

  it('restores persisted pending actions after a page reload', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain('aiApi.listActions(50)')
    expect(source).toContain('class="pending-strip"')
    expect(source).toContain('function showPendingActions()')
    expect(source).toContain('void loadPersistedActions()')
    expect(source).toContain('class="action-stages"')
    expect(source).toContain('@click="retryAction(action)"')
    expect(source).toContain('aiApi.retryAction(action)')
  })

  it('requires an explicit confirmation phrase for strong-confirm actions', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain("action.confirmationPolicy === 'STRONG_CONFIRM'")
    expect(source).toContain('strongConfirmInputs[action.actionId]')
    expect(source).toContain("=== '确认执行'")
    expect(source).toContain('!canConfirmAction(action)')
  })

  it('accepts workflow prompts dispatched by teacher and admin pages', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain("window.addEventListener('smart-education:ai-compose', composeFromPage)")
    expect(source).toContain("window.removeEventListener('smart-education:ai-compose', composeFromPage)")
  })
  it('shows the complete guarded automation lifecycle including compensation', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')
    expect(source).toContain("['生成计划', '风险判断', '预览变化', '用户确认', '执行', '验证结果', '失败补偿']")
  })

})
