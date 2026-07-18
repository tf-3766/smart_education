<template>
  <div class="app-shell">
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
        <span class="user-chip">
          <span class="user-avatar">{{ session.currentUser.name.slice(0, 1) }}</span>
        </span>
        <button class="text-link" @click="onLogout">切换账号</button>
      </div>
    </header>

    <div class="app-body">
      <aside id="workspaceSidebar" class="sidebar workspace-sidebar" :class="{ open: mobileSidebarOpen }" :aria-label="`${domainName}导航`">
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
      </aside>
      <button class="workspace-sidebar-backdrop" :class="{ open: mobileSidebarOpen }" :aria-label="`关闭${domainName}导航`" @click="closeMobileSidebar" />

      <main class="main-area">
        <section class="content">
          <RouterView />
        </section>
      </main>
    </div>
    <NotificationDrawer :open="notificationOpen" @close="notificationOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { Bell, Menu } from 'lucide-vue-next'
import NotificationDrawer from '@/components/NotificationDrawer.vue'
import { getRoleSidebar } from '@/layouts/roleSidebar'
import type { RoleSidebarItem } from '@/layouts/roleSidebar'
import { roleLabels, useSessionStore } from '@/stores/session'
import { useNotificationStore } from '@/stores/notifications'
import type { Role } from '@/types/domain'

const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const notifications = useNotificationStore()
const mobileSidebarOpen = ref(false)
const notificationOpen = ref(false)

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

onUnmounted(() => notifications.stopRealtime())

async function onLogout() {
  await session.logoutRemote()
  await router.push('/login')
}
</script>
