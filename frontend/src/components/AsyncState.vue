<template>
  <div v-if="loading" class="async-state" role="status">正在加载…</div>
  <div v-else-if="error" class="async-state async-state-error" role="alert">
    <strong>{{ error.message }}</strong>
    <small v-if="error.traceId">追踪编号：{{ error.traceId }}</small>
    <button v-if="retry" class="text-link" @click="retry">重新加载</button>
  </div>
  <div v-else-if="empty" class="async-state">{{ emptyText }}</div>
</template>

<script setup lang="ts">
import type { RuntimeError } from '@/services/runtime'

defineProps<{
  loading?: boolean
  error?: RuntimeError | null
  empty?: boolean
  emptyText?: string
  retry?: (() => void) | null
}>()
</script>
