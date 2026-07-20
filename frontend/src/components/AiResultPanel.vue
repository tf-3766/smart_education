<template>
  <div class="ai-card">
    <div class="spread wrap">
      <div class="row">
        <span class="ai-chip">AI</span>
        <strong style="color: var(--ink)">{{ result.title }}</strong>
      </div>
      <StatusBadge v-if="result.confidence != null" tone="green" :label="`置信度 ${Math.round(result.confidence * 100)}%`" />
    </div>

    <p class="pre-line push-top">{{ result.content }}</p>

    <div v-if="result.citations?.length" class="push-top">
      <span class="muted" style="font-size: 12.5px">引用来源</span>
      <div>
        <span v-for="c in result.citations" :key="c.title" class="ai-cite">{{ c.source }} / {{ c.title }}</span>
      </div>
    </div>
    <p v-else class="muted push-top" style="font-size: 12.5px">未找到可引用资料，请结合课程内容人工确认。</p>

    <div class="row push-top">
      <AppButton v-if="adoptLabel" variant="primary" @click="onAdopt">{{ confirmed ? '已采用' : adoptLabel }}</AppButton>
      <AppButton v-else variant="primary" @click="confirmed = true">{{ confirmed ? '已确认采用' : '确认采用' }}</AppButton>
      <AppButton variant="secondary" @click="$emit('regenerate')">重新生成</AppButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AppButton from '@/components/AppButton.vue'
import type { AiResult } from '@/types/domain'

const props = defineProps<{ result: AiResult; adoptLabel?: string }>()
const emit = defineEmits<{ regenerate: []; adopt: [content: string] }>()
const confirmed = ref(false)
function onAdopt() { confirmed.value = true; emit('adopt', props.result.content) }
</script>
