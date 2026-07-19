import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useSessionStore } from '@/stores/session'

describe('session store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('starts unauthenticated and defaults to demo mode', () => {
    const session = useSessionStore()
    expect(session.authenticated).toBe(false)
    expect(session.isDemoMode).toBe(true)
  })

  it('logs in with a role and persists it to storage', () => {
    const session = useSessionStore()
    session.login('student')
    expect(session.authenticated).toBe(true)
    expect(session.currentRole).toBe('student')
    const stored = JSON.parse(sessionStorage.getItem('smart-education-session') ?? '{}')
    expect(stored).toEqual({ authenticated: true, role: 'student' })
  })

  it('switching role updates the resolved current user', () => {
    const session = useSessionStore()
    session.login('teacher')
    expect(session.currentUser.role).toBe('teacher')
    session.switchRole('admin')
    expect(session.currentUser.role).toBe('admin')
  })

  it('restores a persisted session on creation', () => {
    sessionStorage.setItem('smart-education-session', JSON.stringify({ authenticated: true, role: 'admin' }))
    const session = useSessionStore()
    expect(session.authenticated).toBe(true)
    expect(session.currentRole).toBe('admin')
  })
})
