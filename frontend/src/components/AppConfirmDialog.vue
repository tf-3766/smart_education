<template>
  <AppModal
    :open="state.open"
    :title="state.title"
    :description="state.message"
    @close="settleConfirmDialog(false)"
  >
    <div class="form-actions modal-confirm-actions">
      <AppButton variant="secondary" @click="settleConfirmDialog(false)">{{ state.cancelLabel }}</AppButton>
      <AppButton variant="primary" @click="settleConfirmDialog(true)">{{ state.confirmLabel }}</AppButton>
    </div>
  </AppModal>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AppModal from '@/components/AppModal.vue'
import { confirmDialogState as state, registerConfirmDialogHost, settleConfirmDialog } from '@/services/confirmDialog'

let unregisterHost: (() => void) | null = null
onMounted(() => { unregisterHost = registerConfirmDialogHost() })
onUnmounted(() => { unregisterHost?.() })
</script>
