<template>
  <div>
    <div class="page-header">
      <div><h1 class="page-title">学情预警</h1><p class="page-subtitle">依据学习进度、作业缺交与成绩规则生成预警，处理结论由教师确认。</p></div>
      <AppButton variant="primary" :disabled="!courseId || generating" @click="generateWarnings">{{ generating ? '生成中…' : '生成预警' }}</AppButton>
    </div>

    <div v-if="message" class="toast">{{ message }}</div>
    <div class="filter-bar">
      <label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="loadWarnings"><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label>
      <label class="filter-field"><span>状态</span><select v-model="status" class="select" @change="loadWarnings"><option value="">全部状态</option><option value="OPEN">待处理</option><option value="HANDLED">已处理</option><option value="IGNORED">已忽略</option></select></label>
    </div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="loadWarnings" />

    <div class="kpi-grid">
      <AppMetric label="高风险" :value="byLevel('HIGH')" hint="需重点干预" tone="danger" :icon="TriangleAlert" />
      <AppMetric label="中风险" :value="byLevel('MEDIUM')" hint="需关注" tone="warn" :icon="Activity" />
      <AppMetric label="已处理" :value="resolvedCount" hint="当前课程" tone="success" :icon="CircleCheck" />
    </div>

    <div class="stack">
      <article v-for="warning in warningRows" :key="warning.warningId" class="panel" :class="{ 'notification-resource-target': warning.warningId === selectedWarningId }">
        <div class="spread wrap">
          <div class="row">
            <span class="rank-index">{{ (warning.studentName || warning.studentId).slice(0, 1) }}</span>
            <div><strong style="color: var(--ink)">{{ warning.studentName || warning.studentId }}</strong><p class="muted" style="margin: 2px 0 0; font-size: 12.5px">{{ courseName(warning.courseId) }} · {{ warning.warningType.label }}</p></div>
          </div>
          <div class="row"><StatusBadge :tone="warning.warningLevel.code === 'HIGH' ? 'red' : warning.warningLevel.code === 'MEDIUM' ? 'amber' : 'gray'" :label="`${warning.warningLevel.label}风险`" /><StatusBadge :tone="warning.warningStatus.code === 'OPEN' ? 'amber' : 'green'" :label="warning.warningStatus.label" /></div>
        </div>

        <div class="notice push-top"><div><strong>{{ warning.summary }}</strong><p v-for="evidence in warning.evidences" :key="evidence.evidenceId">{{ evidence.description }}</p></div></div>
        <p v-if="warning.suggestion" class="muted push-top">建议：{{ warning.suggestion }}</p>
        <div v-if="!aiDrafts[warning.warningId]" class="ai-card push-top">
          <div class="spread wrap"><div class="row"><span class="ai-chip">AI</span><strong style="color: var(--ink)">风险解读</strong></div><AiAssistButton label="生成 AI 解读" :loading="aiLoadingId === warning.warningId" @click="draftExplanation(warning)" /></div>
          <p v-if="aiErrors[warning.warningId]" class="form-error" style="margin-top: 10px">{{ aiErrors[warning.warningId] }}</p>
        </div>
        <AiResultPanel v-else :result="aiDrafts[warning.warningId]" :adopt-label="warning.warningStatus.code === 'OPEN' ? '采用为干预记录' : undefined" class="push-top" @adopt="remarks[warning.warningId] = $event" @regenerate="draftExplanation(warning)" />

        <template v-if="warning.warningStatus.code === 'OPEN'">
          <textarea v-model="remarks[warning.warningId]" class="textarea push-top" placeholder="填写干预记录或忽略原因" />
          <div class="row push-top"><AppButton variant="secondary" @click="handleWarning(warning, 'IGNORED')">忽略预警</AppButton><AppButton variant="primary" @click="handleWarning(warning, 'HANDLED')">记录干预并关闭</AppButton></div>
        </template>
        <div v-else-if="warning.handleRemark" class="notice push-top"><div><strong>处理记录</strong><p>{{ warning.handleRemark }}</p></div></div>
      </article>
      <p v-if="!state.loading.value && !warningRows.length" class="list-empty">当前筛选条件下暂无预警</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Activity, CircleCheck, TriangleAlert } from 'lucide-vue-next'
import AiAssistButton from '@/components/AiAssistButton.vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import AppMetric from '@/components/AppMetric.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AiResultPanel from '@/components/AiResultPanel.vue'
import { aiApi, teacherCoursesApi, warningsApi } from '@/services/api'
import type { LearningWarningVO, TeacherCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { aiDraftToResult } from '@/services/aiDraft'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const state = usePageState()
const route = useRoute()
const courses = ref<TeacherCourseListItemVO[]>([])
const warningRows = ref<LearningWarningVO[]>([])
const courseId = ref('')
const status = ref('')
const generating = ref(false)
const message = ref('')
const remarks = reactive<Record<string, string>>({})
const aiDrafts = reactive<Record<string, AiResult>>({})
const aiErrors = reactive<Record<string, string>>({})
const aiLoadingId = ref('')
async function draftExplanation(warning: LearningWarningVO) {
  aiLoadingId.value = warning.warningId
  aiErrors[warning.warningId] = ''
  try {
    const draft = await aiApi.warningExplanation(warning.warningId)
    aiDrafts[warning.warningId] = aiDraftToResult(draft, 'risk', 'AI 风险解读')
  } catch (caught) {
    aiErrors[warning.warningId] = aiErrorMessage(caught)
  } finally { aiLoadingId.value = '' }
}
const selectedWarningId = computed(() => typeof route.query.warningId === 'string' ? route.query.warningId : '')
const selectedCourseId = computed(() => typeof route.query.courseId === 'string' ? route.query.courseId : '')

const byLevel = (level: string) => warningRows.value.filter((item) => item.warningLevel.code === level && item.warningStatus.code === 'OPEN').length
const resolvedCount = computed(() => warningRows.value.filter((item) => item.warningStatus.code !== 'OPEN').length)
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

async function load() {
  const page = await state.run(() => teacherCoursesApi.list({ page: 1, size: 100 }))
  if (!page) return
  courses.value = page.records
  courseId.value = page.records.some((item) => item.courseId === selectedCourseId.value)
    ? selectedCourseId.value
    : courseId.value || page.records[0]?.courseId || ''
  await loadWarnings()
}

async function loadWarnings() {
  if (!courseId.value) { warningRows.value = []; return }
  const page = await state.run(() => warningsApi.teacherList(courseId.value, { page: 1, size: 100, warningStatus: status.value || undefined }))
  if (page) warningRows.value = page.records
}

async function generateWarnings() {
  if (!courseId.value) return
  generating.value = true
  const result = await state.run(() => warningsApi.generate(courseId.value, { warningTypes: ['PROGRESS_LAG', 'MISSING_ASSIGNMENT', 'LOW_SCORE'] }))
  generating.value = false
  if (result) { flash(`已生成 ${result.createdCount} 条预警，跳过 ${result.skippedCount} 条`); await loadWarnings() }
}

async function handleWarning(warning: LearningWarningVO, action: 'HANDLED' | 'IGNORED') {
  const updated = await state.run(() => warningsApi.handle(warning.warningId, { action, remark: remarks[warning.warningId]?.trim() || null, version: warning.version }))
  if (updated) { flash(action === 'HANDLED' ? '已记录干预并关闭预警' : '已忽略该预警'); await loadWarnings() }
}

onMounted(load)
watch([selectedWarningId, selectedCourseId], () => { void load() })
</script>
