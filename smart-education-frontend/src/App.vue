<template>
  <FrappeUIProvider>
    <RouterView />
  </FrappeUIProvider>
</template>

<script setup lang="ts">
import { FrappeUIProvider } from 'frappe-ui'
import { RouterView } from 'vue-router'
</script>
