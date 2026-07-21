<template>
  <div class="ai-float" :class="{ open, dragging: isDragging }" :style="floatStyle">
    <button v-if="!open" class="ai-mascot" aria-label="打开知行 AI 助手" title="知行 AI 助手（可拖动）" @pointerdown="startDrag" @click="openAssistant">
      <BookOpen :size="30" stroke-width="2.15" /><Sparkles class="ai-mascot-sparkle" :size="14" stroke-width="2.4" />
    </button>
    <section v-else class="ai-window" aria-label="知行 AI 助手">
      <header class="ai-window-head" @pointerdown="startDrag">
        <div class="ai-head-avatar"><Bot :size="20" /></div>
        <div><strong>知行 AI 助手</strong><small>{{ roleName }} · 当前页面上下文</small></div>
        <button class="head-button" aria-label="最小化 AI 助手" @pointerdown.stop @click="closeAssistant"><Minus :size="18" /></button>
      </header>
      <div class="capability-row">
        <span :class="{ active: courseId }">RAG{{ courseId ? ' 已定位课程' : ' 进入课程后启用' }}</span>
        <span class="active">Tool Calling</span>
        <span class="active">日期与天气</span>
        <span title="尚未连接外部 MCP Server/Client">MCP 待接入</span>
      </div>
      <div ref="messageList" class="ai-messages">
        <article v-for="item in messages" :key="item.id" :class="['ai-message', item.role]">
          <span v-if="item.role === 'assistant'" class="mini-avatar">知</span>
          <div><p>{{ cleanAiText(item.content) }}</p><small v-if="item.activity">{{ item.activity }}</small></div>
        </article>
        <div v-if="asking" class="thinking"><span /><span /><span /></div>
      </div>
      <div v-if="messages.length === 1" class="suggestion-row">
        <button v-for="suggestion in suggestions" :key="suggestion" @click="question = suggestion">{{ suggestion }}</button>
      </div>
      <form class="ai-composer" @submit.prevent="ask">
        <textarea v-model="question" rows="2" :placeholder="placeholder" @keydown.enter.exact.prevent="ask" />
        <button type="submit" :disabled="asking || !question.trim()" aria-label="发送问题"><Send :size="18" /></button>
      </form>
      <p class="ai-disclaimer">回答会结合角色、当前页面与已授权课程；正式发布和评分仍需人工确认。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { BookOpen, Bot, Minus, Send, Sparkles } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { aiApi } from '@/services/api'
import type { AiToolEvent } from '@/services/api/types'
import { useSessionStore } from '@/stores/session'
import { cleanAiText } from '@/utils/aiText'

interface Message { id: string; role: 'assistant' | 'user'; content: string; activity?: string }
const route = useRoute()
const session = useSessionStore()
const open = ref(false)
const question = ref('')
const asking = ref(false)
const messageList = ref<HTMLElement | null>(null)
const conversationId = `global-${Date.now().toString(36)}`
const messages = ref<Message[]>([{ id: 'welcome', role: 'assistant', content: '你好，我是知行 AI 助手。你可以问我当前页面怎么操作，也可以进入具体课程后询问课程资料、作业与学习安排。' }])
const defaultPosition = () => ({ x: Math.max(16, window.innerWidth - 92), y: Math.max(90, window.innerHeight - 104) })
const position = ref(defaultPosition())
const collapsedPosition = ref(defaultPosition())
const positionStorageKey = 'global-ai-position-v2'
let moved = false
let dragOffset = { x: 0, y: 0 }
const isDragging = ref(false)

const roleName = computed(() => session.currentRole === 'teacher' ? '教师助手' : session.currentRole === 'admin' ? '管理员助手' : '学生助手')
const courseId = computed(() => String(route.params.courseId || route.query.courseId || '') || null)
const lessonId = computed(() => String(route.params.lessonId || route.query.lessonId || '') || null)
const placeholder = computed(() => courseId.value ? '询问当前课程、资料或操作…' : '询问平台功能或当前页面操作…')
const suggestions = computed(() => session.currentRole === 'teacher'
  ? ['如何创建一套完整作业？', '怎样同步课程知识库？', '如何用 AI 辅助组卷？']
  : session.currentRole === 'admin'
    ? ['教师审核流程是什么？', '怎样检查 AI 服务状态？', '平台统计包含哪些指标？']
    : ['我接下来应该学什么？', '今天日期和天气如何？', '如何查看考试和成绩？'])
const floatStyle = computed(() => ({ left: `${position.value.x}px`, top: `${position.value.y}px` }))

function clampPosition(x: number, y: number) {
  const width = open.value ? Math.min(390, window.innerWidth - 24) : 64
  const height = open.value ? Math.min(590, window.innerHeight - 24) : 64
  position.value = { x: Math.max(12, Math.min(window.innerWidth - width - 12, x)), y: Math.max(76, Math.min(window.innerHeight - height - 12, y)) }
}
function startDrag(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  moved = false
  isDragging.value = false
  const dragStart = { x: event.clientX, y: event.clientY }
  dragOffset = { x: event.clientX - position.value.x, y: event.clientY - position.value.y }
  target.setPointerCapture(event.pointerId)
  const move = (next: PointerEvent) => {
    if (!moved && Math.hypot(next.clientX - dragStart.x, next.clientY - dragStart.y) < 4) return
    moved = true
    isDragging.value = true
    clampPosition(next.clientX - dragOffset.x, next.clientY - dragOffset.y)
  }
  const end = () => {
    target.removeEventListener('pointermove', move)
    target.removeEventListener('pointerup', end)
    target.removeEventListener('pointercancel', end)
    isDragging.value = false
    if (!open.value) {
      if (moved) snapMascotToEdge()
      collapsedPosition.value = { ...position.value }
      localStorage.setItem(positionStorageKey, JSON.stringify(collapsedPosition.value))
    }
  }
  target.addEventListener('pointermove', move)
  target.addEventListener('pointerup', end)
  target.addEventListener('pointercancel', end)
}
function snapMascotToEdge() {
  const mascotWidth = 66
  const edgeInset = 12
  const x = position.value.x + mascotWidth / 2 < window.innerWidth / 2
    ? edgeInset
    : window.innerWidth - mascotWidth - edgeInset
  clampPosition(x, position.value.y)
}
function openAssistant() {
  if (moved) return
  collapsedPosition.value = { ...position.value }
  open.value = true
  clampPosition(collapsedPosition.value.x - 318, collapsedPosition.value.y - 490)
}
async function closeAssistant() {
  open.value = false
  await nextTick()
  position.value = { ...collapsedPosition.value }
  clampPosition(position.value.x, position.value.y)
}
async function scrollToBottom() { await nextTick(); messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' }) }
async function ask() {
  const prompt = question.value.trim()
  if (!prompt || asking.value) return
  messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: prompt })
  question.value = ''
  asking.value = true
  const reply: Message = { id: `a-${Date.now()}`, role: 'assistant', content: '' }
  messages.value.push(reply)
  await scrollToBottom()
  try {
    await aiApi.assistantStream({ question: prompt, courseId: courseId.value, lessonId: lessonId.value, pagePath: route.fullPath, pageTitle: document.title, conversationId }, (event) => {
      if (event.type === 'delta') reply.content += String(event.data ?? '')
      if (event.type === 'tool') reply.activity = (event.data as AiToolEvent).summary || '已完成上下文工具调用'
      if (event.type === 'error') reply.content = String((event.data as { message?: string })?.message || 'AI 暂时无法回答，请稍后重试。')
      messages.value = [...messages.value]
      void scrollToBottom()
    })
  } catch (error) { reply.content = error instanceof Error ? error.message : 'AI 暂时无法回答，请稍后重试。' }
  finally { asking.value = false; if (!reply.content) reply.content = '暂未生成回答，请稍后再试。'; messages.value = [...messages.value]; await scrollToBottom() }
}
function restorePosition() {
  try {
    const saved = JSON.parse(localStorage.getItem(positionStorageKey) || '') as { x: number; y: number }
    if (Number.isFinite(saved.x) && Number.isFinite(saved.y) && saved.x >= window.innerWidth / 2) {
      collapsedPosition.value = saved
      position.value = { ...saved }
    } else {
      collapsedPosition.value = defaultPosition()
      position.value = { ...collapsedPosition.value }
    }
  } catch { collapsedPosition.value = defaultPosition(); position.value = { ...collapsedPosition.value } }
  clampPosition(position.value.x, position.value.y)
}
function onResize() { clampPosition(position.value.x, position.value.y) }
onMounted(() => { restorePosition(); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<style scoped>
.ai-float { position: fixed; z-index: 70; user-select: none; transition: left 180ms cubic-bezier(.2,.8,.2,1), top 180ms cubic-bezier(.2,.8,.2,1); }
.ai-float.dragging { transition: none; }
.ai-mascot { position: relative; width: 66px; height: 66px; display: grid; place-items: center; border: 2px solid rgba(255,255,255,.94); border-radius: 22px 22px 22px 9px; color: #fff; background: linear-gradient(145deg, #54b8ff 0%, #197cf2 48%, #3255dd 100%); box-shadow: 0 12px 28px rgba(14, 87, 197, .3), inset 0 1px 0 rgba(255,255,255,.55), inset 0 -8px 14px rgba(21,65,190,.16); cursor: grab; opacity: .88; transition: transform 160ms ease, opacity 160ms ease, box-shadow 160ms ease, filter 160ms ease; }
.ai-mascot::before { position: absolute; inset: 4px; border: 1px solid rgba(255,255,255,.28); border-radius: 17px 17px 17px 6px; content: ''; pointer-events: none; }
.ai-mascot:hover { opacity: 1; filter: saturate(1.08) brightness(1.04); }
.ai-float.dragging .ai-mascot { transform: scale(1.06); opacity: 1; box-shadow: 0 20px 38px rgba(12, 76, 180, .48), inset 0 1px 0 rgba(255,255,255,.72), inset 0 -8px 14px rgba(21,65,190,.14); cursor: grabbing; }
.ai-mascot-sparkle { position: absolute; right: 10px; bottom: 9px; filter: drop-shadow(0 1px 1px rgba(12, 69, 169, .3)); }
.ai-window { width: min(390px, calc(100vw - 24px)); height: min(590px, calc(100vh - 92px)); display: grid; grid-template-rows: auto auto minmax(0,1fr) auto auto auto; overflow: hidden; border: 1px solid #cbd9ed; border-radius: 18px; background: #fff; box-shadow: 0 22px 60px rgba(15,23,42,.25); user-select: text; }
.ai-window-head { display: grid; grid-template-columns: 38px minmax(0,1fr) 30px; gap: 10px; align-items: center; padding: 13px 14px; color: #fff; background: linear-gradient(105deg,#0f67d8,#4f46e5); cursor: grab; user-select: none; }
.ai-head-avatar { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 12px; background: rgba(255,255,255,.18); }
.ai-window-head div:nth-child(2) { display: grid; gap: 2px; }
.ai-window-head small { opacity: .82; font-size: 10px; }
.head-button { display: grid; place-items: center; width: 30px; height: 30px; border: 0; border-radius: 8px; color: #fff; background: rgba(255,255,255,.13); cursor: pointer; }
.capability-row { display: flex; gap: 6px; padding: 9px 12px; overflow-x: auto; border-bottom: 1px solid #e6edf7; background: #f7faff; }
.capability-row span { white-space: nowrap; padding: 3px 7px; border: 1px solid #dbe4f0; border-radius: 999px; color: #64748b; font-size: 10px; }
.capability-row span.active { color: #0969da; border-color: #bfdbfe; background: #eff6ff; }
.ai-messages { overflow-y: auto; padding: 14px; display: grid; align-content: start; gap: 12px; background: #f7f9fc; }
.ai-message { display: flex; gap: 8px; max-width: 92%; }
.ai-message.user { justify-self: end; }
.ai-message > div { padding: 10px 12px; border-radius: 4px 14px 14px; background: #fff; border: 1px solid #e2e8f0; }
.ai-message.user > div { color: #fff; background: #1769d2; border-color: #1769d2; border-radius: 14px 4px 14px 14px; }
.ai-message p { margin: 0; white-space: pre-wrap; line-height: 1.6; font-size: 13px; }
.ai-message small { display: block; margin-top: 7px; color: #64748b; font-size: 10px; }
.mini-avatar { flex: 0 0 auto; width: 27px; height: 27px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: #4f46e5; font-size: 11px; font-weight: 800; }
.thinking { display: flex; gap: 4px; padding-left: 36px; }
.thinking span { width: 6px; height: 6px; border-radius: 50%; background: #7da9e8; animation: bounce 1s infinite alternate; }
.thinking span:nth-child(2) { animation-delay: .18s; }.thinking span:nth-child(3) { animation-delay: .36s; }
.suggestion-row { display: flex; gap: 7px; padding: 9px 12px; overflow-x: auto; border-top: 1px solid #e6edf7; }
.suggestion-row button { white-space: nowrap; border: 1px solid #c9d8eb; padding: 6px 9px; color: #245ca6; background: #fff; font-size: 11px; cursor: pointer; }
.ai-composer { display: grid; grid-template-columns: minmax(0,1fr) 38px; gap: 8px; padding: 10px 12px 6px; border-top: 1px solid #e6edf7; }
.ai-composer textarea { resize: none; border: 1px solid #cbd5e1; padding: 9px; font: inherit; font-size: 12px; outline: none; }
.ai-composer textarea:focus { border-color: #4f8ee8; box-shadow: 0 0 0 2px #e1edff; }
.ai-composer button { align-self: end; width: 38px; height: 38px; display: grid; place-items: center; border: 0; border-radius: 10px; color: #fff; background: #1769d2; cursor: pointer; }
.ai-composer button:disabled { opacity: .45; cursor: not-allowed; }
.ai-disclaimer { margin: 0; padding: 0 12px 9px; color: #94a3b8; font-size: 9.5px; }
@keyframes bounce { to { transform: translateY(-4px); opacity: .5; } }
@media (max-width: 520px) { .ai-window { height: calc(100vh - 88px); } }
</style>
