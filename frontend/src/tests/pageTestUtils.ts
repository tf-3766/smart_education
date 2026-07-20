// 页面挂载测试助手：内存路由 + demo 数据复位 + demoDelay 等待。
import { flushPromises, mount } from '@vue/test-utils'
import type { Component } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { resetDemoData } from '@/services/api'

/** 等待 demoDelay（默认 90ms，写后重载可串联 3-4 次）与后续渲染都完成。 */
export async function settle(ms = 500) {
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, ms))
  await flushPromises()
}

/** 复位演示数据与本地存储，保证用例互不污染。 */
export function freshDemo() {
  localStorage.clear()
  sessionStorage.clear()
  resetDemoData()
}

/**
 * 用内存路由挂载页面。routePath 为路由模式（含参数占位），path 为实际访问地址。
 * Teleport 以 stub 方式渲染在原地，便于直接在 wrapper 上查询弹窗内容。
 */
export async function mountPage(component: Component, options: { path?: string; routePath?: string } = {}) {
  const routePath = options.routePath ?? options.path ?? '/'
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: routePath, component },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
    ],
  })
  await router.push(options.path ?? routePath)
  await router.isReady()
  const wrapper = mount(component, { global: { plugins: [router], stubs: { teleport: true } } })
  await settle()
  return { wrapper, router }
}

/** 在弹窗/面板中按可见文本找按钮并点击。 */
export async function clickByText(wrapper: ReturnType<typeof mount>, selector: string, text: string) {
  const target = wrapper.findAll(selector).find((node) => node.text().includes(text))
  if (!target) throw new Error(`找不到含「${text}」的 ${selector}`)
  await target.trigger('click')
  await settle()
}
