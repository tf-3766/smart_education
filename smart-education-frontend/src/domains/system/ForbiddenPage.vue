<template>
  <section class="empty-state" style="min-height: 60vh">
    <h1 class="page-title">无权访问</h1>
    <p class="muted">当前预览角色（{{ roleLabels[session.currentRole] }}）没有这个功能入口，请切换到有权限的角色。</p>
    <RouterLink class="text-link" :to="roleHome[session.currentRole]">返回工作台 ›</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { roleHome, roleLabels, useSessionStore } from '@/stores/session'
const session = useSessionStore()
</script>
