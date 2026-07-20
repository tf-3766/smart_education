import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { LoginVO } from '@/services/api'

const { loginMock, registerMock } = vi.hoisted(() => ({ loginMock: vi.fn(), registerMock: vi.fn() }))

vi.mock('@/services/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/api')>()
  return { ...original, authApi: { ...original.authApi, login: loginMock, register: registerMock } }
})

import LoginPage from '@/domains/auth/LoginPage.vue'

const loginResult: LoginVO = {
  accessToken: 'real-jwt', tokenType: 'Bearer', expiresIn: 7200, expiresAt: '2026-07-14T14:00:00Z',
  user: { userId: '1001', username: 'student', displayName: '测试学生', activeRole: 'STUDENT', roles: ['STUDENT'], permissions: ['student:access'], version: 0 },
  roles: ['STUDENT'], permissions: ['student:access'],
}

describe('真实账号登录表单', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'real')
    localStorage.clear()
    loginMock.mockReset().mockResolvedValue(loginResult)
  })

  it('点击登录按钮会提交表单并进入角色首页', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: LoginPage },
        { path: '/student/dashboard', component: { template: '<div>学生首页</div>' } },
      ],
    })
    await router.push('/login')
    await router.isReady()
    const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } })

    await wrapper.get('input[autocomplete="username"]').setValue('student')
    await wrapper.get('input[autocomplete="current-password"]').setValue('123456')
    const submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeUndefined()
    await submitButton.trigger('click')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith({ username: 'student', password: '123456' })
    expect(router.currentRoute.value.path).toBe('/student/dashboard')
  })

  it('密码框可切换显示/隐藏', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: LoginPage }] })
    const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } })

    const pwd = wrapper.get('input[autocomplete="current-password"]')
    expect(pwd.attributes('type')).toBe('password')
    await wrapper.get('.login-pwd-toggle').trigger('click')
    expect(wrapper.get('input[autocomplete="current-password"]').attributes('type')).toBe('text')
  })

  it('渲染两栏布局：顶栏品牌与右侧介绍面板', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: LoginPage }] })
    const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } })

    expect(wrapper.find('.login-topbar').text()).toContain('知行教学云')
    expect(wrapper.find('.login-intro').exists()).toBe(true)
  })

  it('同页切换到注册标签并提交注册表单', async () => {
    registerMock.mockReset().mockResolvedValue({
      userId: '2001', username: 'newstudent', displayName: '新同学', role: 'STUDENT',
      userStatus: 'ENABLED', approvalRequired: false, login: null,
    })
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: LoginPage }] })
    const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } })

    const registerTab = wrapper.findAll('.login-tab').find((tab) => tab.text() === '注册')!
    await registerTab.trigger('click')

    await wrapper.get('input[autocomplete="name"]').setValue('新同学')
    await wrapper.get('input[autocomplete="username"]').setValue('newstudent')
    await wrapper.get('input[autocomplete="new-password"]').setValue('abcd1234')
    await wrapper.findAll('input[type="password"]')[1].setValue('abcd1234')

    await wrapper.get('button[type="submit"]').trigger('click')
    await flushPromises()

    expect(registerMock).toHaveBeenCalledWith({ username: 'newstudent', password: 'abcd1234', displayName: '新同学', role: 'STUDENT' })
    expect(wrapper.find('.login-done').text()).toContain('注册成功')
  })
})

describe('演示模式登录', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'demo')
    localStorage.clear()
  })

  it('演示模式渲染角色快捷登录按钮', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: LoginPage }] })
    const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } })

    const roleButtons = wrapper.findAll('.login-role')
    expect(roleButtons.length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('学生')
    expect(wrapper.find('input[autocomplete="username"]').exists()).toBe(false)
  })
})
