import { createApp, h, nextTick, ref } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import AppModal from '@/components/AppModal.vue'

describe('AppModal', () => {
  let host: HTMLDivElement | undefined

  afterEach(() => {
    host?.remove()
    document.body.innerHTML = ''
  })

  it('exposes dialog semantics, traps initial focus and closes on Escape', async () => {
    host = document.createElement('div')
    document.body.append(host)
    const trigger = document.createElement('button')
    document.body.append(trigger)
    trigger.focus()

    const open = ref(true)
    const closed = ref(false)
    const app = createApp({
      setup: () => () =>
        h(AppModal, { open: open.value, title: '新建分类', onClose: () => { closed.value = true } }, {
          default: () => h('input', { class: 'input' }),
        }),
    })
    app.mount(host)
    await nextTick()
    await nextTick()

    const panel = document.querySelector<HTMLElement>('.modal-panel')
    expect(panel?.getAttribute('role')).toBe('dialog')
    expect(panel?.getAttribute('aria-modal')).toBe('true')
    const labelledby = panel?.getAttribute('aria-labelledby')
    expect(labelledby).toBeTruthy()
    expect(document.getElementById(labelledby!)?.textContent).toBe('新建分类')

    // focus moved into the modal (first focusable element)
    expect(panel?.contains(document.activeElement)).toBe(true)
    expect(document.activeElement).toBe(document.querySelector('.modal-panel [aria-label="关闭"]'))

    panel?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    expect(closed.value).toBe(true)

    app.unmount()
  })

  it('restores focus to the opener when it closes', async () => {
    host = document.createElement('div')
    document.body.append(host)
    const trigger = document.createElement('button')
    trigger.id = 'opener'
    document.body.append(trigger)
    trigger.focus()

    const open = ref(true)
    const app = createApp({
      setup: () => () => h(AppModal, { open: open.value, title: '弹窗', onClose: () => undefined }),
    })
    app.mount(host)
    await nextTick()
    await nextTick()
    expect(document.activeElement).not.toBe(trigger)

    open.value = false
    await nextTick()
    expect(document.activeElement).toBe(trigger)

    app.unmount()
  })
})
