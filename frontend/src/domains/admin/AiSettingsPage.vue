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
          <div class="spread"><span class="muted">检查时间</span><span>{{ status.checkedAt ? formatTime(status.checkedAt) : '—' }}</span></div>
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
          <AppButton variant="secondary" :loading="state.loading.value" @click="loadStatus">测试连接</AppButton>
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
const modeLabel = (mode?: string | null) => !mode ? '—' : (({ FRAMEWORK_ONLY: '仅框架（未接模型）', LLM: '大模型', RAG: '检索增强' } as Record<string, string>)[mode] ?? mode)
onMounted(loadStatus)
</script>
