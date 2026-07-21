<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">数据看板</h1><p class="page-subtitle">平台治理与教学运行实时汇总</p></div><AppButton class="announcement-cta" variant="primary" @click="router.push('/admin/content')"><span class="announcement-cta-label"><Megaphone :size="22" />发布公告<ArrowRight :size="25" /></span></AppButton></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <template v-if="overview">
      <div class="kpi-grid"><AppMetric label="总用户" :value="overview.statistics.totalUsers" hint="学生、教师、管理员" :icon="Users" /><AppMetric label="运行课程" :value="overview.statistics.publishedCourses" hint="已发布课程" tone="success" :icon="BookOpen" /><AppMetric label="待审核课程" :value="overview.statistics.pendingCourseReviews" hint="需管理员处理" tone="warn" :icon="ShieldCheck" /><AppMetric label="开放预警" :value="overview.statistics.openWarnings" hint="需教学干预" tone="danger" :icon="TriangleAlert" /></div>
      <div class="grid cols-2">
        <section class="panel"><h2 class="panel-title">平台运行</h2><div class="stack"><div class="notice"><div><strong>活跃选课</strong><p>{{ overview.statistics.activeEnrollments }} 人次</p></div></div><div class="notice"><div><strong>已提交作业</strong><p>{{ overview.statistics.submittedAssignments }} 份</p></div></div><div class="notice"><div><strong>已发布考试</strong><p>{{ overview.statistics.publishedExams }} 场</p></div></div></div></section>
        <section class="panel"><h2 class="panel-title">治理待办</h2><div class="stack"><div class="notice"><div style="flex: 1"><strong>待审核课程</strong><p>课程资料与章节大纲待管理员审核</p></div><AppButton variant="secondary" @click="router.push('/admin/course-reviews')">{{ overview.statistics.pendingCourseReviews }} 项</AppButton></div><div class="ai-card"><div class="spread wrap"><div class="row"><span class="ai-chip">AI</span><strong style="color: var(--ink)">AI 运行状态</strong></div><StatusBadge :tone="overview.ai?.modelConfigured ? 'green' : 'red'" :label="overview.ai?.modelConfigured ? '可用' : '不可用'" /></div><p class="push-top">提供方 {{ overview.ai?.provider || '未知' }} · 模型 {{ overview.ai?.model || '未配置' }}。向量库 {{ overview.ai?.vectorStoreConfigured ? '已配置' : '未配置' }}。</p></div></div></section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'; import { useRouter } from 'vue-router'; import { ArrowRight, BookOpen, Megaphone, ShieldCheck, TriangleAlert, Users } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppMetric from '@/components/AppMetric.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { loadAdminOverview, type AdminOverview } from '@/services/adapters/adminAdapter'; import { usePageState } from '@/services/pageState'; import { useSessionStore } from '@/stores/session'
const router = useRouter(); const state = usePageState(); const overview = ref<AdminOverview | null>(null); const session = useSessionStore()
async function load() { const data = await state.run(() => loadAdminOverview({ includeUsers: session.isSuperAdmin })); if (data) overview.value = data }
onMounted(load)
</script>
