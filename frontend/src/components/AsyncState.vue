<template>
  <!--
    读取中的页面不再插入顶部占位条：它会把已渲染的内容向下推，响应回来后又回弹。
    首次读取由页面自身的结构保持稳定，缓存命中时则直接显示已有的真实数据。
  -->
  <div v-if="error" class="async-state async-state-error" role="alert">
    <strong>{{ error.message }}</strong>
    <small v-if="error.traceId">追踪编号：{{ error.traceId }}</small>
    <button v-if="retry" class="text-link" @click="retry">重新加载</button>
  </div>
  <div v-else-if="!loading && empty" class="async-state">{{ emptyText }}</div>
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
