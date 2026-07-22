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

    expect(source).toContain('aiApi.listActions(20)')
    expect(source).toContain('class="pending-strip"')
    expect(source).toContain('function showPendingActions()')
    expect(source).toContain('void loadPersistedActions()')
  })

  it('requires an explicit confirmation phrase for strong-confirm actions', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/components/GlobalAiAssistant.vue'), 'utf8')

    expect(source).toContain("action.confirmationPolicy === 'STRONG_CONFIRM'")
    expect(source).toContain('strongConfirmInputs[action.actionId]')
    expect(source).toContain("=== '确认执行'")
    expect(source).toContain('!canConfirmAction(action)')
  })
})
