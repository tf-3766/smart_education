<template>
  <section class="empty-state" style="min-height: 60vh">
    <h1 class="page-title">页面不存在</h1>
    <p class="muted">请从侧边菜单选择一个可用功能，或返回登录页。</p>
    <RouterLink class="text-link" to="/login">返回登录 ›</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
