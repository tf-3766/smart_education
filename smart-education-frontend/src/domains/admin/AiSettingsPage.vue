<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">AI 设置</h1><p class="page-subtitle">配置大模型密钥并查看服务状态。密钥仅保存在当前浏览器。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <div class="grid cols-2">
      <section class="panel">
        <h2 class="panel-title">服务状态</h2>
        <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="loadStatus" />
        <div v-if="status && !state.loading.value" class="stack">
          <div class="spread"><span class="muted">提供方</span><span class="cell-strong">{{ providerLabel(status.provider) }}</span></div>
          <div class="spread"><span class="muted">模型</span><span>{{ status.model || '—' }}</span></div>
          <div class="spread"><span class="muted">可用</span><StatusBadge :tone="status.available ? 'green' : 'red'" :label="status.available ? '可用' : '不可用'" /></div>
          <div class="spread"><span class="muted">模式</span><span>{{ modeLabel(status.mode) }}</span></div>
          <div v-if="!status.available && status.detail" class="notice" style="margin-top: 4px"><div><strong>失败原因</strong><p class="form-error" role="alert" style="word-break: break-all">{{ status.detail }}</p></div></div>
          <div class="spread"><span class="muted">检查时间</span><span>{{ status.checkedAt ? formatTime(status.checkedAt) : '—' }}</span></div>

          <template v-if="hasCapabilities">
            <hr style="border: none; border-top: 1px solid var(--line); margin: 6px 0 2px" />
            <div class="muted" style="font-weight: 650; color: var(--ink)">框架能力</div>
            <div v-if="status.framework" class="spread"><span class="muted">框架</span><span>{{ status.framework }}<template v-if="status.frameworkVersion"> {{ status.frameworkVersion }}</template></span></div>
            <div class="spread"><span class="muted">RAG 检索增强</span><StatusBadge :tone="status.ragEnabled ? 'green' : 'gray'" :label="capLabel(status.ragEnabled, ragModeLabel(status.ragMode))" /></div>
            <div class="spread"><span class="muted">向量库</span><span>{{ vectorStoreLabel }}</span></div>
            <div v-if="status.embeddingProvider" class="spread"><span class="muted">嵌入模型</span><span>{{ providerLabel(status.embeddingProvider) }}</span></div>
            <div class="spread"><span class="muted">工具调用</span><StatusBadge :tone="status.toolCallingEnabled ? 'green' : 'gray'" :label="capLabel(status.toolCallingEnabled)" /></div>
            <div class="spread"><span class="muted">对话记忆（多轮）</span><StatusBadge :tone="status.conversationMemoryEnabled ? 'green' : 'gray'" :label="capLabel(status.conversationMemoryEnabled)" /></div>
          </template>
        </div>
      </section>

      <section class="panel">
        <h2 class="panel-title">大模型密钥</h2>
        <label class="field-label" for="ai-key">API 密钥（阿里百炼 / DashScope）</label>
        <div class="login-input-wrap">
          <input id="ai-key" v-model.trim="keyInput" class="input" :type="show ? 'text' : 'password'" autocomplete="off" placeholder="sk-..." />
          <button type="button" class="login-pwd-toggle" :aria-label="show ? '隐藏密钥' : '显示密钥'" @click="show = !show"><component :is="show ? EyeOff : Eye" :size="18" /></button>
        </div>
        <label class="field-label push-top" for="ai-model">对话模型</label>
        <select id="ai-model" v-model="modelInput" class="select">
          <option value="">跟随后端默认（qwen-plus）</option>
          <option value="qwen-plus">qwen-plus（均衡，推荐）</option>
          <option value="qwen-turbo">qwen-turbo（更快更省）</option>
          <option value="qwen-max">qwen-max（效果最强）</option>
          <option value="qwen-long">qwen-long（长文本）</option>
        </select>
        <p class="muted" style="margin: 10px 0 0; font-size: 12.5px">密钥与模型仅保存在当前浏览器 localStorage，不会上传服务器，仅随 AI 请求头发送。请勿在公共设备保存。</p>
        <p v-if="hasStoredKey" class="muted" style="margin: 6px 0 0; font-size: 12.5px">当前已保存密钥：{{ masked }}<template v-if="storedModel"> · 模型：{{ storedModel }}</template></p>
        <div class="form-actions">
          <AppButton variant="secondary" :disabled="!hasStoredKey" @click="clear">清除</AppButton>
          <AppButton variant="secondary" :loading="state.loading.value" :disabled="!keyInput" @click="testConnection">测试连接</AppButton>
          <AppButton variant="primary" :disabled="!dirty" @click="save">保存</AppButton>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { aiApi } from '@/services/api'
import type { AiServiceStatusVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { clearAiKey, getAiKey, getAiModel, setAiKey, setAiModel } from '@/services/aiKey'

const state = usePageState()
const status = ref<AiServiceStatusVO | null>(null)
const storedKey = ref(getAiKey())
const storedModel = ref(getAiModel())
const keyInput = ref(storedKey.value)
const modelInput = ref(storedModel.value)
const show = ref(false)
const message = ref('')

const hasStoredKey = computed(() => storedKey.value.length > 0)
const masked = computed(() => storedKey.value ? `${storedKey.value.slice(0, 3)}••••${storedKey.value.slice(-4)}` : '')
const dirty = computed(() => (keyInput.value && keyInput.value !== storedKey.value) || modelInput.value !== storedModel.value)

function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

async function loadStatus() {
  const data = await state.run(() => aiApi.adminStatus())
  if (data) status.value = data
}
// 测试连接：用当前「输入框」里的密钥与模型即时探测（无需先保存），并回显可用/失败原因。
async function testConnection() {
  const data = await state.run(() => aiApi.adminStatus({ apiKey: keyInput.value, model: modelInput.value }))
  if (data) {
    status.value = data
    flash(data.available ? '连接成功，密钥可用' : '连接失败，请查看失败原因')
  }
}
function save() {
  setAiKey(keyInput.value)
  setAiModel(modelInput.value)
  storedKey.value = getAiKey()
  storedModel.value = getAiModel()
  flash('设置已保存到本浏览器')
  void loadStatus()
}
function clear() {
  clearAiKey()
  setAiModel('')
  storedKey.value = ''
  storedModel.value = ''
  keyInput.value = ''
  modelInput.value = ''
  flash('设置已清除')
}
const formatTime = formatDateTime
const providerLabel = (provider: string) => (({ dashscope: '阿里百炼（DashScope）', openai: 'OpenAI', fallback: '未接入（占位回退）', none: '未配置' } as Record<string, string>)[provider] ?? provider)
const modeLabel = (mode?: string | null) => !mode ? '—' : (({ FRAMEWORK_ONLY: '仅框架（未接模型）', LLM: '大模型', RAG: '检索增强', BYO_KEY: '自带密钥', MODEL: '大模型', UNAVAILABLE: '不可用' } as Record<string, string>)[mode] ?? mode)

// 框架能力：仅当后端返回了扩展能力字段时才展示该区块。
const hasCapabilities = computed(() => Boolean(status.value && (status.value.framework || status.value.ragEnabled != null || status.value.toolCallingEnabled != null || status.value.conversationMemoryEnabled != null)))
const capLabel = (on?: boolean | null, extra?: string) => (on ? (extra ? `已开启 · ${extra}` : '已开启') : '未开启')
const ragModeLabel = (mode?: string | null) => !mode ? '' : (({ HYBRID: '混合检索', VECTOR: '向量检索', KEYWORD: '关键词', DISABLED: '' } as Record<string, string>)[mode] ?? mode)
const vectorStoreLabel = computed(() => {
  const s = status.value
  if (!s || !s.vectorStoreConfigured) return '未配置'
  const provider = s.vectorStoreProvider || '向量库'
  return s.vectorCollection ? `${provider} · ${s.vectorCollection}` : provider
})
onMounted(loadStatus)
</script>
