<script setup lang="ts">
defineProps<{
  label: string
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'ai'
}>()
</script>

<template>
  <span class="status-tag" :class="`status-tag--${tone ?? 'neutral'}`">{{ label }}</span>
</template>
