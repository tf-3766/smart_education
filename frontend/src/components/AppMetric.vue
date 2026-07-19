<template>
  <div class="kpi" :class="tone">
    <div>
      <span class="kpi-label">{{ label }}</span>
      <span class="kpi-value">{{ value }}</span>
      <span v-if="hint" class="kpi-foot">{{ hint }}</span>
    </div>
    <span v-if="icon" class="kpi-icon"><component :is="icon" :size="20" /></span>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    tone?: '' | 'warn' | 'danger' | 'success'
    icon?: Component
  }>(),
  { tone: '' },
)
</script>
