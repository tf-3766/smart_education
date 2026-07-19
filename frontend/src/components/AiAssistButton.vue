<template>
  <AppButton variant="secondary" :loading="loading" :disabled="disabled">
    <span class="row"><Sparkles :size="14" />{{ label }}</span>
  </AppButton>
</template>

<script setup lang="ts">
// 统一的 AI 触发按钮：三个角色的 AI 功能都用它，保证图标/样式/交互一致。
// @click、title 等经单根透传到底层原生 button；loading/disabled 为显式 props。
import { Sparkles } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
defineProps<{ label: string; loading?: boolean; disabled?: boolean }>()
</script>
