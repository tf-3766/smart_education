<template>
  <span class="user-avatar" :style="avatarStyle" :title="name" aria-hidden="true">
    <img v-if="src" :src="src" alt="" />
    <span v-else class="user-avatar-fallback">{{ initial }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { filesApi } from '@/services/api'

const props = withDefaults(defineProps<{ fileId?: string | null; name?: string; size?: number }>(), {
  fileId: null,
  name: '用户',
  size: 32,
})

const src = ref('')
const initial = computed(() => (props.name?.trim()?.slice(0, 1) || '知').toUpperCase())
const avatarStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))

function release() {
  if (src.value.startsWith('blob:')) URL.revokeObjectURL(src.value)
  src.value = ''
}

async function load() {
  release()
  if (!props.fileId) return
  try { src.value = await filesApi.contentObjectUrl(props.fileId) } catch { src.value = '' }
}

watch(() => props.fileId, load, { immediate: true })
onBeforeUnmount(release)
</script>

<style scoped>
.user-avatar { display: inline-grid; flex: 0 0 auto; place-items: center; overflow: hidden; border: 1px solid #d7e1ef; border-radius: 50%; background: #eaf2ff; color: #155dc2; font-weight: 700; line-height: 1; }
.user-avatar img { width: 100%; height: 100%; object-fit: cover; }
.user-avatar-fallback { font-size: 0.8em; }
</style>