<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">课程互动</h1><p class="page-subtitle">管理负责课程的讨论区与课程公告，可隐藏或恢复主题和回复。</p></div><AppButton variant="primary" :disabled="!courseId" @click="openAnnounce"><span class="row"><Plus :size="16" />发布课程公告</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="filter-bar"><label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="loadCourseData"><option v-if="!courses.length" value="" disabled>暂无可管理课程</option><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label></div>
    <div v-if="focusedAnnouncement" class="ai-draft-banner" role="status">
      <Sparkles :size="19" /><div><strong>已定位 AI 公告草稿《{{ focusedAnnouncement.title }}》</strong><p>请检查正文和受众范围，确认后才会通知学生。</p></div>
      <AppButton v-if="focusedAnnouncement.status === 'DRAFT'" variant="primary" @click="confirmAnnouncement(focusedAnnouncement)">确认发布</AppButton>
    </div>
    <section class="discussion-board">
      <div class="discussion-board-head"><div><h2>班级讨论</h2><p>按发布时间展示课程成员发言，教师可回复、置顶或隐藏不当内容。</p></div><span class="count">共 {{ topics.length }} 个主题</span></div>
      <article v-for="topic in topics" :key="topic.topicId" class="discussion-post" data-test="topic-row">
        <UserAvatar :file-id="topic.authorAvatarFileId" :name="topic.authorName || '课程成员'" :size="48" />
        <div class="post-body">
          <header class="post-author"><div><strong>{{ topic.authorName || '课程成员' }}</strong><span>{{ formatTime(topic.createdAt) }}</span></div><StatusBadge :tone="topic.status.code === 'VISIBLE' ? 'green' : 'gray'" :label="topic.status.label" /></header>
          <div class="row wrap post-title"><span v-if="topic.pinned" class="pin-mark">置顶</span><h3>{{ topic.title }}</h3></div>
          <p class="post-content pre-line">{{ topicDetails[topic.topicId]?.content || '正在加载主题内容…' }}</p>
          <footer class="post-actions">
            <button class="post-action" @click="openReplies(topic)"><MessageCircle :size="18" /><span>{{ topic.replyCount }} 条回复</span></button>
            <button class="post-action" @click="pinTopic(topic)"><Pin :size="18" /><span>{{ topic.pinned ? '取消置顶' : '置顶' }}</span></button>
            <button class="post-action" @click="toggle(topic)"><component :is="topic.status.code === 'VISIBLE' ? EyeOff : Eye" :size="18" /><span>{{ topic.status.code === 'VISIBLE' ? '隐藏' : '恢复' }}</span></button>
          </footer>
        </div>
      </article>
      <p v-if="!topics.length" class="list-empty">该课程暂无讨论主题。</p>
    </section>    <section class="panel flush">
      <div class="panel-head"><h2>课程公告</h2><span class="count">共 {{ announcements.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>公告</th><th>受众</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in announcements" :id="`announcement-${item.announcementId}`" :key="item.announcementId" data-test="announcement-row" :class="{ 'deep-linked': item.announcementId === targetAnnouncementId }">
            <td><span class="cell-strong">{{ item.title }}</span><span class="cell-sub">{{ item.content }}</span></td>
            <td>{{ audienceLabel(item.audience) }}</td>
            <td><StatusBadge :tone="announcementTone(item)" :label="announcementLabel(item)" /></td>
            <td>{{ item.publishedAt ? formatTime(item.publishedAt) : '尚未发布' }}</td>
            <td><button v-if="item.status === 'DRAFT'" class="text-link" @click="confirmAnnouncement(item)">确认发布</button><button v-if="item.status === 'PUBLISHED'" class="text-link" @click="withdrawAnnouncement(item)">撤回</button></td>
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
    <AppModal :open="Boolean(currentTopic)" title="课程讨论详情" wide :description="currentTopic ? `${currentTopic.title} · ${currentTopic.authorName || currentTopic.authorId}` : ''" @close="closeReplies">
      <section v-if="topicDetail" class="notice"><div><strong>主题内容</strong><p class="pre-line">{{ topicDetail.content }}</p></div></section>
      <ul class="reply-list">
        <li v-for="reply in replies" :key="reply.replyId" class="reply-item" data-test="reply-item">
          <div class="reply-author"><UserAvatar :file-id="reply.authorAvatarFileId" :name="reply.authorName || reply.authorId" :size="34" /><div><strong>{{ reply.authorName || reply.authorId }}</strong><span class="muted">{{ formatTime(reply.createdAt) }}</span></div></div>
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
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Eye, EyeOff, MessageCircle, Pin, Plus, Sparkles } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'; import UserAvatar from '@/components/UserAvatar.vue'
import { announcementsApi, forumApi, teacherCoursesApi } from '@/services/api'
const route = useRoute()
import type { AnnouncementAudience, AnnouncementVO, ForumReplyVO, ForumTopicDetailVO, ForumTopicListItemVO, TeacherCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { confirmDialog } from '@/services/confirmDialog'
const targetAnnouncementId = computed(() => String(route.query.announcementId || ''))
const focusedAnnouncement = computed(() => announcements.value.find((item) => item.announcementId === targetAnnouncementId.value) ?? null)
const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const courseId = ref(''); const topics = ref<ForumTopicListItemVO[]>([]); const topicDetails = reactive<Record<string, ForumTopicDetailVO>>({}); const announcements = ref<AnnouncementVO[]>([])
const message = ref('')
const formatTime = formatDateTime
const audienceLabel = (value: AnnouncementAudience) => value === 'ALL' ? '课程全体成员' : value === 'STUDENT' ? '仅选课学生' : '全体教师'
const announcementTone = (item: AnnouncementVO) => item.status === 'PUBLISHED' ? 'green' : item.status === 'DRAFT' ? 'amber' : 'gray'
const announcementLabel = (item: AnnouncementVO) => item.status === 'DRAFT' && item.source === 'AI' ? 'AI 草稿' : item.status === 'DRAFT' ? '草稿' : item.status === 'PUBLISHED' ? '已发布' : '已撤回'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() {
  const page = await state.run(() => teacherCoursesApi.listFormal({ page: 1, size: 100 }))
  if (!page) return
  courses.value = page.records
  const requestedCourseId = String(route.query.courseId || '')
  courseId.value = page.records.some((item) => item.courseId === requestedCourseId) ? requestedCourseId : (courseId.value || page.records[0]?.courseId || '')
  await loadCourseData()
  await nextTick()
  if (targetAnnouncementId.value) document.getElementById(`announcement-${targetAnnouncementId.value}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
async function loadCourseData() { await Promise.all([loadTopics(), loadAnnouncements()]) }
async function loadTopics() {
  if (!courseId.value) return
  const page = await state.run(() => forumApi.teacherTopics(courseId.value, { page: 1, size: 100 }))
  if (!page) return
  topics.value = page.records
  await Promise.all(page.records.map(async (topic) => {
    try { topicDetails[topic.topicId] = await forumApi.teacherTopicDetail(topic.topicId) } catch { /* list remains visible */ }
  }))
}
async function loadAnnouncements() { if (!courseId.value) return; const page = await state.run(() => announcementsApi.teacherCourseList(courseId.value, { page: 1, size: 100 })); if (page) announcements.value = page.records }
async function pinTopic(topic: ForumTopicListItemVO) {
  const updated = await state.run(() => forumApi.teacherTopicPin(topic.topicId, { pinned: !topic.pinned, version: topic.version }))
  if (updated) { flash(updated.pinned ? '主题已置顶' : '已取消置顶'); await loadTopics() }
}async function toggle(topic: ForumTopicListItemVO) { const hide = topic.status.code === 'VISIBLE'; const updated = await state.run(() => forumApi.teacherTopicVisibility(topic.topicId, { visible: !hide, reason: hide ? '教师内容管理' : null, version: topic.version })); if (updated) await loadTopics() }

// —— 课程公告 ——
const announceForm = reactive<{ open: boolean; title: string; content: string; audience: AnnouncementAudience }>({ open: false, title: '', content: '', audience: 'ALL' })
function openAnnounce() { Object.assign(announceForm, { open: true, title: '', content: '', audience: 'ALL' }) }
async function publishAnnouncement() {
  const created = await state.run(() => announcementsApi.createCourseAnnouncement(courseId.value, { title: announceForm.title.trim(), content: announceForm.content.trim(), audience: announceForm.audience }))
  if (created) { announceForm.open = false; flash('课程公告已发布'); await loadAnnouncements() }
}
async function withdrawAnnouncement(item: AnnouncementVO) {
  if (!(await confirmDialog(`确认撤回公告《${item.title}》？撤回后学生不再看到该公告。`, { title: '撤回公告', confirmLabel: '确认撤回' }))) return
  const updated = await state.run(() => announcementsApi.teacherWithdraw(item.announcementId, { version: item.version }))
  if (updated) { flash('公告已撤回'); await loadAnnouncements() }
}
async function confirmAnnouncement(item: AnnouncementVO) {
  const updated = await state.run(() => announcementsApi.confirmCourseAnnouncement(item.announcementId))
  if (updated) { flash('AI 草稿公告已确认发布'); await loadAnnouncements() }
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
watch(() => [route.query.courseId, route.query.announcementId], () => { void load() })
</script>

<style scoped>
.ai-draft-banner { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 12px; align-items: center; margin: 14px 0; padding: 14px 16px; border: 1px solid #bcd5ff; border-radius: 14px; color: #174b91; background: linear-gradient(110deg, #eef6ff, #f7f5ff); }
.ai-draft-banner > svg { color: #3867e8; }
.ai-draft-banner strong { display: block; font-size: 14px; }
.ai-draft-banner p { margin: 3px 0 0; color: #64748b; font-size: 12px; }
.deep-linked { outline: 2px solid #79a8ff; outline-offset: -2px; background: #eff6ff !important; }
@media (max-width: 720px) { .ai-draft-banner { grid-template-columns: auto 1fr; } }

.discussion-board { overflow: hidden; border: 1px solid rgba(255,255,255,.78); border-radius: 18px; background: rgba(233,246,255,.42); box-shadow: inset 0 1px 0 rgba(255,255,255,.58), 0 8px 14px rgba(31,99,155,.12); backdrop-filter: blur(20px) saturate(155%); }
.discussion-board-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 18px 20px; background: rgba(255,255,255,.42); border-bottom: 1px solid rgba(255,255,255,.58); }
.discussion-board-head h2 { margin: 0; font-size: 19px; }
.discussion-board-head p { margin: 5px 0 0; color: var(--muted); font-size: 13px; }
.discussion-post { display: grid; grid-template-columns: 52px minmax(0, 1fr); gap: 14px; margin: 14px; padding: 18px; background: rgba(255,255,255,.48); border: 1px solid rgba(255,255,255,.66); box-shadow: inset 0 1px 0 rgba(255,255,255,.55), 0 4px 10px rgba(34,93,141,.08); }
.forum-avatar { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 50%; color: #075fc9; background: linear-gradient(145deg,#dbeafe,#eff6ff); border: 1px solid #bfdbfe; font-size: 18px; font-weight: 800; }
.post-body { min-width: 0; }
.post-author { display: flex; justify-content: space-between; gap: 14px; }
.post-author > div { display: grid; gap: 3px; }
.post-author strong { color: #0969da; }
.post-author span { color: var(--muted); font-size: 12px; }
.post-title { margin-top: 14px; }
.post-title h3 { margin: 0; font-size: 17px; }
.post-content { min-height: 36px; margin: 10px 0 16px; color: #334155; line-height: 1.75; }
.post-actions { display: flex; gap: 26px; padding-top: 12px; border-top: 1px solid #eef2f7; }
.post-action { display: inline-flex; align-items: center; gap: 7px; border: 0; color: #64748b; background: transparent; cursor: pointer; }
.post-action:hover { color: var(--primary); }
.reply-list { list-style: none; margin: 16px 0 0; padding: 0; display: grid; gap: 10px; }
.reply-author { display: flex; align-items: center; gap: 10px; }
.reply-author > div { display: grid; gap: 2px; }
.reply-item { border: 1px solid var(--line); padding: 14px; display: grid; gap: 6px; background: #f8fafc; }
.pin-mark { margin-right: 7px; padding: 2px 6px; color: var(--primary); background: var(--primary-soft); font-size: 11px; }
@media (max-width: 680px) { .discussion-post { grid-template-columns: 40px minmax(0,1fr); margin: 8px; padding: 13px; } .forum-avatar { width: 38px; height: 38px; font-size: 14px; } .post-actions { gap: 12px; flex-wrap: wrap; } }
</style>
