<script setup lang="ts">
import { ref } from 'vue'
import type { UserRole } from '../../types/ui'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

defineProps<{ role: UserRole }>()
const mobileMenuOpen = ref(false)
</script>

<template>
  <div class="app-shell">
    <AppHeader :role="role" @menu="mobileMenuOpen = true" />
    <AppSidebar :role="role" />
    <main class="app-main">
      <slot />
    </main>
    <el-drawer v-model="mobileMenuOpen" direction="ltr" size="280px" :with-header="false" class="mobile-nav-drawer">
      <div class="drawer-brand">智学课堂</div>
      <AppSidebar :role="role" @navigate="mobileMenuOpen = false" />
    </el-drawer>
  </div>
</template>
