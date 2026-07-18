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

    <div class="grid cols-2 account-grid">
      <section class="panel">
        <div class="account-profile-head">
          <span class="account-profile-icon" aria-hidden="true"><UserRound :size="26" /></span>
          <div>
            <h2>{{ displayName }}</h2>
            <p>{{ username }}</p>
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
        <div class="spread">
          <h2 class="panel-title">账号状态</h2>
          <StatusBadge :tone="session.isDemoMode ? 'amber' : 'green'" :label="session.isDemoMode ? '演示' : '已登录'" />
        </div>
        <div class="stack">
          <div class="notice account-capability">
            <div>
              <strong>头像资料</strong>
              <p>{{ avatarStatus }}</p>
            </div>
            <StatusBadge tone="amber" label="未开放" />
          </div>
          <div class="notice account-capability">
            <div>
              <strong>密码</strong>
              <p>当前前端没有账号密码自助变更入口</p>
            </div>
            <StatusBadge tone="amber" label="未开放" />
          </div>
        </div>
      </section>
    </div>

    <section class="panel push-top">
      <div class="spread wrap">
        <h2 class="panel-title">权限信息</h2>
        <StatusBadge tone="blue" :label="permissionCountLabel" />
      </div>
      <div class="account-permissions">
        <span v-for="permission in permissionRows" :key="permission" class="tag blue">{{ permission }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutDashboard, LogOut, UserRound } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { demoAccounts, roleHome, roleLabels, useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()

const backendRoleLabels: Record<string, string> = {
  STUDENT: '学生',
  TEACHER: '教师',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

const demoAccount = computed(() => demoAccounts.find((item) => item.role === session.currentRole))
const displayName = computed(() => session.backendUser?.displayName ?? session.currentUser.name)
const username = computed(() => session.backendUser?.username ?? demoAccount.value?.username ?? session.currentUser.id)
const roleText = computed(() => roleLabels[session.currentRole])
const accountSource = computed(() => (session.isDemoMode ? '演示账号' : '后端账号'))
const accountSubtitle = computed(() => `${roleText.value} · ${accountSource.value}`)
const avatarStatus = computed(() => (session.backendUser?.avatarUrl ? '已有关联头像文件，当前页面不提供上传入口' : '暂无头像资料'))
const backendRoles = computed(() => session.backendUser?.roles ?? [])
const roleListText = computed(() => {
  if (!backendRoles.value.length) return roleText.value
  return backendRoles.value.map((role) => backendRoleLabels[role] ?? role).join(' / ')
})
const profileRows = computed(() => [
  { label: '显示名称', value: displayName.value },
  { label: '用户名', value: username.value },
  { label: '当前身份', value: roleText.value },
  { label: '拥有角色', value: roleListText.value },
  { label: '用户 ID', value: session.currentUser.id },
  { label: '账号来源', value: accountSource.value },
  { label: '资料版本', value: String(session.backendUser?.version ?? 0) },
])
const permissionRows = computed(() => {
  const permissions = session.backendUser?.permissions ?? []
  if (!permissions.length) return ['未下发权限明细']
  return permissions.slice(0, 12)
})
const permissionCountLabel = computed(() => {
  const count = (session.backendUser?.permissions ?? []).length
  return count ? `${count} 项` : '无明细'
})

function goHome() {
  router.push(roleHome[session.currentRole])
}

async function logout() {
  await session.logoutRemote()
  await router.push('/login')
}
</script>
