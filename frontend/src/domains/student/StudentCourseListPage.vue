<template>
  <div>
    <div class="page-header">
      <div><h1 class="page-title">我的课程</h1><p class="page-subtitle">进入已选修课程学习；如需加入新课程请前往选课中心。</p></div>
      <AppButton variant="secondary" @click="router.push('/student/enroll')">去选课中心</AppButton>
    </div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :empty="!courses.length" empty-text="还没有选修的课程，去选课中心加入吧" :retry="load" />
    <section v-if="courses.length" class="panel flush">
      <div class="panel-head"><h2>在学课程</h2><span class="count">共 {{ courses.length }} 门</span></div>
      <div class="table-scroll">
        <table class="table">
          <thead><tr><th>课程</th><th>教师</th><th>学期</th><th>课程状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="course in courses" :key="course.courseId">
              <td><strong class="cell-strong">{{ course.name }}</strong><div class="muted">{{ course.courseCode }} · {{ course.summary || '暂无课程简介' }}</div></td>
              <td>{{ course.ownerTeacherName }}</td><td>{{ course.term || '—' }}</td>
              <td><StatusBadge tone="blue" :label="course.status.label" /></td>
              <td class="cell-actions">
                <AppButton v-if="course.status.code === 'OFFLINE'" variant="secondary" disabled>已下线</AppButton>
                <AppButton v-else variant="primary" :disabled="busy === course.courseId" @click="enter(course)">进入学习</AppButton>
                <button class="text-link" style="margin-left: 10px" :disabled="busy === course.courseId" @click="quit(course)">退课</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { studentLearningApi } from '@/services/api'
import type { StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { confirmDialog } from '@/services/confirmDialog'

const router = useRouter()
const state = usePageState()
const courses = ref<StudentCourseListItemVO[]>([])
const busy = ref('')

async function load() {
  const page = await state.run(() => studentLearningApi.myCourses({ page: 1, size: 100 }))
  if (page) courses.value = page.records ?? []
}
async function quit(course: StudentCourseListItemVO) {
  if (!(await confirmDialog(`确认退出《${course.name}》？退课后学习记录保留，但需重新选课才能继续学习。`, { title: '退出课程', confirmLabel: '确认退出' }))) return
  busy.value = course.courseId
  try {
    const result = await state.run(() => studentLearningApi.withdraw(course.courseId))
    if (result) await load()
  } finally { busy.value = '' }
}
async function enter(course: StudentCourseListItemVO) {
  busy.value = course.courseId
  try {
    const outline = await state.run(() => studentLearningApi.outline(course.courseId))
    const first = outline?.chapters.flatMap((chapter) => chapter.lessons).find((lesson) => lesson.unlocked)
    if (first) await router.push(`/student/lessons/${first.lessonId}`)
  } finally { busy.value = '' }
}
onMounted(load)
</script>
