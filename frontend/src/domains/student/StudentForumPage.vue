<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">课程互动</h1><p class="page-subtitle">围绕已选课程发布主题并参与讨论。</p></div></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel">
      <div class="spread"><h2 class="panel-title">发布主题</h2><select v-model="newTopic.courseId" class="select"><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></div>
      <input v-model="newTopic.title" class="input push-top" placeholder="主题标题" />
      <textarea v-model="newTopic.content" class="textarea push-top" placeholder="描述问题或分享学习心得" />
      <div class="form-actions"><AppButton variant="primary" :disabled="!newTopic.courseId || !newTopic.title.trim() || !newTopic.content.trim()" @click="createTopic">发布主题</AppButton></div>
    </section>
    <div class="stack push-top">
      <article v-for="thread in topics" :key="thread.topicId" class="panel">
        <div class="spread wrap"><div class="row"><strong style="color: var(--ink); font-size: 15px">{{ thread.title }}</strong><StatusBadge v-if="thread.pinned" tone="blue" label="置顶" /></div><span class="muted">{{ courseName(thread.courseId) }} · {{ thread.replyCount }} 条回复</span></div>
        <div v-if="replies[thread.topicId]?.length" class="stack push-top"><div v-for="replyItem in replies[thread.topicId]" :key="replyItem.replyId" class="notice"><div><div class="spread"><strong>{{ replyItem.authorName || '用户' }}</strong><span class="muted">{{ formatTime(replyItem.createdAt) }}</span></div><p>{{ replyItem.content }}</p></div></div></div>
        <div class="row push-top"><input v-model="drafts[thread.topicId]" class="input" placeholder="写下你的回复…" @focus="loadReplies(thread.topicId)" @keyup.enter="reply(thread.topicId)" /><AppButton variant="primary" :disabled="!drafts[thread.topicId]?.trim()" @click="reply(thread.topicId)">发布</AppButton></div>
      </article>
      <p v-if="!topics.length" class="list-empty">暂无讨论主题</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { onMounted, reactive, ref } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { forumApi, studentLearningApi } from '@/services/api'
import type { ForumReplyVO, ForumTopicListItemVO, StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const courses = ref<StudentCourseListItemVO[]>([])
const topics = ref<ForumTopicListItemVO[]>([])
const drafts = reactive<Record<string, string>>({})
const replies = reactive<Record<string, ForumReplyVO[]>>({})
const newTopic = reactive({ courseId: '', title: '', content: '' })
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
const formatTime = formatDateTime
async function load() {
  const data = await state.run(async () => {
    const coursePage = await studentLearningApi.myCourses({ page: 1, size: 100 })
    const pages = await Promise.all(coursePage.records.map((course) => forumApi.studentTopics(course.courseId, { page: 1, size: 100 })))
    return { courses: coursePage.records, topics: pages.flatMap((page) => page.records) }
  })
  if (data) { courses.value = data.courses; topics.value = data.topics; newTopic.courseId ||= data.courses[0]?.courseId ?? '' }
}
async function loadReplies(topicId: string) {
  if (replies[topicId]) return
  const page = await state.run(() => forumApi.listReplies(topicId, { page: 1, size: 100 }))
  if (page) replies[topicId] = page.records
}
async function createTopic() {
  const created = await state.run(() => forumApi.createTopic(newTopic.courseId, { title: newTopic.title, content: newTopic.content }))
  if (created) { newTopic.title = ''; newTopic.content = ''; await load() }
}
async function reply(topicId: string) {
  const content = drafts[topicId]?.trim(); if (!content) return
  const created = await state.run(() => forumApi.createReply(topicId, { content }))
  if (created) { drafts[topicId] = ''; replies[topicId] = [...(replies[topicId] ?? []), created]; await load() }
}
onMounted(load)
</script>
