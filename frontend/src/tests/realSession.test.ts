import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUserVO, LoginVO } from '@/services/api'

const studentUser: CurrentUserVO = {
  userId: '1001',
  username: 'student',
  displayName: '测试学生',
  activeRole: 'STUDENT',
  roles: ['STUDENT'],
  permissions: ['student:access'],
  version: 0,
}

const loginVO: LoginVO = {
  accessToken: 'real-jwt',
  tokenType: 'Bearer',
  expiresIn: 7200,
  expiresAt: '2026-07-13T12:00:00Z',
  user: studentUser,
  roles: ['STUDENT'],
  permissions: ['student:access'],
}

const { loginMock, meMock, logoutMock } = vi.hoisted(() => ({
  loginMock: vi.fn(),
  meMock: vi.fn(),
  logoutMock: vi.fn(),
}))

vi.mock('@/services/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/api')>()
  return {
    ...original,
    authApi: {
      login: loginMock,
      me: meMock,
      logout: logoutMock,
    },
  }
})

import { TOKEN_STORAGE_KEY } from '@/services/api'
import { mapBackendRole, useSessionStore } from '@/stores/session'

describe('real backend session', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'real')
    setActivePinia(createPinia())
    localStorage.clear()
    loginMock.mockClear()
    meMock.mockClear()
    logoutMock.mockClear()
    loginMock.mockResolvedValue(loginVO)
    meMock.mockResolvedValue(studentUser)
    logoutMock.mockResolvedValue({ mode: 'STATELESS', serverSideRevoked: false })
  })

  it('maps backend roles onto frontend workspaces', () => {
    expect(mapBackendRole('STUDENT')).toBe('student')
    expect(mapBackendRole('TEACHER')).toBe('teacher')
    expect(mapBackendRole('ADMIN')).toBe('admin')
    expect(mapBackendRole('SUPER_ADMIN')).toBe('admin')
  })

  it('logs in with credentials and persists the backend user', async () => {
    const session = useSessionStore()
    await session.loginWithCredentials('student', '123456')

    expect(loginMock).toHaveBeenCalledWith({ username: 'student', password: '123456' })
    expect(session.authenticated).toBe(true)
    expect(session.currentRole).toBe('student')
    expect(session.currentUser).toMatchObject({ id: '1001', name: '测试学生', role: 'student' })
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('real-jwt')
    expect(JSON.parse(sessionStorage.getItem('smart-education-session') ?? '{}')).toMatchObject({
      authenticated: true,
      role: 'student',
      user: studentUser,
    })
    // 认证态只落 sessionStorage（按标签页隔离），绝不写 localStorage，
    // 否则同一浏览器不同标签页登录不同用户会互相覆盖。
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(localStorage.getItem('smart-education-session')).toBeNull()
  })

  it('restores the current user from the backend when a token exists', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'existing-jwt')
    const session = useSessionStore()
    await session.restore()

    expect(meMock).toHaveBeenCalledOnce()
    expect(session.authenticated).toBe(true)
    expect(session.currentUser.name).toBe('测试学生')
    expect(session.initialized).toBe(true)
  })

  it('clears token and session after remote logout', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'existing-jwt')
    const session = useSessionStore()
    await session.restore()
    await session.logoutRemote()

    expect(logoutMock).toHaveBeenCalledOnce()
    expect(session.authenticated).toBe(false)
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(sessionStorage.getItem('smart-education-session')).toBeNull()
  })
})
