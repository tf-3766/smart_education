import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import LiquidGlass from '@/components/LiquidGlass.vue'
import { DEFAULT_GLASS_MATERIAL, useGlassMaterial } from '@/composables/useGlassMaterial'

describe('学习首页玻璃材质设置', () => {
  const glass = useGlassMaterial()

  beforeEach(() => {
    localStorage.clear()
    glass.reset()
  })

  afterEach(() => glass.reset())

  it('统一驱动折射、模糊、饱和度和两层透明度并持久化', async () => {
    Object.assign(glass.settings, {
      displacementScale: 36,
      blur: 28,
      saturation: 205,
      surfaceOpacity: 26,
      warpOpacity: 17,
    })
    await nextTick()

    const wrapper = mount(LiquidGlass)
    expect(wrapper.find('feDisplacementMap').attributes('scale')).toBe('36')
    expect(wrapper.attributes('style')).toContain('--liquid-blur: 28px')
    expect(wrapper.attributes('style')).toContain('--liquid-saturation: 205%')
    expect(glass.cssVariables.value['--liquid-surface']).toBe('rgba(255,255,255,0.26)')
    expect(glass.cssVariables.value['--liquid-warp-surface']).toBe('rgba(255,255,255,0.17)')
    expect(JSON.parse(localStorage.getItem('smart-education-glass-material') ?? '{}')).toMatchObject(glass.settings)
  })

  it('可以一键恢复统一默认材质', async () => {
    glass.settings.blur = 34
    glass.settings.surfaceOpacity = 32
    glass.reset()
    await nextTick()

    expect({ ...glass.settings }).toEqual(DEFAULT_GLASS_MATERIAL)
  })
})
