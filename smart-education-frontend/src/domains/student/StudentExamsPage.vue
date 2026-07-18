<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">考试安排</h1><p class="page-subtitle">查看已选课程的已发布考试，并在开放时间内开始答题。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel flush">
      <div class="panel-head"><h2>近期考试</h2><span class="count">共 {{ exams.length }} 场</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>考试名称</th><th>课程</th><th>开始时间</th><th>结束时间</th><th class="num">时长</th><th class="num">总分</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="exam in exams" :key="exam.examId" :class="{ 'notification-resource-target': exam.examId === selectedExamId }"><td class="cell-strong">{{ exam.title }}</td><td>{{ courseName(exam.courseId) }}</td><td>{{ formatTime(exam.startAt) }}</td><td>{{ formatTime(exam.endAt) }}</td><td class="num">{{ exam.durationMinutes }} 分钟</td><td class="num">{{ exam.totalScore }}</td><td><AppButton variant="primary" @click="enter(exam)">进入考试</AppButton></td></tr>
          <tr v-if="!exams.length"><td colspan="7" class="list-empty">暂无考试安排</td></tr>
        </tbody>
      </table></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import { examsApi, studentLearningApi } from '@/services/api'
import type { StudentCourseListItemVO, StudentExamListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const route = useRoute()
const courses = ref<StudentCourseListItemVO[]>([])
const exams = ref<StudentExamListItemVO[]>([])
const message = ref('')
const selectedExamId = computed(() => typeof route.query.examId === 'string' ? route.query.examId : '')
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
const formatTime = formatDateTime
async function load() {
  const data = await state.run(async () => {
    const coursePage = await studentLearningApi.myCourses({ page: 1, size: 100 })
    const pages = await Promise.all(coursePage.records.map((course) => examsApi.studentExams(course.courseId, { page: 1, size: 100 })))
    return { courses: coursePage.records, exams: pages.flatMap((page) => page.records) }
  })
  if (data) { courses.value = data.courses; exams.value = data.exams }
}
const router = useRouter()
function enter(exam: StudentExamListItemVO) {
  router.push({ path: `/student/exams/${exam.examId}/attempt`, query: { title: exam.title } })
}
onMounted(load)
</script>
