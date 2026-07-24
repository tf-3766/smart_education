<template>
  <RouterView />
  <AppConfirmDialog />
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppConfirmDialog from '@/components/AppConfirmDialog.vue'
</script>
