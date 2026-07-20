<template>
  <div>
    <div class="page-header">
      <div><h1 class="page-title">选课中心</h1><p class="page-subtitle">浏览已发布课程并加入；加入后可在「我的课程」进入学习。</p></div>
      <AppButton variant="secondary" @click="router.push('/student/courses')">我的课程</AppButton>
    </div>
    <div v-if="message" class="toast">{{ message }}</div>
    <div class="filter-bar">
      <label class="filter-field grow"><span>搜索课程</span><span class="field-with-icon"><Search :size="16" /><input v-model.trim="keyword" class="input" placeholder="课程名称或编码" /></span></label>
    </div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :empty="!filtered.length" empty-text="暂无可选课程" :retry="load" />
    <section v-if="filtered.length" class="panel flush">
      <div class="panel-head"><h2>可选课程</h2><span class="count">共 {{ filtered.length }} 门</span></div>
      <div class="table-scroll">
        <table class="table">
          <thead><tr><th>课程</th><th>教师</th><th>学期</th><th>选课状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="course in filtered" :key="course.courseId">
              <td><strong class="cell-strong">{{ course.name }}</strong><div class="muted">{{ course.courseCode }} · {{ course.summary || '暂无课程简介' }}</div></td>
              <td>{{ course.ownerTeacherName }}</td><td>{{ course.term || '—' }}</td>
              <td><StatusBadge :tone="enrolled(course) ? 'green' : 'gray'" :label="course.enrollmentStatus?.label || '未选课'" /></td>
              <td>
                <AppButton v-if="enrolled(course)" variant="secondary" @click="router.push('/student/courses')">去学习</AppButton>
                <AppButton v-else variant="primary" :disabled="busy === course.courseId || !course.enrollable" @click="join(course)">{{ course.enrollable ? '加入学习' : '不可选课' }}</AppButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { studentLearningApi } from '@/services/api'
import type { StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const router = useRouter()
const state = usePageState()
const courses = ref<StudentCourseListItemVO[]>([])
const busy = ref('')
const keyword = ref('')
const message = ref('')
const enrolled = (course: StudentCourseListItemVO) => course.enrollmentStatus?.code === 'ENROLLED'
const filtered = computed(() => courses.value.filter((course) => !keyword.value
  || `${course.name}${course.courseCode}`.toLowerCase().includes(keyword.value.toLowerCase())))

function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() {
  const page = await state.run(() => studentLearningApi.catalog({ page: 1, size: 100 }))
  if (page) courses.value = page.records ?? []
}
async function join(course: StudentCourseListItemVO) {
  busy.value = course.courseId
  try {
    const result = await state.run(() => studentLearningApi.enroll(course.courseId))
    if (result) { flash(`已加入《${course.name}》，可在「我的课程」进入学习`); await load() }
  } finally { busy.value = '' }
}
onMounted(load)
</script>
