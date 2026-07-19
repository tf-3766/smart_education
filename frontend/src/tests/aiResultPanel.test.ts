import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AiResultPanel from '@/components/AiResultPanel.vue'
import type { AiResult } from '@/types/domain'

const base: AiResult = { id: 'qa-1', type: 'qa', title: '课程答疑回答', content: '答案内容', confirmed: false }

describe('AiResultPanel 置信度展示', () => {
  it('无置信度时不显示置信度徽章（不伪造）', () => {
    const wrapper = mount(AiResultPanel, { props: { result: base } })
    expect(wrapper.text()).not.toContain('置信度')
  })

  it('有置信度时按百分比展示', () => {
    const wrapper = mount(AiResultPanel, { props: { result: { ...base, confidence: 0.9 } } })
    expect(wrapper.text()).toContain('置信度 90%')
  })
})
