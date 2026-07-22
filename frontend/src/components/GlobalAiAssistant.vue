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
        <span v-if="!capabilities.length">能力载入中</span>
        <span v-for="capability in capabilities" :key="capability.capabilityId" :class="{ active: capability.enabled }" :title="capability.unavailableReason || capability.name">
          {{ capability.name }}
        </span>
      </div>
      <div ref="messageList" class="ai-messages">
        <article v-for="item in messages" :key="item.id" :class="['ai-message', item.role]">
          <span v-if="item.role === 'assistant'" class="mini-avatar">知</span>
          <div>
            <p v-if="item.content">{{ cleanAiText(item.content) }}</p>
            <ul v-if="item.activities.length" class="activity-list" aria-label="工具执行记录">
              <li v-for="activity in item.activities" :key="activity.id"><span :class="['activity-dot', activity.status.toLowerCase()]" />{{ activity.summary }}</li>
            </ul>
            <section v-for="action in item.actions" :key="action.actionId" class="action-result" aria-label="AI 操作结果">
              <div class="action-result-head"><CheckCircle2 :size="16" /><strong>{{ action.title }}</strong></div>
              <p>{{ action.summary }}</p>
              <dl v-if="Object.keys(action.preview || {}).length" class="action-preview">
                <template v-for="(value, label) in action.preview" :key="label">
                  <dt>{{ label }}</dt><dd>{{ value }}</dd>
                </template>
              </dl>
              <label v-if="action.status === 'WAITING_CONFIRMATION' && action.confirmationPolicy === 'STRONG_CONFIRM'" class="strong-confirm">
                <span>输入“确认执行”后方可提交</span>
                <input v-model="strongConfirmInputs[action.actionId]" autocomplete="off" placeholder="确认执行" />
              </label>
              <p v-if="action.errorMessage" class="action-error">{{ action.errorMessage }}</p>
              <div class="action-result-foot">
                <span>{{ actionStatusLabel(action) }}</span>
                <div class="action-buttons">
                  <button v-if="action.status === 'WAITING_CONFIRMATION'" type="button" :disabled="actionBusy.has(action.actionId)" @click="cancelAction(action)"><X :size="14" />取消</button>
                  <button v-if="action.status === 'WAITING_CONFIRMATION'" class="confirm" type="button" :disabled="actionBusy.has(action.actionId) || !canConfirmAction(action)" @click="confirmAction(action)"><Check :size="14" />确认执行</button>
                  <button v-if="action.href" type="button" @click="openAction(action)"><ExternalLink :size="14" />查看</button>
                </div>
              </div>
            </section>
            <ul v-if="item.citations.length" class="citation-list" aria-label="回答引用">
              <li v-for="citation in item.citations" :key="`${citation.resourceType}-${citation.resourceId}`">{{ citation.title }}</li>
            </ul>
          </div>
        </article>
        <div v-if="asking" class="thinking"><span /><span /><span /></div>
      </div>
      <div v-if="pendingActions.length" class="pending-strip">
        <span>{{ pendingActions.length }} 个待确认动作</span>
        <button type="button" @click="showPendingActions">查看</button>
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
import { BookOpen, Bot, Check, CheckCircle2, ExternalLink, Minus, Send, Sparkles, X } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { aiApi } from '@/services/api'
import type { AiActionEvent, AiCapabilityEvent, AiCitationVO, AiToolEvent } from '@/services/api/types'
import { useSessionStore } from '@/stores/session'
import { cleanAiText } from '@/utils/aiText'

interface Activity { id: string; status: string; summary: string }
interface Message {
  id: string
  role: 'assistant' | 'user'
  content: string
  activities: Activity[]
  actions: AiActionEvent[]
  citations: AiCitationVO[]
}
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const open = ref(false)
const question = ref('')
const asking = ref(false)
const messageList = ref<HTMLElement | null>(null)
const conversationId = `global-${Date.now().toString(36)}`
const capabilities = ref<AiCapabilityEvent[]>([])
const actionBusy = ref(new Set<string>())
const strongConfirmInputs = ref<Record<string, string>>({})
const persistedActions = ref<AiActionEvent[]>([])
const pendingActions = computed(() => persistedActions.value.filter((item) => item.status === 'WAITING_CONFIRMATION'))
const messages = ref<Message[]>([{ id: 'welcome', role: 'assistant', content: '你好，我是知行 AI 助手。我会根据你的角色、当前页面和实际授权能力回答或协助处理业务。', activities: [], actions: [], citations: [] }])
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
  ? ['总结我当前的教学任务', '进入课程后根据资料生成练习', '分析课程中的学习预警']
  : session.currentRole === 'admin'
    ? ['汇总当前平台指标', '有哪些课程或教师待审核？', '当前选课安排是什么？']
    : ['我接下来有哪些学习任务？', '我的考试和成绩情况如何？', '进入课程后帮我解释资料内容'])
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
function actionStatusLabel(action: AiActionEvent) {
  if (action.status === 'DRAFT_CREATED') return action.requiresConfirmation ? 'AI 草稿 · 待人工确认' : 'AI 草稿'
  if (action.status === 'WAITING_CONFIRMATION') return '等待确认'
  if (action.status === 'SUCCEEDED') return '已完成'
  if (action.status === 'EXECUTING') return '执行中'
  if (action.status === 'PARTIAL_SUCCESS') return '部分完成'
  if (action.status === 'FAILED') return '执行失败'
  if (action.status === 'CANCELLED') return '已取消'
  if (action.status === 'EXPIRED') return '已过期'
  return action.status
}
function openAction(action: AiActionEvent) {
  if (action.href) void router.push(action.href)
}
function replaceAction(updated: AiActionEvent) {
  for (const message of messages.value) {
    const index = message.actions.findIndex((item) => item.actionId === updated.actionId)
    if (index >= 0) message.actions.splice(index, 1, updated)
  }
  const persistedIndex = persistedActions.value.findIndex((item) => item.actionId === updated.actionId)
  if (persistedIndex >= 0) persistedActions.value.splice(persistedIndex, 1, updated)
  else persistedActions.value.unshift(updated)
  persistedActions.value = [...persistedActions.value]
  messages.value = [...messages.value]
}
function setActionBusy(actionId: string, busy: boolean) {
  const next = new Set(actionBusy.value)
  if (busy) next.add(actionId)
  else next.delete(actionId)
  actionBusy.value = next
}
function canConfirmAction(action: AiActionEvent) {
  return action.confirmationPolicy !== 'STRONG_CONFIRM'
    || strongConfirmInputs.value[action.actionId]?.trim() === '确认执行'
}
async function confirmAction(action: AiActionEvent) {
  if (actionBusy.value.has(action.actionId) || !canConfirmAction(action)) return
  setActionBusy(action.actionId, true)
  try { replaceAction(await aiApi.confirmAction(action, strongConfirmInputs.value[action.actionId])) }
  catch (error) { replaceAction({ ...action, errorMessage: error instanceof Error ? error.message : '确认执行失败，请稍后重试。' }) }
  finally { setActionBusy(action.actionId, false) }
}
async function cancelAction(action: AiActionEvent) {
  if (actionBusy.value.has(action.actionId)) return
  setActionBusy(action.actionId, true)
  try { replaceAction(await aiApi.cancelAction(action)) }
  catch (error) { replaceAction({ ...action, errorMessage: error instanceof Error ? error.message : '取消失败，请稍后重试。' }) }
  finally { setActionBusy(action.actionId, false) }
}
function showPendingActions() {
  const existing = messages.value.find((item) => item.id === 'persisted-actions')
  if (existing) existing.actions = [...pendingActions.value]
  else messages.value.push({
    id: 'persisted-actions', role: 'assistant', content: '以下动作仍在等待你的正式确认：',
    activities: [], actions: [...pendingActions.value], citations: [],
  })
  messages.value = [...messages.value]
  openAssistant()
  void scrollToBottom()
}
async function loadPersistedActions() {
  try { persistedActions.value = await aiApi.listActions(20) }
  catch { persistedActions.value = [] }
}
async function ask() {
  const prompt = question.value.trim()
  if (!prompt || asking.value) return
  messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: prompt, activities: [], actions: [], citations: [] })
  question.value = ''
  asking.value = true
  const reply: Message = { id: `a-${Date.now()}`, role: 'assistant', content: '', activities: [], actions: [], citations: [] }
  messages.value.push(reply)
  await scrollToBottom()
  try {
    await aiApi.assistantStream({ question: prompt, courseId: courseId.value, lessonId: lessonId.value, pagePath: route.fullPath, pageTitle: document.title, conversationId }, (event) => {
      if (event.type === 'delta') reply.content += String(event.data ?? '')
      if (event.type === 'capability') capabilities.value = (event.data as AiCapabilityEvent[]).filter((item) => item.enabled)
      if (event.type === 'tool') {
        const tool = event.data as AiToolEvent
        reply.activities.push({ id: `${tool.toolName}-${reply.activities.length}`, status: tool.status, summary: tool.summary || `已调用 ${tool.toolName}` })
      }
      if (event.type === 'action') reply.actions.push(event.data as AiActionEvent)
      if (event.type === 'citation') reply.citations.push(event.data as AiCitationVO)
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
onMounted(() => { restorePosition(); window.addEventListener('resize', onResize); void loadPersistedActions() })
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
.ai-window { width: min(390px, calc(100vw - 24px)); height: min(590px, calc(100vh - 92px)); display: grid; grid-template-rows: auto auto minmax(0,1fr) auto auto auto auto; overflow: hidden; border: 1px solid #cbd9ed; border-radius: 18px; background: #fff; box-shadow: 0 22px 60px rgba(15,23,42,.25); user-select: text; }
.ai-window-head { display: grid; grid-template-columns: 38px minmax(0,1fr) 30px; gap: 10px; align-items: center; padding: 13px 14px; color: #fff; background: linear-gradient(105deg,#0f67d8,#4f46e5); cursor: grab; user-select: none; }
.ai-head-avatar { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 12px; background: rgba(255,255,255,.18); }
.ai-window-head div:nth-child(2) { display: grid; gap: 2px; }
.ai-window-head small { opacity: .82; font-size: 10px; }
.head-button { display: grid; place-items: center; width: 30px; height: 30px; border: 0; border-radius: 8px; color: #fff; background: rgba(255,255,255,.13); cursor: pointer; }
.capability-row { display: flex; gap: 6px; padding: 9px 12px; overflow-x: auto; border-bottom: 1px solid #e6edf7; background: #f7faff; }
.capability-row span { white-space: nowrap; padding: 3px 7px; border: 1px solid #dbe4f0; border-radius: 999px; color: #64748b; font-size: 10px; }
.capability-row span.active { color: #0969da; border-color: #bfdbfe; background: #eff6ff; }
.ai-messages { overflow-y: auto; padding: 14px; display: grid; align-content: start; gap: 12px; background: #f7f9fc; }
.pending-strip { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 7px 12px; border-top: 1px solid #f2df9a; color: #765b00; background: #fff9df; font-size: 11px; }
.pending-strip button { border: 0; padding: 2px 0; color: #1769d2; background: transparent; cursor: pointer; font-weight: 700; }
.ai-message { display: flex; gap: 8px; max-width: 92%; }
.ai-message.user { justify-self: end; }
.ai-message > div { padding: 10px 12px; border-radius: 4px 14px 14px; background: #fff; border: 1px solid #e2e8f0; }
.ai-message.user > div { color: #fff; background: #1769d2; border-color: #1769d2; border-radius: 14px 4px 14px 14px; }
.ai-message p { margin: 0; white-space: pre-wrap; line-height: 1.6; font-size: 13px; }
.mini-avatar { flex: 0 0 auto; width: 27px; height: 27px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: #4f46e5; font-size: 11px; font-weight: 800; }
.activity-list, .citation-list { display: grid; gap: 5px; margin: 8px 0 0; padding: 8px 0 0; border-top: 1px solid #edf1f6; list-style: none; color: #64748b; font-size: 10px; }
.activity-list li { display: flex; gap: 6px; align-items: center; }
.activity-dot { width: 6px; height: 6px; flex: 0 0 auto; border-radius: 50%; background: #94a3b8; }
.activity-dot.completed { background: #15956d; }
.activity-dot.failed { background: #dc4c4c; }
.action-result { display: grid; gap: 7px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #dce8df; }
.action-result-head { display: flex; gap: 7px; align-items: center; color: #176d54; }
.action-result-head strong { min-width: 0; overflow-wrap: anywhere; font-size: 12px; }
.action-result p { color: #536273; font-size: 11px; }
.action-preview { display: grid; grid-template-columns: minmax(70px,auto) minmax(0,1fr); gap: 4px 10px; margin: 0; padding: 8px; background: #f6f8fb; font-size: 10px; }
.action-preview dt { color: #64748b; }
.action-preview dd { min-width: 0; margin: 0; color: #273548; overflow-wrap: anywhere; }
.strong-confirm { display: grid; gap: 5px; color: #8a4308; font-size: 10px; }
.strong-confirm input { min-width: 0; border: 1px solid #e2b979; padding: 6px 8px; color: #502b08; background: #fffaf3; font: inherit; }
.action-error { color: #b42318 !important; }
.action-result-foot { display: flex; justify-content: space-between; gap: 8px; align-items: center; color: #8a6500; font-size: 10px; }
.action-buttons { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 7px; }
.action-result-foot button { display: inline-flex; gap: 4px; align-items: center; border: 0; padding: 3px 0; color: #1769d2; background: transparent; cursor: pointer; }
.action-result-foot button.confirm { color: #176d54; font-weight: 700; }
.action-result-foot button:disabled { cursor: wait; opacity: .55; }
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
