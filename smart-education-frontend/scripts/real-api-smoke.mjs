const gateway = process.env.GATEWAY_URL || 'http://localhost:18080'

const accounts = [
  { role: 'STUDENT', username: 'student', password: '123456', list: '/api/v1/student/courses?page=1&size=5', forbidden: '/api/v1/admin/statistics' },
  { role: 'TEACHER', username: 'teacher', password: 't123456', list: '/api/v1/teacher/courses?page=1&size=5', forbidden: '/api/v1/admin/statistics' },
  { role: 'SUPER_ADMIN', username: 'admin', password: 'admin123', list: '/api/v1/admin/statistics', forbidden: '/api/v1/teacher/courses?page=1&size=5' },
]

async function json(path, init = {}) {
  const response = await fetch(`${gateway}${path}`, init)
  const payload = await response.json()
  return { response, payload }
}

for (const account of accounts) {
  const login = await json('/api/v1/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: account.username, password: account.password }),
  })
  if (login.payload.code !== 'SUCCESS') throw new Error(`${account.username} login failed: ${login.payload.message}`)
  const token = login.payload.data.accessToken
  const headers = { Authorization: `Bearer ${token}` }
  const me = await json('/api/v1/auth/me', { headers })
  if (me.payload.code !== 'SUCCESS' || me.payload.data.activeRole !== account.role) throw new Error(`${account.username} /auth/me mismatch`)
  const list = await json(account.list, { headers })
  if (list.payload.code !== 'SUCCESS') throw new Error(`${account.username} list failed: ${list.payload.message}`)
  const forbidden = await json(account.forbidden, { headers })
  if (forbidden.response.status !== 403 || forbidden.payload.code !== 'FORBIDDEN') throw new Error(`${account.username} authorization boundary failed`)
  console.log(`${account.role}: login, me, list and authorization boundary OK (${me.payload.data.displayName})`)
}
