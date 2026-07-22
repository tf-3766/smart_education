<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">数据看板</h1><p class="page-subtitle">平台治理与教学运行实时汇总</p></div><AppButton class="announcement-cta" variant="primary" @click="router.push('/admin/content')"><span class="announcement-cta-label"><Megaphone :size="22" />发布公告<ArrowRight :size="25" /></span></AppButton></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <template v-if="overview">
      <div class="kpi-grid"><AppMetric label="总用户" :value="overview.statistics.totalUsers" hint="学生、教师、管理员" :icon="Users" /><AppMetric label="运行课程" :value="overview.statistics.publishedCourses" hint="已发布课程" tone="success" :icon="BookOpen" /><AppMetric label="待审核课程" :value="overview.statistics.pendingCourseReviews" hint="需管理员处理" tone="warn" :icon="ShieldCheck" /><AppMetric label="开放预警" :value="overview.statistics.openWarnings" hint="需教学干预" tone="danger" :icon="TriangleAlert" /></div>
      <section class="operations-brief" aria-labelledby="operations-brief-title">
        <div class="operations-brief-head">
          <div>
            <span class="ai-brief-chip"><Sparkles :size="14" />AI P0 自动流</span>
            <h2 id="operations-brief-title">每日运营简报</h2>
            <p>汇总用户、课程、审核、预警与 AI 服务状态，识别异常并给出可确认的处理方案。</p>
          </div>
          <AppButton variant="primary" :loading="briefLoading" @click="generateOperationsBrief">{{ operationsBrief ? '刷新简报' : '生成今日简报' }}</AppButton>
        </div>
        <label class="brief-instruction"><span>关注重点（可选）</span><input v-model="briefInstruction" class="input" placeholder="例如：优先检查高风险课程和 AI 服务异常" /></label>
        <p v-if="briefError" class="form-error" role="alert">{{ briefError }}</p>
        <AiResultPanel v-if="operationsBrief" :result="operationsBrief" :allow-adopt="false" />
        <footer class="brief-safety">
          <span><ShieldCheck :size="15" />简报只读；教师审批、批量通知、课程下线仍须强确认并完整审计</span>
          <button v-if="operationsBrief" class="text-link" type="button" @click="openHandlingPlan">让 AI 生成处置计划 <ArrowRight :size="14" /></button>
        </footer>
      </section>
      <div class="grid cols-2">
        <section class="panel"><h2 class="panel-title">平台运行</h2><div class="stack"><div class="notice"><div><strong>活跃选课</strong><p>{{ overview.statistics.activeEnrollments }} 人次</p></div></div><div class="notice"><div><strong>已提交作业</strong><p>{{ overview.statistics.submittedAssignments }} 份</p></div></div><div class="notice"><div><strong>已发布考试</strong><p>{{ overview.statistics.publishedExams }} 场</p></div></div></div></section>
        <section class="panel"><h2 class="panel-title">治理待办</h2><div class="stack"><div class="notice"><div style="flex: 1"><strong>待审核课程</strong><p>课程资料与章节大纲待管理员审核</p></div><AppButton variant="secondary" @click="router.push('/admin/course-reviews')">{{ overview.statistics.pendingCourseReviews }} 项</AppButton></div><div class="ai-card"><div class="spread wrap"><div class="row"><span class="ai-chip">AI</span><strong style="color: var(--ink)">AI 运行状态</strong></div><StatusBadge :tone="overview.ai?.modelConfigured ? 'green' : 'red'" :label="overview.ai?.modelConfigured ? '可用' : '不可用'" /></div><p class="push-top">提供方 {{ overview.ai?.provider || '未知' }} · 模型 {{ overview.ai?.model || '未配置' }}。向量库 {{ overview.ai?.vectorStoreConfigured ? '已配置' : '未配置' }}。</p></div></div></section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'; import { useRouter } from 'vue-router'; import { ArrowRight, BookOpen, Megaphone, ShieldCheck, Sparkles, TriangleAlert, Users } from 'lucide-vue-next'
import AiResultPanel from '@/components/AiResultPanel.vue'; import AppButton from '@/components/AppButton.vue'; import AppMetric from '@/components/AppMetric.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { loadAdminOverview, type AdminOverview } from '@/services/adapters/adminAdapter'; import { aiApi } from '@/services/api'; import { aiDraftToResult } from '@/services/aiDraft'; import { aiErrorMessage } from '@/services/aiHint'; import { usePageState } from '@/services/pageState'; import { useSessionStore } from '@/stores/session'; import type { AiResult } from '@/types/domain'
const router = useRouter(); const state = usePageState(); const overview = ref<AdminOverview | null>(null); const session = useSessionStore()
const briefInstruction = ref(''); const operationsBrief = ref<AiResult | null>(null); const briefLoading = ref(false); const briefError = ref('')
async function load() { const data = await state.run(() => loadAdminOverview({ includeUsers: session.isSuperAdmin })); if (data) overview.value = data }
async function generateOperationsBrief() {
  briefLoading.value = true; briefError.value = ''
  try {
    const draft = await aiApi.adminOperationsBrief(briefInstruction.value.trim() || undefined)
    operationsBrief.value = aiDraftToResult(draft, 'operations-brief', 'AI 每日运营简报')
  } catch (error) { briefError.value = aiErrorMessage(error) }
  finally { briefLoading.value = false }
}
function openHandlingPlan() {
  window.dispatchEvent(new CustomEvent('smart-education:ai-compose', {
    detail: { prompt: '根据刚生成的每日运营简报，先列出异常项的处理计划、风险等级和预览变化。不要直接执行；教师审批、批量通知、课程下线等操作必须等待我输入“确认执行”。' },
  }))
}
onMounted(load)
</script>

<style scoped>
.operations-brief { display: grid; gap: 16px; margin: 18px 0; padding: 22px; overflow: hidden; border: 1px solid #cfe0f5; border-radius: 20px; background: linear-gradient(135deg, rgba(239,246,255,.96), rgba(255,255,255,.98) 58%, rgba(238,242,255,.94)); box-shadow: 0 12px 30px rgba(37,99,235,.08); }
.operations-brief-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; }
.operations-brief-head h2 { margin: 8px 0 5px; color: var(--ink); font-size: 21px; }
.operations-brief-head p { margin: 0; color: var(--muted); line-height: 1.65; }
.ai-brief-chip { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; color: #1d4ed8; background: #dbeafe; font-size: 12px; font-weight: 750; }
.brief-instruction { display: grid; gap: 7px; color: var(--ink); font-size: 13px; font-weight: 650; }
.brief-safety { display: flex; justify-content: space-between; align-items: center; gap: 14px; padding-top: 13px; border-top: 1px solid #dbe7f5; color: var(--muted); font-size: 12px; }
.brief-safety > span, .brief-safety .text-link { display: inline-flex; align-items: center; gap: 6px; }
.brief-safety .text-link { border: 0; background: transparent; cursor: pointer; font-weight: 700; }
@media (max-width: 720px) {
  .operations-brief-head, .brief-safety { align-items: stretch; flex-direction: column; }
  .operations-brief-head .app-button { width: 100%; }
}
</style>
