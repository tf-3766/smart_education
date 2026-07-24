<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">课程治理</h1><p class="page-subtitle">按状态浏览全部课程，审核教师提交的课程（驳回必须填写原因）。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <div class="toolbar"><div class="seg" role="tablist" aria-label="课程审核状态筛选">
      <button v-for="tab in filters" :key="tab.value" type="button" :class="{ active: reviewFilter === tab.value }" :aria-selected="reviewFilter === tab.value" role="tab" @click="setFilter(tab.value)">{{ tab.label }}</button>
    </div></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section v-if="reviews.length" class="panel ai-governance-panel">
      <div class="spread wrap"><div><strong>AI P1 · 课程合规检查</strong><p class="muted">核对审核状态、课时与资料完整度；结果仅作为管理员逐课审核依据。</p></div><AppButton variant="secondary" :loading="complianceLoading" @click="runComplianceCheck">检查 {{ Math.min(reviews.length, 50) }} 门</AppButton></div>
      <AiGenerationProgress :active="complianceLoading" label="正在检查课程合规性" class="push-top" />
      <div v-if="governanceDraft" class="governance-results push-top"><div class="spread"><strong>成功 {{ governanceDraft.successCount }} · 失败 {{ governanceDraft.failureCount }} · 需复核 {{ governanceDraft.reviewCount }}</strong><small>{{ governanceDraft.status === 'FRAMEWORK_ONLY' ? '规则兜底模式' : 'AI 草稿' }}</small></div><div v-for="item in governanceDraft.courseCompliance" :key="item.courseId" class="notice push-top"><div><strong>{{ item.courseName }} · 准备度 {{ item.readinessScore }}%</strong><p>课时 {{ item.lessonCount }} · 资料 {{ item.materialCount }}<template v-if="item.reasons.length">；{{ item.reasons.join('；') }}</template></p><small v-if="item.evidence.length" class="muted">依据：{{ item.evidence.join('；') }}</small></div><StatusBadge :tone="item.failed ? 'red' : item.reviewRequired ? 'amber' : 'green'" :label="item.failed ? '检查失败' : item.reviewRequired ? '需复核' : '可进入审核'" /></div></div>
      <div v-if="governanceDraft?.courseCompliance.length" class="version-snapshot push-top">
        <small v-for="item in governanceDraft.courseCompliance" :key="`version-${item.courseId}`" class="muted">
          {{ item.courseCode || item.courseId }} · 目标版本 {{ item.targetVersion ?? '-' }} · 分类 {{ item.categoryId ?? '-' }} · 学期 {{ item.term ?? '-' }} · 院系 {{ item.department ?? '-' }} · 学分 {{ item.credit ?? '-' }}
          · 课程 {{ item.startAt ? formatTime(item.startAt) : '-' }} 至 {{ item.endAt ? formatTime(item.endAt) : '-' }}
          · 选课 {{ item.enrollmentOpenAt ? formatTime(item.enrollmentOpenAt) : '-' }} 至 {{ item.enrollmentCloseAt ? formatTime(item.enrollmentCloseAt) : '-' }}
          · 简介 {{ item.summary ?? '-' }}
        </small>
      </div>
    </section>
    <div class="grid cols-2">
      <section class="panel flush">
        <div class="panel-head"><h2>课程列表</h2><span class="count">共 {{ reviews.length }} 门</span></div>
        <button v-for="course in reviews" :key="course.courseId" class="list-nav" :class="{ active: course.courseId === currentId }" style="margin: 0; padding: 14px 20px; border-bottom: 1px solid var(--line-soft)" @click="select(course.courseId)">
          <div class="spread"><span class="cell-strong">{{ course.name }}</span><StatusBadge :tone="reviewTone(course.reviewStatus.code)" :label="course.reviewStatus.label" /></div>
          <small>{{ course.ownerTeacherName }} · {{ course.courseCode }} · {{ course.courseStatus.label }}</small>
        </button>
        <p v-if="!reviews.length" class="list-empty">该状态下暂无课程</p>
      </section>
      <section v-if="detail" class="panel">
        <div class="spread wrap"><h2 class="panel-title" style="margin: 0">{{ detail.course.name }}</h2><span class="row"><StatusBadge tone="blue" :label="detail.course.status.label" /><StatusBadge :tone="reviewTone(detail.course.reviewStatus.code)" :label="detail.course.reviewStatus.label" /></span></div>
        <p class="muted push-top">负责人 {{ detail.course.ownerTeacherName }} · 学期 {{ detail.course.term || '—' }}</p>
        <div class="notice push-top"><div><strong>课程简介</strong><p>{{ detail.course.summary || '暂无简介' }}</p></div></div>
        <template v-if="detail.course.reviewStatus.code === 'PENDING'">
          <label class="field-label push-top" for="cr-reason">审核意见（驳回时必填）</label><textarea id="cr-reason" v-model="reason" class="textarea" placeholder="填写审核意见或驳回原因" />
          <div class="form-actions"><AppButton variant="secondary" :loading="state.loading.value" :disabled="!reason.trim()" @click="decide('reject')">驳回</AppButton><AppButton variant="primary" :loading="state.loading.value" @click="decide('approve')">通过</AppButton></div>
        </template>
        <p v-else class="muted push-top">该课程当前无待审核操作。</p>
        <template v-if="detail.history.length"><p class="field-label push-top">审核历史</p><div v-for="item in detail.history" :key="item.reviewId" class="notice push-top"><div><div class="spread"><strong>{{ item.reviewStatus.label }} · {{ item.reviewerName }}</strong><small class="muted">{{ formatTime(item.reviewedAt) }}</small></div><p v-if="item.reason || item.remark">{{ item.reason || item.remark }}</p></div></div></template>
      </section>
      <section v-else class="panel empty-state">请选择左侧课程查看详情。</section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { onMounted, ref } from 'vue'; import AiGenerationProgress from '@/components/AiGenerationProgress.vue'; import AppButton from '@/components/AppButton.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { aiApi, courseReviewsApi } from '@/services/api'; import type { AdminGovernanceDraftVO, CourseReviewDetailVO, CourseReviewListItemVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'

type ReviewFilter = '' | 'PENDING' | 'APPROVED' | 'REJECTED'
const filters: { value: ReviewFilter; label: string }[] = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: '', label: '全部' },
]
const state = usePageState(); const reviews = ref<CourseReviewListItemVO[]>([]); const currentId = ref(''); const detail = ref<CourseReviewDetailVO | null>(null); const reason = ref(''); const message = ref(''); const reviewFilter = ref<ReviewFilter>('PENDING')
const complianceLoading = ref(false); const governanceDraft = ref<AdminGovernanceDraftVO | null>(null)
const formatTime = formatDateTime
const reviewTone = (code: string) => code === 'APPROVED' ? 'green' : code === 'REJECTED' ? 'red' : code === 'PENDING' ? 'amber' : 'gray'
async function load() {
  const page = await state.run(() => courseReviewsApi.list({ page: 1, size: 100, ...(reviewFilter.value ? { reviewStatus: reviewFilter.value } : {}) }))
  if (page) {
    reviews.value = page.records
    const id = page.records.find((item) => item.courseId === currentId.value)?.courseId ?? page.records[0]?.courseId ?? ''
    if (id) await select(id); else { currentId.value = ''; detail.value = null }
  }
}
function setFilter(value: ReviewFilter) { if (reviewFilter.value === value) return; reviewFilter.value = value; currentId.value = ''; void load() }
async function select(id: string) { currentId.value = id; reason.value = ''; const data = await state.run(() => courseReviewsApi.detail(id)); if (data) detail.value = data }
async function decide(action: 'approve' | 'reject') { if (!currentId.value) return; const result = await state.run(() => action === 'approve' ? courseReviewsApi.approve(currentId.value, { remark: reason.value || null }) : courseReviewsApi.reject(currentId.value, { reason: reason.value })); if (result) { message.value = action === 'approve' ? '课程审核已通过' : '课程已驳回'; await load() } }
async function runComplianceCheck() { complianceLoading.value = true; try { governanceDraft.value = await aiApi.adminGovernanceDraft({ courseIds: reviews.value.slice(0, 50).map((course) => course.courseId) }) } catch (error) { message.value = error instanceof Error ? error.message : '课程合规检查失败，请稍后重试' } finally { complianceLoading.value = false } }
onMounted(load)
</script>

<style scoped>
.version-snapshot { display: grid; gap: 4px; }
</style>
