<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">课程互动</h1><p class="page-subtitle">管理负责课程的讨论区与课程公告，可隐藏或恢复主题和回复。</p></div><AppButton variant="primary" :disabled="!courseId" @click="openAnnounce"><span class="row"><Plus :size="16" />发布课程公告</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="filter-bar"><label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="loadCourseData"><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label></div>
    <section class="panel flush"><div class="panel-head"><h2>讨论主题</h2><span class="count">共 {{ topics.length }} 项</span></div><div class="table-scroll"><table class="table"><thead><tr><th>主题</th><th>发起人</th><th class="num">回复数</th><th>可见状态</th><th>最后回复</th><th>操作</th></tr></thead><tbody><tr v-for="topic in topics" :key="topic.topicId" data-test="topic-row"><td class="cell-strong">{{ topic.title }}</td><td>{{ topic.authorName || topic.authorId }}</td><td class="num">{{ topic.replyCount }}</td><td><StatusBadge :tone="topic.status.code === 'VISIBLE' ? 'green' : 'gray'" :label="topic.status.label" /></td><td>{{ topic.lastRepliedAt ? formatTime(topic.lastRepliedAt) : '—' }}</td><td class="cell-actions"><button class="text-link" @click="openReplies(topic)">回复管理</button><button class="text-link" @click="toggle(topic)">{{ topic.status.code === 'VISIBLE' ? '隐藏' : '恢复' }}</button></td></tr><tr v-if="!topics.length"><td colspan="6" class="list-empty">暂无讨论主题</td></tr></tbody></table></div></section>
    <section class="panel flush">
      <div class="panel-head"><h2>课程公告</h2><span class="count">共 {{ announcements.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>公告</th><th>受众</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in announcements" :key="item.announcementId" data-test="announcement-row">
            <td><span class="cell-strong">{{ item.title }}</span><span class="cell-sub">{{ item.content }}</span></td>
            <td>{{ audienceLabel(item.audience) }}</td>
            <td><StatusBadge :tone="item.status === 'PUBLISHED' ? 'green' : 'gray'" :label="item.status === 'PUBLISHED' ? '已发布' : '已撤回'" /></td>
            <td>{{ formatTime(item.publishedAt) }}</td>
            <td><button v-if="item.status === 'PUBLISHED'" class="text-link" @click="withdrawAnnouncement(item)">撤回</button></td>
          </tr>
          <tr v-if="!announcements.length"><td colspan="5" class="list-empty">暂无课程公告，点击右上角发布。</td></tr>
        </tbody>
      </table></div>
    </section>
    <AppModal :open="announceForm.open" title="发布课程公告" description="公告发布后，选课学生会在公告与通知中看到。" @close="announceForm.open = false">
      <label class="field-label" for="tf-ann-title">公告标题</label><input id="tf-ann-title" v-model="announceForm.title" class="input" placeholder="公告标题" />
      <label class="field-label push-top" for="tf-ann-audience">受众范围</label><select id="tf-ann-audience" v-model="announceForm.audience" class="select"><option value="ALL">课程全体成员</option><option value="STUDENT">仅选课学生</option></select>
      <label class="field-label push-top" for="tf-ann-content">公告内容</label><textarea id="tf-ann-content" v-model="announceForm.content" class="textarea" rows="5" placeholder="公告正文" />
      <div class="form-actions"><AppButton variant="secondary" @click="announceForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!announceForm.title.trim() || !announceForm.content.trim()" @click="publishAnnouncement">发布</AppButton></div>
    </AppModal>
    <AppModal :open="Boolean(currentTopic)" title="回复管理" :description="currentTopic ? `${currentTopic.title} · ${currentTopic.authorName || currentTopic.authorId}` : ''" @close="closeReplies">
      <section v-if="topicDetail" class="notice"><div><strong>主题内容</strong><p class="pre-line">{{ topicDetail.content }}</p></div></section>
      <ul class="reply-list">
        <li v-for="reply in replies" :key="reply.replyId" class="reply-item" data-test="reply-item">
          <div class="spread"><strong>{{ reply.authorName || reply.authorId }}</strong><span class="muted">{{ formatTime(reply.createdAt) }}</span></div>
          <p class="pre-line" :class="{ muted: reply.status.code !== 'VISIBLE' }">{{ reply.content }}</p>
          <div class="spread"><StatusBadge :tone="reply.status.code === 'VISIBLE' ? 'green' : 'gray'" :label="reply.status.label" /><button class="text-link" @click="toggleReply(reply)">{{ reply.status.code === 'VISIBLE' ? '隐藏' : '恢复' }}</button></div>
        </li>
        <li v-if="!replies.length" class="list-empty">暂无回复</li>
      </ul>
      <label class="field-label push-top" for="tf-reply-content">教师回复</label>
      <textarea id="tf-reply-content" v-model="replyContent" class="textarea" rows="3" placeholder="以教师身份回复该主题" />
      <div class="form-actions"><AppButton variant="secondary" @click="closeReplies">关闭</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!replyContent.trim()" @click="sendReply">发表回复</AppButton></div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { onMounted, reactive, ref } from 'vue'
import { Plus } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { announcementsApi, forumApi, teacherCoursesApi } from '@/services/api'
import type { AnnouncementAudience, AnnouncementVO, ForumReplyVO, ForumTopicDetailVO, ForumTopicListItemVO, TeacherCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const courseId = ref(''); const topics = ref<ForumTopicListItemVO[]>([]); const announcements = ref<AnnouncementVO[]>([])
const message = ref('')
const formatTime = formatDateTime
const audienceLabel = (value: AnnouncementAudience) => value === 'ALL' ? '课程全体成员' : value === 'STUDENT' ? '仅选课学生' : '全体教师'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => teacherCoursesApi.list({ page: 1, size: 100 })); if (page) { courses.value = page.records; courseId.value ||= page.records[0]?.courseId ?? ''; await loadCourseData() } }
async function loadCourseData() { await Promise.all([loadTopics(), loadAnnouncements()]) }
async function loadTopics() { if (!courseId.value) return; const page = await state.run(() => forumApi.teacherTopics(courseId.value, { page: 1, size: 100 })); if (page) topics.value = page.records }
async function loadAnnouncements() { if (!courseId.value) return; const page = await state.run(() => announcementsApi.teacherCourseList(courseId.value, { page: 1, size: 100 })); if (page) announcements.value = page.records }
async function toggle(topic: ForumTopicListItemVO) { const hide = topic.status.code === 'VISIBLE'; const updated = await state.run(() => forumApi.teacherTopicVisibility(topic.topicId, { visible: !hide, reason: hide ? '教师内容管理' : null, version: topic.version })); if (updated) await loadTopics() }

// —— 课程公告 ——
const announceForm = reactive<{ open: boolean; title: string; content: string; audience: AnnouncementAudience }>({ open: false, title: '', content: '', audience: 'ALL' })
function openAnnounce() { Object.assign(announceForm, { open: true, title: '', content: '', audience: 'ALL' }) }
async function publishAnnouncement() {
  const created = await state.run(() => announcementsApi.createCourseAnnouncement(courseId.value, { title: announceForm.title.trim(), content: announceForm.content.trim(), audience: announceForm.audience }))
  if (created) { announceForm.open = false; flash('课程公告已发布'); await loadAnnouncements() }
}
async function withdrawAnnouncement(item: AnnouncementVO) {
  if (!window.confirm(`确认撤回公告《${item.title}》？撤回后学生不再看到该公告。`)) return
  const updated = await state.run(() => announcementsApi.teacherWithdraw(item.announcementId, { version: item.version }))
  if (updated) { flash('公告已撤回'); await loadAnnouncements() }
}

// —— 回复管理 ——
const currentTopic = ref<ForumTopicListItemVO | null>(null); const topicDetail = ref<ForumTopicDetailVO | null>(null); const replies = ref<ForumReplyVO[]>([]); const replyContent = ref('')
async function openReplies(topic: ForumTopicListItemVO) {
  currentTopic.value = topic; topicDetail.value = null; replies.value = []; replyContent.value = ''
  const [detail, page] = await Promise.all([
    state.run(() => forumApi.teacherTopicDetail(topic.topicId)),
    state.run(() => forumApi.teacherListReplies(topic.topicId, { page: 1, size: 100 })),
  ])
  if (detail) topicDetail.value = detail
  if (page) replies.value = page.records
}
function closeReplies() { currentTopic.value = null; topicDetail.value = null; replies.value = []; replyContent.value = '' }
async function reloadReplies() { const topic = currentTopic.value; if (!topic) return; const page = await state.run(() => forumApi.teacherListReplies(topic.topicId, { page: 1, size: 100 })); if (page) replies.value = page.records }
async function toggleReply(reply: ForumReplyVO) { const hide = reply.status.code === 'VISIBLE'; const updated = await state.run(() => forumApi.teacherReplyVisibility(reply.replyId, { visible: !hide, reason: hide ? '教师内容管理' : null, version: reply.version })); if (updated) await reloadReplies() }
async function sendReply() {
  const topic = currentTopic.value
  if (!topic) return
  const created = await state.run(() => forumApi.teacherCreateReply(topic.topicId, { content: replyContent.value.trim() }))
  if (created) { replyContent.value = ''; flash('回复已发表'); await Promise.all([reloadReplies(), loadTopics()]) }
}
onMounted(load)
</script>

<style scoped>
.reply-list { list-style: none; margin: 12px 0 0; padding: 0; display: grid; gap: 10px; }
.reply-item { border: 1px solid var(--border, #e5e5e5); padding: 10px 12px; display: grid; gap: 6px; }
</style>
