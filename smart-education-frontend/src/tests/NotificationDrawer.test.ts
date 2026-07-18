import { createPinia } from 'pinia'
import { createApp, h, nextTick, ref } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it } from 'vitest'
import NotificationDrawer from '@/components/NotificationDrawer.vue'
import { resetDemoData } from '@/services/api'

describe('NotificationDrawer', () => {
  let host: HTMLDivElement | undefined

  afterEach(() => {
    host?.remove()
    document.body.innerHTML = ''
  })

  it('renders the inbox, filters unread messages and emits close', async () => {
    localStorage.clear()
    resetDemoData()
    sessionStorage.setItem('smart-education-session', JSON.stringify({ authenticated: true, role: 'student' }))
    host = document.createElement('div')
    document.body.append(host)
    const closed = ref(false)
    const app = createApp({
      setup: () => () => h(NotificationDrawer, { open: true, onClose: () => { closed.value = true } }),
    })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/student/dashboard', component: { template: '<div />' } },
        { path: '/student/:pathMatch(.*)*', component: { template: '<div />' } },
      ],
    })
    await router.push('/student/dashboard')
    app.use(createPinia())
    app.use(router)
    app.mount(host)
    await nextTick()
    await new Promise((resolve) => setTimeout(resolve, 220))
    await nextTick()

    expect(document.body.textContent).toContain('站内信')
    expect(document.querySelectorAll('.inbox-message')).toHaveLength(6)

    const unreadButton = [...document.querySelectorAll<HTMLButtonElement>('button')].find((button) => button.textContent?.trim() === '未读')
    unreadButton?.click()
    await nextTick()
    expect(unreadButton?.getAttribute('aria-pressed')).toBe('true')

    const subscriptionsButton = [...document.querySelectorAll<HTMLButtonElement>('button')].find((button) => button.textContent?.trim() === '订阅管理')
    subscriptionsButton?.click()
    await nextTick()
    expect(document.querySelector('.inbox-subscriptions')).not.toBeNull()
    expect(document.querySelectorAll('.inbox-subscriptions input[type="checkbox"]')).toHaveLength(5)

    const moreButton = [...document.querySelectorAll<HTMLButtonElement>('button')].find((button) => button.textContent?.trim() === '查看更多')
    moreButton?.click()
    await nextTick()
    const allButton = [...document.querySelectorAll<HTMLButtonElement>('button')].find((button) => button.textContent?.trim() === '全部')
    expect(allButton?.getAttribute('aria-pressed')).toBe('true')
    expect(document.querySelector('.inbox-subscriptions')).toBeNull()

    const closeButton = document.querySelector<HTMLButtonElement>('[aria-label="关闭站内信"]')
    closeButton?.click()
    expect(closed.value).toBe(true)

    app.unmount()
  })

  it('moves focus into the drawer and closes on Escape', async () => {
    localStorage.clear()
    resetDemoData()
    sessionStorage.setItem('smart-education-session', JSON.stringify({ authenticated: true, role: 'student' }))
    host = document.createElement('div')
    document.body.append(host)
    const closed = ref(false)
    const app = createApp({
      setup: () => () => h(NotificationDrawer, { open: true, onClose: () => { closed.value = true } }),
    })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div />' } }] })
    await router.push('/')
    app.use(createPinia())
    app.use(router)
    app.mount(host)
    await nextTick()

    const drawer = document.querySelector<HTMLElement>('#notificationDrawer')
    expect(document.activeElement).toBe(drawer)

    drawer?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    expect(closed.value).toBe(true)

    app.unmount()
  })
})
