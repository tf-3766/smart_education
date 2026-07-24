<template>
  <section v-if="active" class="ai-generation-progress" role="status" aria-live="polite">
    <span class="ai-generation-orbit" aria-hidden="true"><Sparkles :size="15" /></span>
    <div>
      <strong>{{ label }}</strong>
      <p>{{ stages[stageIndex] }}<span aria-hidden="true"> · </span>已等待 {{ elapsedSeconds }} 秒</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Sparkles } from 'lucide-vue-next'

const props = withDefaults(defineProps<{ active: boolean; label?: string }>(), { label: '正在生成 AI 草稿' })
const stages = ['已提交生成请求', '正在读取已授权的数据', '正在组织草稿内容', '正在校验结果与安全边界']
const elapsedSeconds = ref(0)
const stageIndex = ref(0)
let timer: number | undefined

function stopTimer() { if (timer) window.clearInterval(timer); timer = undefined }
function startTimer() {
  stopTimer(); elapsedSeconds.value = 0; stageIndex.value = 0
  timer = window.setInterval(() => {
    elapsedSeconds.value += 1
    stageIndex.value = elapsedSeconds.value >= 8 ? 3 : elapsedSeconds.value >= 4 ? 2 : elapsedSeconds.value >= 1 ? 1 : 0
  }, 1000)
}
watch(() => props.active, (active) => { if (active) startTimer(); else stopTimer() }, { immediate: true })
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.ai-generation-progress { display: flex; align-items: center; gap: 10px; padding: 11px 12px; border: 1px solid rgba(120, 183, 247, .52); border-radius: 14px; color: #23507f; background: linear-gradient(120deg, rgba(245, 252, 255, .68), rgba(218, 239, 255, .5)); box-shadow: inset 0 1px 0 rgba(255,255,255,.78), 0 6px 16px rgba(44, 113, 180, .08); backdrop-filter: blur(14px) saturate(130%); }
.ai-generation-orbit { display: inline-grid; width: 28px; height: 28px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #1269df; background: rgba(222, 241, 255, .76); animation: ai-generation-pulse 1.4s ease-in-out infinite; }
strong { display: block; color: var(--ink); font-size: 13px; }
p { margin: 2px 0 0; color: var(--muted); font-size: 12px; line-height: 1.45; }
@keyframes ai-generation-pulse { 50% { transform: scale(1.08); box-shadow: 0 0 0 5px rgba(52, 135, 239, .1); } }
@media (prefers-reduced-motion: reduce) { .ai-generation-orbit { animation: none; } }
</style>
