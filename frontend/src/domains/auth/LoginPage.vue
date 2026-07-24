<template>
  <main class="login-page">
    <header class="login-topbar">
      <span class="login-topbrand">
        <span class="login-topbrand-icon" aria-hidden="true"><BookOpenCheck :size="27" /></span>
        <span class="login-topbrand-name">知行教学云</span>
      </span>
    </header>

    <div class="login-shell">
      <section class="login-card" :class="{ 'login-card-register': mode === 'register' }">
        <template v-if="!session.isDemoMode">
          <div class="login-tabs" role="tablist">
            <button type="button" :class="['login-tab', { active: mode === 'login' }]" role="tab" :aria-selected="mode === 'login'" @click="switchMode('login')">登录</button>
            <button type="button" :class="['login-tab', { active: mode === 'register' }]" role="tab" :aria-selected="mode === 'register'" @click="switchMode('register')">注册</button>
          </div>

          <form v-if="mode === 'login'" class="login-form" @submit.prevent="submit">
            <label class="login-field">
              <span class="field-label">账号</span>
              <input v-model.trim="username" class="input" autocomplete="username" placeholder="请输入账号" required />
            </label>
            <label class="login-field">
              <span class="field-label">密码</span>
              <div class="login-input-wrap">
                <input v-model="password" class="input" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required />
                <button type="button" class="login-pwd-toggle" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                  <component :is="showPassword ? EyeOff : Eye" :size="18" />
                </button>
              </div>
            </label>
            <p v-if="error" class="login-error" role="alert">{{ error }}</p>
            <AppButton type="submit" variant="primary" class="login-submit" :disabled="submitting || !username || !password" @click.prevent="submit">
              {{ submitting ? '登录中…' : '登录 →' }}
            </AppButton>
          </form>

          <div v-else-if="registered" class="login-done">
            <p class="login-done-title">{{ registered }}</p>
            <AppButton variant="primary" class="login-submit" @click="backToLogin">去登录</AppButton>
          </div>

          <form v-else class="login-form" @submit.prevent="register">
            <div class="register-avatar-field">
              <div class="register-avatar-preview">
                <img v-if="avatarPreview" :src="avatarPreview" alt="注册头像预览" />
                <UserRound v-else :size="30" />
              </div>
              <div>
                <span class="field-label">头像（可选）</span>
                <p class="login-rule">支持 JPG、PNG、WebP 或 GIF，最大 5MB。</p>
                <div class="row wrap avatar-actions">
                  <label class="avatar-picker" for="register-avatar">选择头像</label>
                  <button v-if="avatarFile" type="button" class="text-link" @click="clearAvatar">移除</button>
                </div>
                <input id="register-avatar" class="sr-only" type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="onAvatarChange" />
              </div>
            </div>
            <label class="login-field">
              <span class="field-label">姓名</span>
              <input v-model.trim="reg.displayName" class="input" autocomplete="name" placeholder="请输入姓名" required />
            </label>
            <label class="login-field">
              <span class="field-label">账号</span>
              <input v-model.trim="reg.username" class="input" autocomplete="username" placeholder="3-64 位字母、数字或 ._-" required />
              <small class="login-rule">账号至少 3 位；密码至少 8 位且须同时包含字母和数字，两者是不同规则。</small>
            </label>
            <label class="login-field">
              <span class="field-label">密码</span>
              <div class="login-input-wrap">
                <input v-model="reg.password" class="input" :type="showRegPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="8-128 位，含字母和数字" required />
                <button type="button" class="login-pwd-toggle" :aria-label="showRegPassword ? '隐藏密码' : '显示密码'" @click="showRegPassword = !showRegPassword">
                  <component :is="showRegPassword ? EyeOff : Eye" :size="18" />
                </button>
              </div>
            </label>
            <label class="login-field">
              <span class="field-label">确认密码</span>
              <div class="login-input-wrap">
                <input v-model="reg.confirmPassword" class="input" :type="showRegConfirm ? 'text' : 'password'" autocomplete="new-password" placeholder="请再次输入密码" required />
                <button type="button" class="login-pwd-toggle" :aria-label="showRegConfirm ? '隐藏密码' : '显示密码'" @click="showRegConfirm = !showRegConfirm">
                  <component :is="showRegConfirm ? EyeOff : Eye" :size="18" />
                </button>
              </div>
            </label>
            <label class="login-field">
              <span class="field-label">身份</span>
              <select v-model="reg.role" class="select">
                <option value="STUDENT">学生</option>
                <option value="TEACHER">教师（需管理员审核）</option>
              </select>
            </label>
            <p v-if="regError" class="login-error" role="alert">{{ regError }}</p>
            <AppButton type="submit" variant="primary" class="login-submit" :disabled="regSubmitting" @click.prevent="register">
              {{ regSubmitting ? '提交中…' : '注册 →' }}
            </AppButton>
            <p class="register-login-link">已有账号？<button type="button" @click="backToLogin">去登录</button></p>
          </form>
        </template>

        <div v-else class="login-roles">
          <button v-for="account in demoAccounts" :key="account.role" type="button" class="login-role" @click="enter(account.role)">
            <span class="user-avatar">{{ account.name.slice(0, 1) }}</span>
            <span style="flex: 1">
              <b>{{ roleLabels[account.role] }}</b>
              <small>{{ account.name }} · {{ account.desc }}</small>
            </span>
            <ChevronRight :size="18" style="color: var(--muted-2)" />
          </button>
        </div>
      </section>

      <section class="login-intro" aria-label="平台介绍">
        <p class="login-kicker">Learning Console</p>
        <h2>让教学流程<br /><span>更清晰、更高效</span></h2>
        <ul>
          <li>集中管理课程、作业与考试</li>
          <li>快速检索教学资源与学情数据</li>
          <li>随时跟进学生进度与成绩反馈</li>
        </ul>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpenCheck, ChevronRight, Eye, EyeOff, UserRound } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import { authApi } from '@/services/api'
import { demoAccounts, roleHome, roleLabels, useSessionStore } from '@/stores/session'
import type { Role } from '@/types/domain'

const router = useRouter()
const session = useSessionStore()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')
const showPassword = ref(false)
const showRegPassword = ref(false)
const showRegConfirm = ref(false)

const reg = reactive({ displayName: '', username: '', password: '', confirmPassword: '', role: 'STUDENT' as 'STUDENT' | 'TEACHER' })
const regSubmitting = ref(false)
const regError = ref('')
const registered = ref('')
const avatarFile = ref<File | null>(null)
const avatarPreview = ref('')

function clearAvatar() {
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = ''
  avatarFile.value = null
}
function onAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
    regError.value = '头像仅支持 JPG、PNG、WebP 或 GIF。'
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    regError.value = '头像大小不能超过 5MB。'
    input.value = ''
    return
  }
  clearAvatar()
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
  regError.value = ''
}
function switchMode(next: 'login' | 'register') {
  mode.value = next
  error.value = ''
  regError.value = ''
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    await session.loginWithCredentials(username.value, password.value)
    await router.push(roleHome[session.currentRole])
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '登录失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

async function register() {
  regError.value = ''
  const account = reg.username.trim()
  if (!reg.displayName.trim()) { regError.value = '请输入姓名'; return }
  if (!/^[A-Za-z0-9._-]{3,64}$/.test(account)) { regError.value = '账号需为 3-64 位字母、数字或 ._- 组合。'; return }
  if (!(reg.password.length >= 8 && reg.password.length <= 128 && /[A-Za-z]/.test(reg.password) && /\d/.test(reg.password))) {
    regError.value = '密码需为 8-128 位并同时包含字母和数字。'
    return
  }
  if (reg.password !== reg.confirmPassword) { regError.value = '两次输入的密码不一致。'; return }

  regSubmitting.value = true
  try {
    const vo = await authApi.register({ username: account, password: reg.password, displayName: reg.displayName.trim(), role: reg.role }, avatarFile.value)
    username.value = vo.username
    registered.value = vo.approvalRequired
      ? '教师账号已提交，待管理员审核通过后即可登录。'
      : '注册成功，请使用账号登录。'
  } catch (caught) {
    regError.value = caught instanceof Error ? caught.message : '注册失败，请稍后重试。'
  } finally {
    regSubmitting.value = false
  }
}

function backToLogin() {
  registered.value = ''
  Object.assign(reg, { displayName: '', username: '', password: '', confirmPassword: '', role: 'STUDENT' })
  clearAvatar()
  switchMode('login')
}

onBeforeUnmount(clearAvatar)

function enter(role: Role) {
  session.login(role)
  router.push(roleHome[role])
}
</script>

<style scoped>
.login-rule { color: var(--muted); font-size: 12px; line-height: 1.5; }
.register-avatar-field { display: grid; grid-template-columns: 68px minmax(0, 1fr); gap: 14px; align-items: center; padding: 12px; border: 1px solid var(--line); background: #f8fafc; }
.register-avatar-preview { display: grid; place-items: center; width: 64px; height: 64px; overflow: hidden; border-radius: 50%; color: var(--primary); background: #e8f1ff; border: 1px solid #c8dcff; }
.register-avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.register-avatar-field p { margin: 3px 0 7px; }
.avatar-actions { gap: 12px; }
.avatar-picker { padding: 5px 10px; border: 1px solid #b8c6da; color: var(--primary); background: #fff; font-size: 12px; font-weight: 700; cursor: pointer; }</style>
