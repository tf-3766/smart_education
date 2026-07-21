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
})
