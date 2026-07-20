// 3.1 认证接口
import { demoDelay } from '../runtime'
import { TOKEN_STORAGE_KEY, get, isRealMode, post, put } from './client'
import { badRequest, conflict, currentUser, db, nextId, nowIso, persist } from './demo/db'
import type { UserRow } from './demo/db'
import type { CurrentUserVO, LoginRequest, LoginVO, LogoutVO, RegisterRequest, RegistrationVO, UpdateAvatarRequest } from './types'

function toCurrentUserVO(user: UserRow): CurrentUserVO {
  return {
    userId: user.userId,
    username: user.username,
    displayName: user.displayName,
    avatarFileId: user.avatarFileId ?? null,
    avatarUrl: user.avatarFileId ? `/api/v1/files/${user.avatarFileId}/content` : null,
    activeRole: user.roles[0],
    roles: [...user.roles],
    permissions: [],
    version: user.version,
  }
}

function toLoginVO(user: UserRow): LoginVO {
  return {
    accessToken: `demo-token-${user.userId}-${Date.now().toString(36)}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
    expiresAt: new Date(Date.now() + 7200 * 1000).toISOString(),
    user: toCurrentUserVO(user),
    roles: [...user.roles],
    permissions: [],
  }
}

export const authApi = {
  async login(body: LoginRequest): Promise<LoginVO> {
    if (isRealMode()) {
      const vo = await post<LoginVO>('/api/v1/auth/login', body)
      sessionStorage.setItem(TOKEN_STORAGE_KEY, vo.accessToken)
      return vo
    }
    const username = body.username.trim().toLowerCase()
    const user = db.users.find((item) => item.username === username && item.password === body.password)
    if (!user) badRequest('用户名或密码不正确。')
    if (user.userStatus === 'PENDING') conflict('教师账号待审核，审核通过后才能登录。', 'TEACHER_REGISTRATION_NOT_PENDING')
    if (user.userStatus !== 'ENABLED') conflict('账号不可用，请联系管理员。', 'OPERATION_NOT_ALLOWED')
    db.session.userId = user.userId
    persist()
    const vo = toLoginVO(user)
    sessionStorage.setItem(TOKEN_STORAGE_KEY, vo.accessToken)
    return demoDelay(vo)
  },

  async register(body: RegisterRequest): Promise<RegistrationVO> {
    if (isRealMode()) return post<RegistrationVO>('/api/v1/auth/register', body)
    const username = body.username.trim().toLowerCase()
    if (!/^[A-Za-z0-9._-]{3,64}$/.test(username)) badRequest('用户名需为 3-64 位字母、数字或 ._- 组合。')
    if (!(body.password.length >= 8 && body.password.length <= 128 && /[A-Za-z]/.test(body.password) && /\d/.test(body.password))) {
      badRequest('密码需为 8-128 位并同时包含字母和数字。')
    }
    if (db.users.some((item) => item.username === username)) conflict('用户名已被占用，请更换。', 'USERNAME_ALREADY_EXISTS')
    const pending = body.role === 'TEACHER'
    const user: UserRow = {
      userId: nextId(),
      username,
      password: body.password,
      displayName: body.displayName,
      roles: [body.role],
      userStatus: pending ? 'PENDING' : 'ENABLED',
      superAdministrator: false,
      createdAt: nowIso(),
      version: 0,
    }
    db.users.push(user)
    persist()
    return demoDelay({
      userId: user.userId,
      username: user.username,
      displayName: user.displayName,
      role: body.role,
      userStatus: user.userStatus as 'ENABLED' | 'PENDING',
      approvalRequired: pending,
      login: pending ? null : toLoginVO(user),
    })
  },

  async me(): Promise<CurrentUserVO> {
    if (isRealMode()) return get<CurrentUserVO>('/api/v1/auth/me')
    const user = db.users.find((item) => item.userId === db.session.userId) ?? currentUser('STUDENT')
    return demoDelay(toCurrentUserVO(user))
  },

  async logout(): Promise<LogoutVO> {
    const finish = () => sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    if (isRealMode()) {
      const vo = await post<LogoutVO>('/api/v1/auth/logout')
      finish()
      return vo
    }
    db.session.userId = null
    persist()
    finish()
    return demoDelay({ mode: 'STATELESS', serverSideRevoked: false })
  },

  async updateAvatar(body: UpdateAvatarRequest): Promise<CurrentUserVO> {
    if (isRealMode()) return put<CurrentUserVO>('/api/v1/auth/me/avatar', body)
    const user = db.users.find((item) => item.userId === db.session.userId) ?? currentUser('STUDENT')
    if (user.version !== body.version) conflict('用户信息已更新，请刷新后重试。')
    user.avatarFileId = body.fileId == null ? null : String(body.fileId)
    user.version += 1
    persist()
    return demoDelay(toCurrentUserVO(user))
  },
}
