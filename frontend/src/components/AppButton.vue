<template>
  <Button
    :type="type"
    :variant="variant === 'primary' ? 'solid' : 'subtle'"
    theme="gray"
    :loading="loading"
    :disabled="disabled"
    :class="['app-button', variant === 'primary' ? 'app-btn-primary' : 'app-btn-secondary']"
  >
    <slot />
  </Button>
</template>

<script setup lang="ts">
import { Button } from 'frappe-ui'
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary'
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
  }>(),
  { type: 'button' },
)
</script>
