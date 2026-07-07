<script setup lang="ts">
import type { MetricItem } from '../../types/ui'

defineProps<{ item: MetricItem }>()
</script>

<template>
  <article class="metric-card" :class="`metric-card--${item.tone}`">
    <span class="metric-icon"><el-icon><component :is="item.icon" /></el-icon></span>
    <div class="metric-copy">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <small>{{ item.detail }}</small>
    </div>
  </article>
</template>
