import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppButton from '@/components/AppButton.vue'

describe('AppButton', () => {
  it('forwards loading to a busy, non-interactive button', () => {
    const wrapper = mount(AppButton, { props: { variant: 'primary', loading: true }, slots: { default: '保存' } })
    const button = wrapper.get('button')
    expect(button.attributes('aria-busy')).toBe('true')
    expect(button.classes().join(' ')).toContain('pointer-events-none')
    expect(button.attributes('disabled')).toBeDefined()
  })

  it('forwards disabled', () => {
    const wrapper = mount(AppButton, { props: { variant: 'primary', disabled: true }, slots: { default: '保存' } })
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it('is interactive by default', () => {
    const wrapper = mount(AppButton, { props: { variant: 'primary' }, slots: { default: '保存' } })
    const button = wrapper.get('button')
    expect(button.attributes('aria-busy')).toBeUndefined()
    expect(button.attributes('disabled')).toBeUndefined()
  })
})
