<template>
  <div
    class="app-shell"
    :class="{ 'liquid-glass-shell': liquidGlassThemeActive, 'student-dashboard-shell': studentDashboardActive }"
    :style="liquidGlassThemeActive ? glassCssVariables : undefined"
  >
    <header class="topbar">
      <button class="mobile-sidebar-toggle" :aria-label="`打开${domainName}导航`" aria-controls="workspaceSidebar" :aria-expanded="mobileSidebarOpen" @click="mobileSidebarOpen = true">
        <Menu :size="20" />
      </button>
      <div class="brand">
        <span class="brand-mark">知</span>
        <span>知行教学云</span>
      </div>
      <div class="topbar-actions">
        <span class="preview-switch"><span>{{ roleLabels[session.currentRole] }}</span></span>
        <button class="topbar-bell" aria-label="打开站内信" aria-controls="notificationDrawer" :aria-expanded="notificationOpen" @click="notificationOpen = true">
          <Bell :size="20" />
          <b v-if="notifications.unreadCount">{{ notifications.unreadCount }}</b>
        </button>
        <RouterLink class="account-center-link" to="/account/profile" aria-label="个人中心（含退出登录）" title="个人中心">
          <UserAvatar :file-id="session.backendUser?.avatarFileId" :name="session.currentUser.name" :size="28" />
          <span>个人中心</span>
        </RouterLink>
      </div>
    </header>

    <div class="app-body">
      <component
        :is="liquidGlassThemeActive ? LiquidGlass : 'aside'"
        id="workspaceSidebar"
        as="aside"
        class="sidebar workspace-sidebar"
        :class="{ open: mobileSidebarOpen }"
        :interactive="liquidGlassThemeActive"
        :aria-label="`${domainName}导航`"
      >
        <p class="nav-title">{{ domainName }}</p>
        <nav class="workspace-nav" :aria-label="`${domainName}导航`">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ 'router-link-active': isActive(item) }"
            @click="closeMobileSidebar"
          >
            <component :is="item.icon" :size="20" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
        <button
          v-if="liquidGlassThemeActive"
          ref="glassSettingsTrigger"
          class="glass-settings-trigger"
          type="button"
          aria-haspopup="dialog"
          :aria-expanded="glassSettingsOpen"
          aria-controls="glassMaterialPanel"
          @click="openGlassSettings"
        >
          <SlidersHorizontal :size="18" />
          <span>背景透明调整</span>
        </button>
      </component>
      <button class="workspace-sidebar-backdrop" :class="{ open: mobileSidebarOpen }" :aria-label="`关闭${domainName}导航`" @click="closeMobileSidebar" />

      <main class="main-area">
        <LiquidGlass v-if="routeGlassSurfaceActive" as="section" class="content route-glass-surface" interactive>
          <RouterView />
        </LiquidGlass>
        <section v-else class="content">
          <RouterView />
        </section>
      </main>
    </div>
    <Teleport to="body">
      <Transition name="glass-settings">
        <div v-if="glassSettingsOpen" class="glass-settings-layer">
          <button class="glass-settings-dismiss" type="button" aria-label="关闭玻璃材质设置" @click="closeGlassSettings" />
          <section id="glassMaterialPanel" ref="glassSettingsPanel" class="glass-settings-panel" role="dialog" aria-modal="false" aria-labelledby="glassSettingsTitle">
            <header>
              <div>
                <strong id="glassSettingsTitle">玻璃材质</strong>
                <span>所有板块即时同步</span>
              </div>
              <button type="button" aria-label="关闭玻璃材质设置" @click="closeGlassSettings"><X :size="18" /></button>
            </header>

            <div class="glass-settings-controls">
              <label v-for="control in glassControls" :key="control.key" class="glass-setting-row">
                <span><b>{{ control.label }}</b><output :for="`glass-${control.key}`">{{ glassSettings[control.key] }}{{ control.unit }}</output></span>
                <input
                  :id="`glass-${control.key}`"
                  type="range"
                  :min="control.min"
                  :max="control.max"
                  :step="control.step"
                  :value="glassSettings[control.key]"
                  @input="updateGlassSetting(control.key, $event)"
                />
              </label>
            </div>

            <footer>
              <button type="button" @click="resetGlassMaterial"><RotateCcw :size="15" /> 恢复默认</button>
              <span>自动保存在此浏览器</span>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
    <NotificationDrawer :open="notificationOpen" @close="notificationOpen = false" />
    <GlobalAiAssistant />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { Bell, Menu, RotateCcw, SlidersHorizontal, X } from 'lucide-vue-next'
import NotificationDrawer from '@/components/NotificationDrawer.vue'
import GlobalAiAssistant from '@/components/GlobalAiAssistant.vue'
import LiquidGlass from '@/components/LiquidGlass.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useGlassMaterial, type GlassMaterialSettings } from '@/composables/useGlassMaterial'
import { getRoleSidebar } from '@/layouts/roleSidebar'
import type { RoleSidebarItem } from '@/layouts/roleSidebar'
import { roleLabels, useSessionStore } from '@/stores/session'
import { useNotificationStore } from '@/stores/notifications'
import type { Role } from '@/types/domain'

const route = useRoute()
const session = useSessionStore()
const notifications = useNotificationStore()
const mobileSidebarOpen = ref(false)
const notificationOpen = ref(false)
const glassSettingsOpen = ref(false)
const glassSettingsTrigger = ref<HTMLButtonElement | null>(null)
const glassSettingsPanel = ref<HTMLElement | null>(null)
const { settings: glassSettings, cssVariables: glassCssVariables, reset: resetGlassMaterial } = useGlassMaterial()
const liquidGlassThemeActive = computed(() => /^\/(student|teacher|admin)(?:\/|$)/.test(route.path))
const studentDashboardActive = computed(() => session.currentRole === 'student' && route.path === '/student/dashboard')
const routeGlassSurfaceActive = computed(() => liquidGlassThemeActive.value && !studentDashboardActive.value)

const glassControls: Array<{
  key: keyof GlassMaterialSettings
  label: string
  min: number
  max: number
  step: number
  unit: string
}> = [
  { key: 'displacementScale', label: '折射强度', min: 0, max: 50, step: 1, unit: '' },
  { key: 'blur', label: '模糊', min: 0, max: 40, step: 1, unit: 'px' },
  { key: 'saturation', label: '饱和度', min: 100, max: 220, step: 5, unit: '%' },
  { key: 'surfaceOpacity', label: '白色表面透明度', min: 4, max: 40, step: 1, unit: '%' },
  { key: 'warpOpacity', label: '折射层透明度', min: 0, max: 24, step: 1, unit: '%' },
]

const domainNames: Record<Role, string> = {
  student: '学习中心',
  teacher: '教学工作台',
  admin: '管理控制台',
}
const domainName = computed(() => domainNames[session.currentRole])
const navItems = computed<RoleSidebarItem[]>(() => getRoleSidebar(session.currentRole, { superAdmin: session.isSuperAdmin }).secondary.all.items)

function closeMobileSidebar() {
  mobileSidebarOpen.value = false
}

async function openGlassSettings() {
  glassSettingsOpen.value = true
  closeMobileSidebar()
  await nextTick()
  glassSettingsPanel.value?.querySelector<HTMLInputElement>('input')?.focus()
}

async function closeGlassSettings() {
  glassSettingsOpen.value = false
  await nextTick()
  if (window.innerWidth > 820) glassSettingsTrigger.value?.focus()
}

function updateGlassSetting(key: keyof GlassMaterialSettings, event: Event) {
  glassSettings[key] = Number((event.target as HTMLInputElement).value)
}

function onWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && glassSettingsOpen.value) void closeGlassSettings()
}

function isActive(item: RoleSidebarItem) {
  if (route.path === item.to || route.path.startsWith(`${item.to}/`)) return true
  return (item.matchPrefixes ?? []).some((prefix) => route.path.startsWith(prefix))
}

watch(
  () => [session.currentRole, session.currentUser.id],
  () => {
    notifications.reset()
    void notifications.refreshUnreadCount()
    notifications.startRealtime()
  },
  { immediate: true },
)

watch(() => route.path, () => { glassSettingsOpen.value = false })

onMounted(() => window.addEventListener('keydown', onWindowKeydown))

onUnmounted(() => {
  notifications.stopRealtime()
  window.removeEventListener('keydown', onWindowKeydown)
})

</script>
