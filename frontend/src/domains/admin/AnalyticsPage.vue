<template>
  <div><div class="page-header"><div><h1 class="page-title">数据统计</h1><p class="page-subtitle">课程、用户、作业、考试与预警的业务统计。</p></div></div><AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <template v-if="stats"><div class="kpi-grid"><AppMetric label="课程数量" :value="stats.totalCourses" hint="全部课程" :icon="BookOpen" /><AppMetric label="活跃选课" :value="stats.activeEnrollments" hint="有效选课人次" tone="success" :icon="Users" /><AppMetric label="已发布作业" :value="stats.publishedAssignments" hint="教学任务" tone="warn" :icon="ClipboardList" /><AppMetric label="开放预警" :value="stats.openWarnings" hint="需干预" tone="danger" :icon="TriangleAlert" /></div>
    <section class="panel flush"><div class="panel-head"><h2>统计明细</h2><span class="count">实时数据</span></div><div class="table-scroll"><table class="table"><thead><tr><th>指标</th><th class="num">数量</th><th>说明</th></tr></thead><tbody><tr v-for="item in details" :key="item.label"><td class="cell-strong">{{ item.label }}</td><td class="num">{{ item.value }}</td><td>{{ item.hint }}</td></tr></tbody></table></div></section></template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'; import { BookOpen, ClipboardList, TriangleAlert, Users } from 'lucide-vue-next'
import AppMetric from '@/components/AppMetric.vue'; import AsyncState from '@/components/AsyncState.vue'; import { adminStatisticsApi } from '@/services/api'; import type { AdminStatisticsVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
const state = usePageState(); const stats = ref<AdminStatisticsVO | null>(null)
const details = computed(() => stats.value ? [{ label: '启用用户', value: stats.value.enabledUsers, hint: '当前可登录账号' }, { label: '学生', value: stats.value.students, hint: '学生角色用户' }, { label: '教师', value: stats.value.teachers, hint: '教师角色用户' }, { label: '已提交作业', value: stats.value.submittedAssignments, hint: '学生正式提交' }, { label: '已发布公告', value: stats.value.publishedAnnouncements, hint: '系统公告' }] : [])
async function load() { const data = await state.run(() => adminStatisticsApi.get()); if (data) stats.value = data }
onMounted(load)
</script>
