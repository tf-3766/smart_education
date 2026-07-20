<template>
  <div class="student-exams-page">
    <header class="exam-page-header">
      <h1>考试安排</h1>
      <p>在这里查看您已报名考试的时间与详细安排，并在开放时间内开始答题。</p>
    </header>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <LiquidGlass as="section" class="exams-board" :interactive="true" aria-labelledby="upcoming-exams-title">
      <div class="exams-board__head">
        <h2 id="upcoming-exams-title">近期考试</h2>
        <span>共 {{ exams.length }} 场</span>
      </div>

      <div class="exam-table-layer">
        <div class="table-scroll">
          <table class="exam-table">
            <colgroup>
              <col class="exam-col-title" />
              <col class="exam-col-course" />
              <col class="exam-col-start" />
              <col class="exam-col-end" />
              <col class="exam-col-duration" />
              <col class="exam-col-score" />
              <col class="exam-col-action" />
            </colgroup>
            <thead>
              <tr>
                <th>考试名称</th>
                <th>课程</th>
                <th>开始时间</th>
                <th>结束时间</th>
                <th class="num">时长</th>
                <th class="num">总分</th>
                <th class="exam-action-column">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="exam in exams"
                :key="exam.examId"
                :class="{ 'notification-resource-target': exam.examId === selectedExamId }"
              >
                <td class="exam-title-cell">{{ exam.title }}</td>
                <td>{{ courseName(exam.courseId) }}</td>
                <td class="exam-data-cell">{{ formatTime(exam.startAt) }}</td>
                <td class="exam-data-cell">{{ formatTime(exam.endAt) }}</td>
                <td class="num exam-data-cell">{{ exam.durationMinutes }} 分钟</td>
                <td class="num exam-data-cell">{{ exam.totalScore }}</td>
                <td class="exam-action-cell">
                  <AppButton variant="primary" @click="enter(exam)">进入考试</AppButton>
                </td>
              </tr>
              <tr v-if="!exams.length">
                <td colspan="7" class="exam-empty">暂无考试安排</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </LiquidGlass>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import LiquidGlass from '@/components/LiquidGlass.vue'
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

<style>
.app-shell.liquid-glass-shell:has(.student-exams-page) {
  --exam-page-edge: clamp(36px, 3.1vw, 52px);
  grid-template-rows: 127px minmax(0, auto);
}

.app-shell.liquid-glass-shell:has(.student-exams-page)::before {
  background: linear-gradient(
    90deg,
    rgba(24, 74, 125, .015) 12%,
    rgba(24, 74, 125, .035) 38%,
    rgba(28, 75, 120, .08) 66%,
    rgba(28, 75, 120, .06) 100%
  );
  backdrop-filter: brightness(.96) saturate(.98);
  -webkit-backdrop-filter: brightness(.96) saturate(.98);
}

.liquid-glass-shell:has(.student-exams-page) .topbar {
  padding-inline: var(--exam-page-edge);
  transform: translateY(5px);
}

.liquid-glass-shell:has(.student-exams-page) .brand {
  width: 190px;
  min-height: 58px;
  box-sizing: border-box;
  transform: translateY(3px);
  padding: 0 17px;
  border: 1px solid rgba(255, 255, 255, .72);
  border-radius: 16px;
  background: rgba(255, 255, 255, .27);
  box-shadow: 0 4px 8px rgba(43, 80, 119, .09), inset 0 1px 0 rgba(255, 255, 255, .76);
  backdrop-filter: blur(14px) saturate(135%);
  -webkit-backdrop-filter: blur(14px) saturate(135%);
}

.liquid-glass-shell:has(.student-exams-page) .app-body {
  grid-template-columns: 228px minmax(0, 1fr);
  gap: 17px;
  padding: 0 var(--exam-page-edge) 47px;
}

.liquid-glass-shell:has(.student-exams-page) .workspace-sidebar,
.liquid-glass-shell:has(.student-exams-page) .route-glass-surface {
  min-height: calc(100dvh - 175px);
  border-radius: 22px !important;
}

.liquid-glass-shell:has(.student-exams-page) .workspace-sidebar {
  padding: 18px 12px;
  --liquid-surface: rgba(237, 244, 248, .32);
  --liquid-warp-surface: rgba(255, 255, 255, .1);
}

.liquid-glass-shell:has(.student-exams-page) .nav-title {
  min-height: 42px;
  display: flex;
  align-items: center;
  margin: 0 14px 19px;
  color: #223d61;
  font-size: 16px;
  font-weight: 680;
  letter-spacing: 0;
}

.liquid-glass-shell:has(.student-exams-page) .workspace-nav {
  gap: 10px;
}

.liquid-glass-shell:has(.student-exams-page) .nav-link {
  min-height: 54px;
  padding-inline: 15px;
  font-size: 17px;
}

.liquid-glass-shell:has(.student-exams-page) .nav-link > svg {
  width: 22px;
  height: 22px;
}

.liquid-glass-shell:has(.student-exams-page) .route-glass-surface {
  --liquid-surface: linear-gradient(
    180deg,
    rgba(218, 232, 246, .28) 0%,
    rgba(218, 232, 246, .18) 52%,
    rgba(218, 232, 246, .11) 100%
  );
  --liquid-warp-surface: rgba(192, 214, 235, .045);
  --liquid-border: rgba(255, 255, 255, .78);
}

.liquid-glass-shell:has(.student-exams-page) .route-glass-surface > .liquid-glass__warp {
  backdrop-filter: blur(11px) saturate(var(--liquid-saturation));
  -webkit-backdrop-filter: blur(11px) saturate(var(--liquid-saturation));
}

.liquid-glass-shell:has(.student-exams-page) .route-glass-surface > .liquid-glass__content {
  padding: 28px 44px 48px;
}
</style>

<style scoped>
.student-exams-page {
  min-height: 100%;
  color: #17243b;
}

.exam-page-header {
  padding: 2px 2px 22px;
}

.exam-page-header h1 {
  margin: 0;
  color: #17243b;
  font-size: 28px;
  font-weight: 750;
  line-height: 1.25;
  letter-spacing: -.02em;
  text-wrap: balance;
  text-shadow: 0 1px 12px rgba(255, 255, 255, .78);
}

.exam-page-header p {
  max-width: 68ch;
  margin: 7px 0 0;
  color: #465870;
  font-size: 16px;
  line-height: 1.6;
  text-shadow: 0 1px 10px rgba(255, 255, 255, .72);
}

.exams-board {
  min-height: 62.8dvh;
  margin-left: -24px;
  margin-right: -17px;
  --liquid-surface: rgba(229, 239, 249, .24);
  --liquid-warp-surface: rgba(238, 246, 253, .08);
  --liquid-border: rgba(255, 255, 255, .82);
  border-radius: 18px;
}

.exams-board :deep(.liquid-glass__content) {
  min-height: inherit;
  display: flex;
  flex-direction: column;
  padding: 0 18px 12px 12px;
  box-sizing: border-box;
}

.exams-board__head {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 19px;
}

.exams-board__head h2 {
  margin: 0;
  color: #17243b;
  font-size: 21px;
  font-weight: 720;
  letter-spacing: -.015em;
}

.exams-board__head span {
  color: #52647d;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.exam-table-layer {
  overflow: hidden;
  border: 1px solid rgba(86, 139, 205, .48);
  border-radius: 14px;
  background: rgba(248, 251, 255, .5);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .84);
}

.exam-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  color: #465870;
  font-size: 15px;
}

.exam-col-title { width: 18%; }
.exam-col-course { width: 15%; }
.exam-col-start { width: 18%; }
.exam-col-end { width: 17%; }
.exam-col-duration { width: 9%; }
.exam-col-score { width: 9%; }
.exam-col-action { width: 14%; }

.exam-table th,
.exam-table td {
  padding: 16px 28px;
  text-align: left;
  vertical-align: middle;
  border-bottom: 1px solid rgba(86, 139, 205, .36);
}

.exam-table td {
  padding-block: 18px;
}

.exam-table th {
  color: #40526b;
  background: rgba(222, 235, 248, .56);
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.exam-table td {
  color: #465870;
  background: rgba(252, 254, 255, .34);
}

.exam-table tbody tr {
  transition: background-color 160ms cubic-bezier(.22, 1, .36, 1);
}

.exam-table tbody tr:hover td {
  background: rgba(226, 239, 255, .64);
}

.exam-table tbody tr:last-child td {
  border-bottom: 0;
}

.exam-table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.exam-title-cell {
  color: #17243b !important;
  font-weight: 700;
}

.exam-data-cell {
  color: #40506a !important;
  white-space: nowrap;
}

.exam-action-column,
.exam-action-cell {
  text-align: center !important;
}

.exam-action-cell {
  border-left: 1px solid rgba(121, 151, 185, .24);
}

.exam-action-cell :deep(.app-button) {
  min-width: 106px;
  min-height: 46px;
  padding-inline: 16px !important;
}

.notification-resource-target td {
  background: rgba(218, 235, 255, .72);
}

.exam-empty {
  padding: 42px 20px !important;
  color: #6b7b91 !important;
  text-align: center !important;
}

@media (prefers-reduced-motion: reduce) {
  .exam-table tbody tr { transition: none; }
}
</style>
