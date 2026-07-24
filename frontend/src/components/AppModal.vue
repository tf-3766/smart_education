<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="$emit('close')">
      <section
        ref="panel"
        :class="['modal-panel', { 'modal-panel-wide': wide }]"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        tabindex="-1"
        @keydown.esc.prevent="$emit('close')"
        @keydown.tab="trapFocus"
      >
        <header class="modal-header">
          <div class="modal-heading">
            <h2 :id="titleId" class="modal-title">{{ title }}</h2>
            <p v-if="description" class="modal-description">{{ description }}</p>
          </div>
          <button class="modal-close" aria-label="关闭" @click="$emit('close')">×</button>
        </header>
        <slot />
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, useId, watch } from 'vue'

const props = defineProps<{ open: boolean; title: string; description?: string; wide?: boolean }>()
defineEmits<{ close: [] }>()

const titleId = useId()
const panel = ref<HTMLElement | null>(null)
let lastFocused: HTMLElement | null = null

const focusableSelector =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

function focusables(): HTMLElement[] {
  if (!panel.value) return []
  return Array.from(panel.value.querySelectorAll<HTMLElement>(focusableSelector))
}

function trapFocus(event: KeyboardEvent) {
  const items = focusables()
  if (items.length === 0) {
    event.preventDefault()
    panel.value?.focus()
    return
  }
  const first = items[0]
  const last = items[items.length - 1]
  const active = document.activeElement as HTMLElement | null
  if (event.shiftKey && (active === first || active === panel.value)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      lastFocused = document.activeElement as HTMLElement | null
      void nextTick(() => {
        const [firstFocusable] = focusables()
        ;(firstFocusable ?? panel.value)?.focus()
      })
    } else {
      lastFocused?.focus?.()
      lastFocused = null
    }
  },
  { immediate: true },
)
</script>
