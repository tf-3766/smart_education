<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">你好，{{ session.currentUser.name }}</h1>
        <p class="page-subtitle">当前有 {{ pendingTasks.length }} 项学习任务待完成</p>
      </div>
      <AppButton variant="primary" @click="router.push('/student/courses')">继续学习</AppButton>
    </div>

    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <template v-if="!state.loading.value && !state.error.value">
      <div class="kpi-grid">
        <AppMetric label="在学课程" :value="overview.courses.length" hint="已选并进行中" :icon="BookOpen" />
        <AppMetric label="待交作业" :value="pendingTasks.length" hint="按截止时间排序" tone="warn" :icon="ClipboardList" />
        <AppMetric label="近期考试" :value="overview.exams.length" hint="已发布安排" :icon="FileQuestion" />
        <AppMetric label="平均进度" :value="`${avgProgress}%`" hint="课程学习记录" tone="success" :icon="TrendingUp" />
      </div>

      <div class="grid cols-2">
        <section class="panel">
          <div class="spread"><h2 class="panel-title">继续学习</h2><span class="muted">按进度</span></div>
          <div class="stack">
            <div v-for="course in overview.courses" :key="course.id" class="notice continue-course">
              <div class="continue-course-main">
                <strong>{{ course.title }}</strong>
                <div class="progress-inline push-top">
                  <span class="progress good"><i :style="{ width: course.progress + '%' }" /></span>
                  <span class="num muted">{{ course.progress }}%</span>
                </div>
              </div>
              <StatusBadge class="continue-course-status" tone="green" label="学习中" />
              <AppButton class="continue-course-enter" variant="secondary" @click="enterCourse(course.id, course.nextLessonId)">进入</AppButton>
            </div>
            <p v-if="!overview.courses.length" class="list-empty">暂无在学课程</p>
          </div>
        </section>

        <section class="panel">
          <div class="spread"><h2 class="panel-title">今日待办</h2><span class="muted">共 {{ pendingTasks.length }} 项</span></div>
          <div class="stack">
            <div v-for="task in pendingTasks" :key="task.id" class="notice">
              <div style="flex: 1"><strong>{{ task.title }}</strong><p>{{ courseName(task.courseId) }} · 截止 {{ formatTime(task.dueAt) }}</p></div>
              <AppButton variant="secondary" @click="router.push('/student/assignments')">处理</AppButton>
            </div>
            <p v-if="!pendingTasks.length" class="list-empty">暂无待办任务</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, ClipboardList, FileQuestion, TrendingUp } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AppMetric from '@/components/AppMetric.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { studentLearningApi } from '@/services/api'
import { loadStudentOverview, type StudentOverview } from '@/services/adapters/studentAdapter'
import { usePageState } from '@/services/pageState'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()
const state = usePageState()
const overview = reactive<StudentOverview>({ courses: [], assignments: [], exams: [], grades: [], topics: [] })
const pendingTasks = computed(() => overview.assignments.filter((item) => item.status === 'OPEN' && !['SUBMITTED', 'GRADED'].includes(item.submissionStatus)))
const avgProgress = computed(() => overview.courses.length ? Math.round(overview.courses.reduce((sum, item) => sum + item.progress, 0) / overview.courses.length) : 0)
const courseName = (id: string) => overview.courses.find((item) => item.id === id)?.title ?? id
const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })

async function load() {
  const data = await state.run(loadStudentOverview)
  if (data) Object.assign(overview, data)
}
async function enterCourse(courseId: string, lessonId: string | null) {
  if (lessonId) return router.push(`/student/lessons/${lessonId}`)
  const outline = await state.run(() => studentLearningApi.outline(courseId))
  const first = outline?.chapters.flatMap((chapter) => chapter.lessons).find((lesson) => lesson.unlocked)
  if (first) router.push(`/student/lessons/${first.lessonId}`)
}
onMounted(load)
</script>
