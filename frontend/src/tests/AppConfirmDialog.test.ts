import { createApp, nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import AppConfirmDialog from '@/components/AppConfirmDialog.vue'
import { confirmDialog } from '@/services/confirmDialog'

describe('AppConfirmDialog', () => {
  let app: ReturnType<typeof createApp> | undefined

  afterEach(() => {
    app?.unmount()
    app = undefined
    document.body.innerHTML = ''
  })

  it('uses the shared liquid-glass modal and resolves after confirmation', async () => {
    const host = document.createElement('div')
    document.body.append(host)
    app = createApp(AppConfirmDialog)
    app.mount(host)

    const result = confirmDialog('交卷后不能修改答案。', {
      title: '提交试卷',
      confirmLabel: '确认交卷',
    })
    await nextTick()
    await nextTick()

    const panel = document.querySelector<HTMLElement>('.modal-panel')
    expect(panel?.textContent).toContain('提交试卷')
    expect(panel?.textContent).toContain('交卷后不能修改答案。')
    expect(panel?.querySelector('.app-btn-primary')?.textContent).toContain('确认交卷')

    panel?.querySelector<HTMLButtonElement>('.app-btn-primary')?.click()
    await expect(result).resolves.toBe(true)
    await nextTick()
    expect(document.querySelector('.modal-panel')).toBeNull()
  })
})
