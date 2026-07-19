<template>
  <span class="tag" :class="tone"><slot>{{ label }}</slot></span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{ label?: string; tone?: 'blue' | 'green' | 'amber' | 'red' | 'gray' }>(),
  { tone: 'gray' },
)
</script>
