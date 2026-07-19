<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">AI 设置</h1>
        <p class="page-subtitle">查看当前后端模型与向量库配置状态。</p>
      </div>
      <AppButton variant="secondary" :loading="state.loading.value" @click="loadStatus">刷新状态</AppButton>
    </div>

    <div class="grid cols-2">
      <section class="panel">
        <h2 class="panel-title">服务状态</h2>
        <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="loadStatus" />
        <div v-if="status && !state.loading.value" class="stack">
          <div class="spread"><span class="muted">服务</span><StatusBadge :tone="status.serviceStatus === 'UP' ? 'green' : 'red'" :label="status.serviceStatus" /></div>
          <div class="spread"><span class="muted">框架</span><span>{{ status.framework }} {{ status.frameworkVersion }}</span></div>
          <div class="spread"><span class="muted">提供方</span><span class="cell-strong">{{ providerLabel(status.provider) }}</span></div>
          <div class="spread"><span class="muted">模型</span><span>{{ status.model || '—' }}</span></div>
          <div class="spread"><span class="muted">模型配置</span><StatusBadge :tone="status.modelConfigured ? 'green' : 'gray'" :label="status.modelConfigured ? '已配置' : '未配置'" /></div>
          <div class="spread"><span class="muted">向量库</span><StatusBadge :tone="status.vectorStoreConfigured ? 'green' : 'gray'" :label="status.vectorStoreConfigured ? '已配置' : '未配置'" /></div>
          <div class="spread"><span class="muted">检查时间</span><span>{{ formatTime(status.checkedAt) }}</span></div>
        </div>
      </section>

      <section class="panel">
        <h2 class="panel-title">模型配置方式</h2>
        <div class="notice">
          <div>
            <strong>密钥由后端统一管理</strong>
            <p class="muted" style="margin: 6px 0 0">浏览器不会保存或发送模型密钥。请在启动 AI 服务前通过环境变量配置，修改后重启服务并刷新状态。</p>
          </div>
        </div>
        <div class="stack push-top">
          <div class="spread"><span class="muted">启用模型</span><code>AI_CHAT_PROVIDER=openai</code></div>
          <div class="spread"><span class="muted">访问密钥</span><code>DASHSCOPE_API_KEY</code></div>
          <div class="spread"><span class="muted">模型名称</span><code>DASHSCOPE_CHAT_MODEL</code></div>
          <div class="spread"><span class="muted">兼容地址</span><code>DASHSCOPE_BASE_URL</code></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { aiApi } from '@/services/api'
import type { AiServiceStatusVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { formatDateTime } from '@/utils/datetime'

const state = usePageState()
const status = ref<AiServiceStatusVO | null>(null)

async function loadStatus() {
  const data = await state.run(() => aiApi.adminStatus())
  if (data) status.value = data
}

const providerLabel = (provider: string) => (({
  'aliyun-bailian': '阿里云百炼',
  fallback: '未接入（框架回退）',
} as Record<string, string>)[provider] ?? provider)
const formatTime = (value: string) => formatDateTime(value)

onMounted(loadStatus)
</script>