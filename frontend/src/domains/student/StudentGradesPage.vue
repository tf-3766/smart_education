<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">成绩进度</h1><p class="page-subtitle">仅展示教师已经发布的作业成绩。</p></div></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="kpi-grid">
      <AppMetric label="平均得分率" :value="average" hint="已发布成绩" :icon="BarChart3" />
      <AppMetric label="已通过" :value="passed" hint="得分率不低于 60%" tone="success" :icon="CircleCheck" />
      <AppMetric label="待改进" :value="failed" hint="得分率低于 60%" tone="danger" :icon="TriangleAlert" />
    </div>
    <section class="panel flush">
      <div class="panel-head"><h2>成绩明细</h2><span class="count">共 {{ grades.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>作业</th><th>课程</th><th class="num">得分</th><th class="num">满分</th><th class="num">得分率</th><th>结果</th><th>教师评语</th></tr></thead>
        <tbody>
          <tr v-for="grade in grades" :key="grade.gradeId"><td class="cell-strong">{{ grade.assignmentTitle }}</td><td>{{ courseName(grade.courseId) }}</td><td class="num">{{ grade.score }}</td><td class="num">{{ grade.maxScore }}</td><td class="num">{{ Math.round(grade.scoreRate * 100) }}%</td><td><StatusBadge :tone="grade.scoreRate >= 0.6 ? 'green' : 'red'" :label="grade.scoreRate >= 0.6 ? '通过' : '需改进'" /></td><td>{{ grade.teacherComment || '—' }}</td></tr>
          <tr v-if="!grades.length"><td colspan="7" class="list-empty">暂无已发布成绩</td></tr>
        </tbody>
      </table></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BarChart3, CircleCheck, TriangleAlert } from 'lucide-vue-next'
import AppMetric from '@/components/AppMetric.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { assignmentsApi, studentLearningApi } from '@/services/api'
import type { StudentCourseListItemVO, StudentGradeVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const grades = ref<StudentGradeVO[]>([])
const courses = ref<StudentCourseListItemVO[]>([])
const passed = computed(() => grades.value.filter((item) => item.scoreRate >= 0.6).length)
const failed = computed(() => grades.value.length - passed.value)
const average = computed(() => grades.value.length ? `${Math.round(grades.value.reduce((sum, item) => sum + item.scoreRate, 0) / grades.value.length * 100)}%` : '—')
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
async function load() {
  const data = await state.run(() => Promise.all([assignmentsApi.studentGrades({ page: 1, size: 100 }), studentLearningApi.myCourses({ page: 1, size: 100 })]))
  if (data) { grades.value = data[0].records; courses.value = data[1].records }
}
onMounted(load)
</script>
