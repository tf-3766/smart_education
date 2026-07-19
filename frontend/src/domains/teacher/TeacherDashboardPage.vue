<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">教学工作台</h1><p class="page-subtitle">{{ session.currentUser.name }} · 教学运行汇总</p></div><span class="muted">最近同步：刚刚</span></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="kpi-grid">
      <AppMetric label="负责课程" :value="overview.courses.length" :hint="`${publishedCount} 门已发布，${reviewCount} 门待审核`" :icon="BookOpen" />
      <AppMetric label="已发布作业" :value="publishedAssignments" hint="真实课程作业" tone="warn" :icon="ClipboardList" />
      <AppMetric label="待处理预警" :value="openWarnings" hint="需教师干预" tone="danger" :icon="TriangleAlert" />
      <AppMetric label="课程互动" :value="overview.topics.length" hint="讨论主题" :icon="MessageSquare" />
    </div>
    <div class="toolbar">
      <AppButton variant="primary" @click="router.push('/teacher/courses')"><span class="row"><Plus :size="16" />新建教学任务</span></AppButton>
      <AppButton variant="secondary" @click="router.push('/teacher/assignments')">进入批改</AppButton>
    </div>
    <section class="panel flush">
      <div class="panel-head"><h2>全部教学事项</h2><span class="count">共 {{ tasks.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>教学事项</th><th>课程</th><th>时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id"><td class="cell-strong">{{ task.title }}</td><td>{{ task.course }}</td><td>{{ task.time }}</td><td><StatusBadge :tone="task.tone" :label="task.status" /></td><td><button class="text-link" @click="router.push(task.to)">{{ task.action }}</button></td></tr>
          <tr v-if="!tasks.length"><td colspan="5" class="list-empty">暂无教学事项</td></tr>
        </tbody>
      </table></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, ClipboardList, MessageSquare, Plus, TriangleAlert } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AppMetric from '@/components/AppMetric.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { loadTeacherOverview, type TeacherOverview } from '@/services/adapters/teacherAdapter'
import { usePageState } from '@/services/pageState'
import { useSessionStore } from '@/stores/session'

const router = useRouter(); const session = useSessionStore(); const state = usePageState()
const overview = reactive<TeacherOverview>({ courses: [], assignments: [], warnings: [], topics: [] })
const publishedCount = computed(() => overview.courses.filter((item) => item.status === 'PUBLISHED').length)
const reviewCount = computed(() => overview.courses.filter((item) => item.review === 'PENDING').length)
const publishedAssignments = computed(() => overview.assignments.filter((item) => item.assignmentStatus.code === 'PUBLISHED').length)
const openWarnings = computed(() => overview.warnings.filter((item) => item.warningStatus.code === 'OPEN').length)
const courseName = (id: string) => overview.courses.find((item) => item.id === id)?.title ?? id
const tasks = computed(() => [
  ...overview.assignments.map((item) => ({ id: `a-${item.assignmentId}`, title: item.title, course: courseName(item.courseId), time: formatDateTime(item.dueAt), status: item.availabilityStatus.label, tone: 'blue' as const, to: '/teacher/assignments', action: '进入批改' })),
  ...overview.warnings.filter((item) => item.warningStatus.code === 'OPEN').map((item) => ({ id: `w-${item.warningId}`, title: item.summary, course: courseName(item.courseId), time: formatDateTime(item.generatedAt), status: item.warningLevel.code === 'HIGH' ? '高风险' : '需关注', tone: 'red' as const, to: '/teacher/warnings', action: '记录干预' })),
])
async function load() { const data = await state.run(loadTeacherOverview); if (data) Object.assign(overview, data) }
onMounted(load)
</script>
