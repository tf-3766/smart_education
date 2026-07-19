<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">课程讨论</h1>
        <p class="page-subtitle">讨论区属于具体课程；主题、发言和回复都保留在该课程的学习空间中。</p>
      </div>
    </div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="course-switcher">
      <span>当前课程</span>
      <select v-model="courseId" class="select" @change="loadTopics">
        <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
          {{ course.name }} · {{ course.ownerTeacherName }}老师
        </option>
      </select>
    </div>
    <section class="panel topic-composer">
      <p class="eyebrow">发布到课程</p>
      <h2>{{ courseName(courseId) }}</h2>
      <input v-model="newTopic.title" class="input push-top" placeholder="主题标题" />
      <textarea v-model="newTopic.content" class="textarea push-top" placeholder="描述问题、分享学习心得或发起讨论" />
      <div class="form-actions">
        <AppButton variant="primary" :disabled="!courseId || !newTopic.title.trim() || !newTopic.content.trim()" @click="createTopic">发布主题</AppButton>
      </div>
    </section>
    <div class="stack push-top">
      <article v-for="thread in topics" :key="thread.topicId" class="panel discussion-card">
        <header class="discussion-head">
          <div class="discussion-author-row">
            <UserAvatar :file-id="thread.authorAvatarFileId" :name="thread.authorName || '课程成员'" :size="42" />
            <div>
              <div class="row"><StatusBadge v-if="thread.pinned" tone="blue" label="置顶" /><strong>{{ thread.title }}</strong></div>
              <p class="muted">{{ thread.authorName || '课程成员' }} · {{ formatTime(thread.createdAt) }}</p>
            </div>
          </div>
          <span class="muted">{{ thread.replyCount }} 条回复</span>
        </header>
        <p class="topic-content pre-line">{{ details[thread.topicId]?.content }}</p>
        <div class="reply-stream">
          <div v-for="replyItem in replies[thread.topicId]" :key="replyItem.replyId" class="reply-item" :class="{ nested: replyItem.parentReplyId }">
            <div class="reply-author"><UserAvatar :file-id="replyItem.authorAvatarFileId" :name="replyItem.authorName || '课程成员'" :size="30" /><div class="reply-meta"><strong>{{ replyItem.authorName || '课程成员' }}</strong><span>{{ formatTime(replyItem.createdAt) }}</span></div></div>
            <p>{{ replyItem.content }}</p>
            <button class="text-link" @click="setReplyTarget(thread.topicId, replyItem)">回复此发言</button>
          </div>
          <p v-if="!replies[thread.topicId]?.length" class="muted">还没有回复，欢迎发表第一条看法。</p>
        </div>
        <p v-if="replyTo[thread.topicId]" class="replying-hint">
          正在回复 {{ replyName(thread.topicId) }}
          <button class="text-link" @click="replyTo[thread.topicId] = null">取消</button>
        </p>
        <div class="row reply-box">
          <input v-model="drafts[thread.topicId]" class="input" :placeholder="replyTo[thread.topicId] ? '写下针对该发言的回复…' : '参与讨论…'" @keyup.enter="reply(thread.topicId)" />
          <AppButton variant="primary" :disabled="!drafts[thread.topicId]?.trim()" @click="reply(thread.topicId)">回复</AppButton>
        </div>
      </article>
      <p v-if="!topics.length" class="list-empty">该课程暂无讨论主题</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { forumApi, studentLearningApi } from '@/services/api'
import type { ForumReplyVO, ForumTopicDetailVO, ForumTopicListItemVO, StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { formatDateTime } from '@/utils/datetime'

const state = usePageState()
const courses = ref<StudentCourseListItemVO[]>([])
const courseId = ref('')
const topics = ref<ForumTopicListItemVO[]>([])
const details = reactive<Record<string, ForumTopicDetailVO>>({})
const drafts = reactive<Record<string, string>>({})
const replies = reactive<Record<string, ForumReplyVO[]>>({})
const replyTo = reactive<Record<string, string | null>>({})
const newTopic = reactive({ title: '', content: '' })
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? '请选择课程'
const formatTime = formatDateTime

async function load() {
  const page = await state.run(() => studentLearningApi.myCourses({ page: 1, size: 100 }))
  if (!page) return
  courses.value = page.records
  courseId.value ||= page.records[0]?.courseId ?? ''
  await loadTopics()
}
async function loadTopics() {
  if (!courseId.value) return
  const page = await state.run(() => forumApi.studentTopics(courseId.value, { page: 1, size: 100 }))
  if (!page) return
  topics.value = page.records
  await Promise.all(page.records.map(async (topic) => {
    const [detail, replyPage] = await Promise.all([
      forumApi.topicDetail(topic.topicId),
      forumApi.listReplies(topic.topicId, { page: 1, size: 100 }),
    ])
    details[topic.topicId] = detail
    replies[topic.topicId] = replyPage.records
  }))
}
async function createTopic() {
  const created = await state.run(() => forumApi.createTopic(courseId.value, { title: newTopic.title.trim(), content: newTopic.content.trim() }))
  if (!created) return
  newTopic.title = ''
  newTopic.content = ''
  await loadTopics()
}
function setReplyTarget(topicId: string, reply: ForumReplyVO) { replyTo[topicId] = reply.replyId }
function replyName(topicId: string) {
  const id = replyTo[topicId]
  return replies[topicId]?.find((item) => item.replyId === id)?.authorName || '该发言'
}
async function reply(topicId: string) {
  const content = drafts[topicId]?.trim()
  if (!content) return
  const created = await state.run(() => forumApi.createReply(topicId, { content, parentReplyId: replyTo[topicId] || null }))
  if (!created) return
  drafts[topicId] = ''
  replyTo[topicId] = null
  replies[topicId] = [...(replies[topicId] ?? []), created]
  await loadTopics()
}
onMounted(load)
</script>

<style scoped>
.course-switcher { padding: 14px 16px; margin-bottom: 14px; display: flex; align-items: center; gap: 14px; background: #f5f7fb; border: 1px solid var(--line); }
.course-switcher > span { font-size: 13px; font-weight: 700; }
.course-switcher .select { max-width: 460px; }
.topic-composer h2 { margin: 5px 0 0; font-size: 18px; }
.eyebrow { margin: 0; color: var(--primary); font-size: 12px; font-weight: 700; }
.discussion-card { padding: 0; overflow: hidden; }
.discussion-head { padding: 18px 20px; display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid var(--line); }
.discussion-head strong { font-size: 16px; }
.discussion-author-row, .reply-author { display: flex; align-items: center; gap: 11px; }
.reply-author .reply-meta { flex: 1; }
.discussion-head p { margin: 6px 0 0; font-size: 12px; }
.topic-content { padding: 18px 20px; margin: 0; font-size: 14px; line-height: 1.7; }
.reply-stream { margin: 0 20px; padding: 4px 0 4px 18px; border-left: 2px solid var(--primary-soft); display: grid; gap: 10px; }
.reply-item { padding: 12px 14px; background: #f7f9fc; }
.reply-item.nested { margin-left: 24px; }
.reply-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; }
.reply-meta strong { color: var(--ink); }
.reply-item p { margin: 7px 0; }
.replying-hint { margin: 12px 20px 0; color: var(--primary); font-size: 12px; }
.reply-box { padding: 14px 20px 18px; }
@media (max-width: 720px) {
  .course-switcher, .discussion-head, .reply-box { align-items: stretch; flex-direction: column; }
  .course-switcher .select { max-width: none; }
  .reply-item.nested { margin-left: 12px; }
}
</style>