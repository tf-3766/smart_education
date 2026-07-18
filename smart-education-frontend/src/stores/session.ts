import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi, TOKEN_STORAGE_KEY } from '@/services/api'
import type { CurrentUserVO } from '@/services/api'
import { getApiMode } from '@/services/runtime'
import type { Role, User } from '@/types/domain'

const STORAGE_KEY = 'smart-education-session'

export const roleLabels: Record<Role, string> = {
  admin: '管理员',
  teacher: '教师',
  student: '学生',
}

export const roleHome: Record<Role, string> = {
  admin: '/admin/dashboard',
  teacher: '/teacher/dashboard',
  student: '/student/dashboard',
}

/** 演示账号：仅用于本地演示登录，真实鉴权仍以后端为准。 */
export const demoAccounts: { role: Role; username: string; name: string; desc: string }[] = [
  { role: 'student', username: 'student', name: '王一诺', desc: '软件 2301 · 学习端' },
  { role: 'teacher', username: 'teacher', name: '李明', desc: '计算机学院 · 教学端' },
  { role: 'admin', username: 'admin', name: '周敏', desc: '教务处 · 管理端' },
]

interface PersistedSession {
  authenticated: boolean
  role: Role
  user?: CurrentUserVO
}

export function mapBackendRole(role: string): Role {
  if (role === 'STUDENT') return 'student'
  if (role === 'TEACHER') return 'teacher'
  if (role === 'ADMIN' || role === 'SUPER_ADMIN') return 'admin'
  throw new Error(`Unsupported backend role: ${role}`)
}

function toUser(user: CurrentUserVO, role: Role): User {
  return {
    id: user.userId,
    name: user.displayName,
    role,
    department: '',
  }
}

function loadPersisted(): PersistedSession {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as PersistedSession
  } catch {
    /* ignore malformed storage */
  }
  return { authenticated: false, role: 'teacher' }
}

export const useSessionStore = defineStore('session', () => {
  const persisted = loadPersisted()
  const authenticated = ref(persisted.authenticated)
  const currentRole = ref<Role>(persisted.role)
  const backendUser = ref<CurrentUserVO | null>(persisted.user ?? null)
  const initialized = ref(false)

  const apiMode = computed(() => getApiMode())
  const isDemoMode = computed(() => apiMode.value === 'demo')

  // 是否为超级管理员：仅超级管理员拥有「用户管理」等 admin:manage 权限；
  // 普通被授权的管理员（ADMIN）只有 admin:access，不应看到/进入用户管理。
  const isSuperAdmin = computed(() => {
    if (currentRole.value !== 'admin') return false
    // 真实模式以后端下发角色为准；演示模式快捷登录（无 backendUser）的管理员视为超级管理员。
    if (backendUser.value) return backendUser.value.roles?.includes('SUPER_ADMIN') ?? false
    return true
  })

  const currentUser = computed<User>(() => {
    if (backendUser.value) return toUser(backendUser.value, currentRole.value)
    // 演示模式快捷登录未经 authApi，占位信息取自演示账号表。
    const account = demoAccounts.find((item) => item.role === currentRole.value) ?? demoAccounts[0]
    return { id: `demo-${account.role}`, name: account.name, role: account.role, department: account.desc.split(' · ')[0] ?? '' }
  })

  function persist(includeUser = false) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      authenticated: authenticated.value,
      role: currentRole.value,
      ...(includeUser && backendUser.value ? { user: backendUser.value } : {}),
    }))
  }

  function login(role: Role) {
    backendUser.value = null
    currentRole.value = role
    authenticated.value = true
    persist()
  }

  function switchRole(role: Role) {
    backendUser.value = null
    currentRole.value = role
    persist()
  }

  function logout() {
    authenticated.value = false
    backendUser.value = null
    persist()
  }

  async function loginWithCredentials(username: string, password: string) {
    const result = await authApi.login({ username, password })
    const role = mapBackendRole(result.user.activeRole)
    sessionStorage.setItem(TOKEN_STORAGE_KEY, result.accessToken)
    backendUser.value = result.user
    currentRole.value = role
    authenticated.value = true
    initialized.value = true
    persist(true)
    return result
  }

  async function restore() {
    if (initialized.value) return
    if (getApiMode() === 'demo') {
      initialized.value = true
      return
    }
    if (!sessionStorage.getItem(TOKEN_STORAGE_KEY)) {
      authenticated.value = false
      backendUser.value = null
      sessionStorage.removeItem(STORAGE_KEY)
      initialized.value = true
      return
    }
    try {
      const user = await authApi.me()
      const role = mapBackendRole(user.activeRole)
      backendUser.value = user
      currentRole.value = role
      authenticated.value = true
      persist(true)
    } catch (error) {
      authenticated.value = false
      backendUser.value = null
      sessionStorage.removeItem(TOKEN_STORAGE_KEY)
      sessionStorage.removeItem(STORAGE_KEY)
      throw error
    } finally {
      initialized.value = true
    }
  }

  async function logoutRemote() {
    try {
      if (sessionStorage.getItem(TOKEN_STORAGE_KEY)) await authApi.logout()
    } finally {
      authenticated.value = false
      backendUser.value = null
      sessionStorage.removeItem(TOKEN_STORAGE_KEY)
      sessionStorage.removeItem(STORAGE_KEY)
      initialized.value = true
    }
  }

  return {
    authenticated,
    currentRole,
    currentUser,
    isSuperAdmin,
    backendUser,
    initialized,
    apiMode,
    isDemoMode,
    roleLabels,
    login,
    switchRole,
    logout,
    loginWithCredentials,
    restore,
    logoutRemote,
  }
})
