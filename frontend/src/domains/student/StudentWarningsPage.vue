<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">学习预警</h1>
        <p class="page-subtitle">查看学习进度、作业和成绩规则产生的个人提醒。</p>
      </div>
    </div>

    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div v-if="!state.loading.value" class="stack">
      <article
        v-for="warning in warnings"
        :key="warning.warningId"
        class="panel"
        :class="{ 'notification-resource-target': warning.warningId === selectedWarningId }"
      >
        <div class="spread wrap">
          <div class="row">
            <TriangleAlert :size="20" />
            <strong>{{ warning.warningType.label }}</strong>
          </div>
          <div class="row">
            <StatusBadge :tone="warning.warningLevel.code === 'HIGH' ? 'red' : warning.warningLevel.code === 'MEDIUM' ? 'amber' : 'gray'" :label="`${warning.warningLevel.label}风险`" />
            <StatusBadge :tone="warning.warningStatus.code === 'OPEN' ? 'amber' : 'green'" :label="warning.warningStatus.label" />
          </div>
        </div>
        <p class="warning-context"><BookOpen :size="15" /> {{ warning.courseName }}<span>授课教师：{{ warning.teacherName || '待确认' }}</span></p>
        <div class="notice push-top">
          <div>
            <strong>{{ warning.summary }}</strong>
            <p v-for="evidence in warning.evidences" :key="evidence.evidenceId">{{ evidence.description }}</p>
          </div>
        </div>
        <p v-if="warning.suggestion" class="muted push-top">建议：{{ warning.suggestion }}</p>
        <time class="muted" :datetime="warning.generatedAt">{{ formatTime(warning.generatedAt) }}</time>
      </article>
      <p v-if="warnings.length === 0" class="list-empty">暂无学习预警</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BookOpen, TriangleAlert } from 'lucide-vue-next'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { warningsApi } from '@/services/api'
import type { LearningWarningVO } from '@/services/api'
import { usePageState } from '@/services/pageState'

const route = useRoute()
const state = usePageState()
const warnings = ref<LearningWarningVO[]>([])
const selectedWarningId = computed(() => typeof route.query.warningId === 'string' ? route.query.warningId : '')

async function load() {
  const page = await state.run(() => warningsApi.studentList({ page: 1, size: 100 }))
  if (page) warnings.value = page.records
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(load)
</script>

<style scoped>
.warning-context { display: flex; align-items: center; gap: 7px; margin: 14px 0 0; color: var(--ink); font-weight: 600; }
.warning-context span { margin-left: auto; color: var(--muted); font-size: 12.5px; font-weight: 400; }
</style>
