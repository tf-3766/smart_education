<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">学习任务</h1><p class="page-subtitle">保存草稿或确认提交作业，教师发布的反馈会在此展示。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :empty="!items.length" empty-text="暂无学习任务" :retry="load" />
    <div class="grid cols-2">
      <section v-for="item in items" :key="item.assignment.assignmentId" class="panel" :class="{ 'notification-resource-target': item.assignment.assignmentId === selectedAssignmentId }">
        <div class="spread wrap"><h2 class="panel-title" style="margin: 0">{{ item.assignment.title }}</h2><StatusBadge :tone="tone(item)" :label="label(item)" /></div>
        <p class="muted push-top">{{ courseName(item.assignment.courseId) }} · 截止 {{ formatTime(item.assignment.dueAt) }} · 满分 {{ item.assignment.maxScore }}</p>
        <div v-if="item.submission?.score != null" class="notice push-top"><div><div class="spread"><strong>教师反馈</strong><StatusBadge tone="green" :label="`${item.submission.score} 分`" /></div><p>{{ item.submission.teacherComment || '教师暂未填写评语' }}</p></div></div>
        <template v-else>
          <textarea v-model="drafts[item.assignment.assignmentId]" class="textarea push-top" placeholder="填写作业内容、附件链接或代码仓库地址" :disabled="locked(item)" />
          <div class="form-actions"><AppButton variant="secondary" :disabled="locked(item)" @click="save(item)">保存草稿</AppButton><AppButton variant="primary" :disabled="locked(item) || item.assignment.availabilityStatus.code !== 'OPEN'" @click="submit(item)">确认提交</AppButton></div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { assignmentsApi, studentLearningApi } from '@/services/api'
import type { StudentAssignmentDetailVO, StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const route = useRoute()
const items = ref<StudentAssignmentDetailVO[]>([])
const courses = ref<StudentCourseListItemVO[]>([])
const drafts = reactive<Record<string, string>>({})
const message = ref('')
const selectedAssignmentId = computed(() => typeof route.query.assignmentId === 'string' ? route.query.assignmentId : '')
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
const formatTime = formatDateTime
const locked = (item: StudentAssignmentDetailVO) => ['SUBMITTED', 'GRADED'].includes(item.submission?.submissionStatus.code ?? '')
const label = (item: StudentAssignmentDetailVO) => item.submission?.submissionStatus.label ?? item.assignment.availabilityStatus.label
const tone = (item: StudentAssignmentDetailVO): 'green' | 'blue' | 'gray' => item.submission?.score != null ? 'green' : locked(item) ? 'blue' : 'gray'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

async function load() {
  const result = await state.run(async () => {
    const coursePage = await studentLearningApi.myCourses({ page: 1, size: 100 })
    const listPages = await Promise.all(coursePage.records.map((course) => assignmentsApi.studentList(course.courseId, { page: 1, size: 100 })))
    const details = await Promise.all(listPages.flatMap((page) => page.records).map((item) => assignmentsApi.studentDetail(item.assignmentId)))
    return { courses: coursePage.records, details }
  })
  if (result) {
    courses.value = result.courses; items.value = result.details
    items.value.forEach((item) => { drafts[item.assignment.assignmentId] = item.submission?.content ?? '' })
  }
}
async function save(item: StudentAssignmentDetailVO) {
  const id = item.assignment.assignmentId
  const saved = await state.run(() => assignmentsApi.saveDraft(id, { content: drafts[id] ?? '', version: item.submission?.version ?? null }))
  if (saved) { flash('草稿已保存'); await load() }
}
async function submit(item: StudentAssignmentDetailVO) {
  const id = item.assignment.assignmentId
  const saved = await state.run(() => assignmentsApi.submit(id, { content: drafts[id] ?? '', version: item.submission?.version ?? null }))
  if (saved) { flash('作业已提交，等待教师批改'); await load() }
}
onMounted(load)
</script>
