<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">{{ accountSubtitle }}</p>
      </div>
      <div class="account-page-actions">
        <AppButton variant="secondary" @click="goHome">
          <span class="app-button-label"><LayoutDashboard :size="16" />返回工作台</span>
        </AppButton>
        <AppButton variant="secondary" @click="logout">
          <span class="app-button-label"><LogOut :size="16" />退出登录</span>
        </AppButton>
      </div>
    </div>

    <div v-if="message" class="toast">{{ message }}</div>

    <div class="grid cols-2 account-grid">
      <section class="panel">
        <h2 class="panel-title">个人信息</h2>
        <div class="account-profile-head">
          <span class="account-avatar" aria-hidden="true">
            <img v-if="avatarSrc" :src="avatarSrc" alt="头像" />
            <UserRound v-else :size="30" />
          </span>
          <div class="account-profile-meta">
            <h2>{{ displayName }}</h2>
            <p>{{ username }}</p>
            <div class="account-avatar-actions">
              <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp,image/gif" hidden @change="onPickAvatar" />
              <button type="button" class="text-link" :disabled="avatarBusy" @click="fileInput?.click()">{{ avatarBusy ? '上传中…' : '更换头像' }}</button>
              <button v-if="hasAvatar" type="button" class="text-link danger" :disabled="avatarBusy" @click="removeAvatar">移除头像</button>
            </div>
          </div>
        </div>
        <dl class="account-fields">
          <div v-for="item in profileRows" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value }}</dd>
          </div>
        </dl>
      </section>

      <section class="panel">
        <h2 class="panel-title">修改密码</h2>
        <form class="stack" @submit.prevent="submitPassword">
          <div>
            <label class="field-label" for="pw-current">当前密码</label>
            <input id="pw-current" v-model="pw.current" class="input" type="password" autocomplete="current-password" placeholder="请输入当前密码" />
          </div>
          <div>
            <label class="field-label" for="pw-new">新密码</label>
            <input id="pw-new" v-model="pw.next" class="input" type="password" autocomplete="new-password" placeholder="8-128 位，含字母和数字" />
          </div>
          <div>
            <label class="field-label" for="pw-confirm">确认新密码</label>
            <input id="pw-confirm" v-model="pw.confirm" class="input" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
          </div>
          <p v-if="pwError" class="form-error" role="alert">{{ pwError }}</p>
          <div class="form-actions">
            <AppButton variant="primary" type="submit" :loading="pwBusy" :disabled="!pwFilled">保存新密码</AppButton>
          </div>
          <p class="muted" style="font-size: 12.5px; margin: 0">修改成功后当前登录仍有效，下次登录请使用新密码。</p>
        </form>
      </section>
    </div>

    <section class="panel push-top">
      <div class="spread wrap">
        <h2 class="panel-title">权限信息</h2>
        <StatusBadge tone="blue" :label="permissionCountLabel" />
      </div>
      <div class="account-permissions">
        <span v-for="permission in permissionRows" :key="permission.code" class="tag blue">{{ permission.label }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutDashboard, LogOut, UserRound } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { authApi, filesApi } from '@/services/api'
import { permissionLabel, roleLabel } from '@/utils/permissionLabels'
import { demoAccounts, roleHome, roleLabels, useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()

const demoAccount = computed(() => demoAccounts.find((item) => item.role === session.currentRole))
const displayName = computed(() => session.backendUser?.displayName ?? session.currentUser.name)
const username = computed(() => session.backendUser?.username ?? demoAccount.value?.username ?? session.currentUser.id)
const accountSubtitle = computed(() => `${roleText.value} · ${session.isDemoMode ? '演示模式' : '已登录'}`)

// 角色：以后端下发的角色列表为准并转中文；无后端用户（演示快捷登录）时回退预览角色。
// 合并为单行展示，避免同义字段重复。
const roleText = computed(() => {
  const roles = session.backendUser?.roles ?? []
  return roles.length ? roles.map(roleLabel).join(' / ') : roleLabels[session.currentRole]
})
const profileRows = computed(() => [
  { label: '显示名称', value: displayName.value },
  { label: '用户名', value: username.value },
  { label: '用户 ID', value: session.backendUser?.userId ?? session.currentUser.id },
  { label: '角色', value: roleText.value },
])

const permissionRows = computed(() => {
  const permissions = session.backendUser?.permissions ?? []
  if (!permissions.length) return [{ code: '__none__', label: '未下发权限明细' }]
  return permissions.map((code) => ({ code, label: permissionLabel(code) }))
})
const permissionCountLabel = computed(() => {
  const count = (session.backendUser?.permissions ?? []).length
  return count ? `${count} 项` : '无明细'
})

const message = ref('')
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

// —— 头像 ——
const fileInput = ref<HTMLInputElement | null>(null)
const avatarBusy = ref(false)
const avatarSrc = ref('')
const hasAvatar = computed(() => Boolean(session.backendUser?.avatarFileId))

async function loadAvatar() {
  const fileId = session.backendUser?.avatarFileId
  releaseAvatar()
  if (!fileId) { avatarSrc.value = ''; return }
  try { avatarSrc.value = await filesApi.contentObjectUrl(fileId) } catch { avatarSrc.value = '' }
}
function releaseAvatar() {
  if (avatarSrc.value.startsWith('blob:')) URL.revokeObjectURL(avatarSrc.value)
}

async function onPickAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type)) { flash('头像仅支持 PNG / JPEG / WebP / GIF。'); return }
  if (file.size > 5 * 1024 * 1024) { flash('头像不能超过 5MB。'); return }
  avatarBusy.value = true
  try {
    const stored = await filesApi.upload(file, 'AVATAR')
    const user = await authApi.updateAvatar({ fileId: stored.fileId, version: session.backendUser?.version ?? 0 })
    session.applyCurrentUser(user)
    await loadAvatar()
    flash('头像已更新')
  } catch (error) {
    flash(error instanceof Error ? error.message : '头像更新失败，请重试。')
  } finally {
    avatarBusy.value = false
  }
}

async function removeAvatar() {
  avatarBusy.value = true
  try {
    const user = await authApi.updateAvatar({ fileId: null, version: session.backendUser?.version ?? 0 })
    session.applyCurrentUser(user)
    await loadAvatar()
    flash('头像已移除')
  } catch (error) {
    flash(error instanceof Error ? error.message : '操作失败，请重试。')
  } finally {
    avatarBusy.value = false
  }
}

// —— 修改密码 ——
const pw = reactive({ current: '', next: '', confirm: '' })
const pwBusy = ref(false)
const pwError = ref('')
const pwFilled = computed(() => Boolean(pw.current && pw.next && pw.confirm))

async function submitPassword() {
  pwError.value = ''
  if (pw.next !== pw.confirm) { pwError.value = '两次输入的新密码不一致。'; return }
  if (!(pw.next.length >= 8 && pw.next.length <= 128 && /[A-Za-z]/.test(pw.next) && /\d/.test(pw.next))) {
    pwError.value = '新密码需为 8-128 位并同时包含字母和数字。'; return
  }
  if (pw.next === pw.current) { pwError.value = '新密码不能与当前密码相同。'; return }
  pwBusy.value = true
  try {
    await authApi.changePassword({ currentPassword: pw.current, newPassword: pw.next })
    pw.current = ''; pw.next = ''; pw.confirm = ''
    flash('密码已修改')
  } catch (error) {
    pwError.value = error instanceof Error ? error.message : '密码修改失败，请重试。'
  } finally {
    pwBusy.value = false
  }
}

function goHome() {
  router.push(roleHome[session.currentRole])
}
async function logout() {
  await session.logoutRemote()
  await router.push('/login')
}

watch(() => session.backendUser?.avatarFileId, loadAvatar)
onMounted(loadAvatar)
onBeforeUnmount(releaseAvatar)
</script>
