<template>
  <div>
    <div class="page-header">
      <div><h1 class="page-title">内容治理</h1><p class="page-subtitle">维护系统公告，并集中治理论坛主题与回复。</p></div>
      <AppButton variant="primary" @click="openAnnounce = true"><span class="row"><Megaphone :size="16" />发布系统公告</span></AppButton>
    </div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />

    <section class="panel flush">
      <div class="panel-head"><h2>系统公告</h2><span class="count">{{ announcements.length }} 项</span></div>
      <div class="table-scroll"><table class="table"><thead><tr><th>公告</th><th>受众</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in announcements" :key="item.announcementId"><td><span class="cell-strong">{{ item.title }}</span><span class="cell-sub">{{ item.content }}</span></td><td>{{ audienceLabel(item.audience) }}</td><td><StatusBadge :tone="item.status === 'PUBLISHED' ? 'green' : 'gray'" :label="item.status === 'PUBLISHED' ? '已发布' : '已撤回'" /></td><td>{{ formatTime(item.publishedAt) }}</td><td><button v-if="item.status === 'PUBLISHED'" class="text-link" @click="withdraw(item)">撤回</button></td></tr>
        <tr v-if="!announcements.length"><td colspan="5" class="list-empty">暂无系统公告</td></tr>
      </tbody></table></div>
    </section>

    <section class="panel flush push-top">
      <div class="panel-head"><h2>论坛主题治理</h2><span class="count">{{ topics.length }} 项</span></div>
      <div class="table-scroll"><table class="table"><thead><tr><th>主题</th><th>作者</th><th>课程 ID</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in topics" :key="item.topicId"><td><span class="cell-strong">{{ item.title }}</span><span class="cell-sub">{{ formatTime(item.createdAt) }}</span></td><td>{{ item.authorName || item.authorId }}</td><td>{{ item.courseId }}</td><td><StatusBadge :tone="item.status.code === 'VISIBLE' ? 'green' : 'red'" :label="item.status.label" /></td><td><button class="text-link" @click="openModeration('topic', item)">{{ item.status.code === 'VISIBLE' ? '隐藏' : '恢复' }}</button></td></tr>
        <tr v-if="!topics.length"><td colspan="5" class="list-empty">暂无论坛主题</td></tr>
      </tbody></table></div>
    </section>

    <section class="panel flush push-top">
      <div class="panel-head"><h2>论坛回复治理</h2><span class="count">{{ replies.length }} 项</span></div>
      <div class="table-scroll"><table class="table"><thead><tr><th>回复内容</th><th>作者</th><th>主题 ID</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in replies" :key="item.replyId"><td><span class="cell-strong">{{ item.content }}</span><span class="cell-sub">{{ formatTime(item.createdAt) }}</span></td><td>{{ item.authorName || item.authorId }}</td><td>{{ item.topicId }}</td><td><StatusBadge :tone="item.status.code === 'VISIBLE' ? 'green' : 'red'" :label="item.status.label" /></td><td><button class="text-link" @click="openModeration('reply', item)">{{ item.status.code === 'VISIBLE' ? '隐藏' : '恢复' }}</button></td></tr>
        <tr v-if="!replies.length"><td colspan="5" class="list-empty">暂无论坛回复</td></tr>
      </tbody></table></div>
    </section>

    <AppModal :open="openAnnounce" title="发布系统公告" description="公告将按受众范围展示。" @close="openAnnounce = false">
      <label class="field-label" for="cg-ann-title">公告标题</label><input id="cg-ann-title" v-model="announcementForm.title" class="input" placeholder="公告标题" />
      <label class="field-label push-top" for="cg-ann-audience">受众范围</label><select id="cg-ann-audience" v-model="announcementForm.audience" class="select"><option value="ALL">全体用户</option><option value="TEACHER">全体教师</option><option value="STUDENT">全体学生</option></select>
      <label class="field-label push-top" for="cg-ann-content">公告内容</label><textarea id="cg-ann-content" v-model="announcementForm.content" class="textarea" placeholder="公告正文" />
      <div class="form-actions"><AppButton variant="secondary" @click="openAnnounce = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!announcementForm.title.trim() || !announcementForm.content.trim()" @click="publish">发布</AppButton></div>
    </AppModal>

    <AppModal :open="moderation.open" :title="moderation.visible ? '恢复论坛内容' : '隐藏论坛内容'" :description="moderation.label" @close="moderation.open = false">
      <label class="field-label" for="cg-mod-reason">处理原因{{ moderation.visible ? '（可选）' : '' }}</label>
      <textarea id="cg-mod-reason" v-model="moderation.reason" class="textarea" rows="4" placeholder="记录处理依据，便于后续复核" />
      <p v-if="moderation.error" class="form-error" role="alert">{{ moderation.error }}</p>
      <div class="form-actions"><AppButton variant="secondary" @click="moderation.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" @click="applyModeration">确认处理</AppButton></div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { onMounted, reactive, ref } from 'vue'
import { Megaphone } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AppModal from '@/components/AppModal.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { announcementsApi, forumApi } from '@/services/api'
import type { AnnouncementAudience, AnnouncementVO, ForumReplyVO, ForumTopicListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const announcements = ref<AnnouncementVO[]>([])
const topics = ref<ForumTopicListItemVO[]>([])
const replies = ref<ForumReplyVO[]>([])
const openAnnounce = ref(false)
const message = ref('')
const announcementForm = reactive<{ title: string; content: string; audience: AnnouncementAudience }>({ title: '', content: '', audience: 'ALL' })
const moderation = reactive({ open: false, type: 'topic' as 'topic' | 'reply', id: '', label: '', visible: false, version: 0, reason: '', error: '' })
const formatTime = formatDateTime
const audienceLabel = (value: AnnouncementAudience) => value === 'ALL' ? '全体用户' : value === 'TEACHER' ? '全体教师' : '全体学生'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() {
  const result = await state.run(() => Promise.all([
    announcementsApi.adminList({ page: 1, size: 100 }),
    forumApi.adminTopics({ page: 1, size: 100 }),
    forumApi.adminReplies({ page: 1, size: 100 }),
  ]))
  if (result) {
    announcements.value = result[0].records
    topics.value = result[1].records
    replies.value = result[2].records
  }
}
async function publish() {
  const result = await state.run(() => announcementsApi.adminCreate({ ...announcementForm }))
  if (result) { openAnnounce.value = false; announcementForm.title = ''; announcementForm.content = ''; flash('公告已发布'); await load() }
}
async function withdraw(item: AnnouncementVO) {
  const result = await state.run(() => announcementsApi.adminWithdraw(item.announcementId, { version: item.version }))
  if (result) { flash('公告已撤回'); await load() }
}
function openModeration(type: 'topic' | 'reply', item: ForumTopicListItemVO | ForumReplyVO) {
  const topic = type === 'topic' ? item as ForumTopicListItemVO : null
  const reply = type === 'reply' ? item as ForumReplyVO : null
  Object.assign(moderation, {
    open: true,
    type,
    id: topic?.topicId ?? reply!.replyId,
    label: topic ? `主题：${topic.title}` : `回复：${reply!.content}`,
    visible: item.status.code === 'HIDDEN',
    version: item.version,
    reason: '',
    error: '',
  })
}
async function applyModeration() {
  moderation.error = ''
  if (!moderation.visible && !moderation.reason.trim()) { moderation.error = '隐藏内容时必须填写处理原因'; return }
  const body = { visible: moderation.visible, reason: moderation.reason.trim() || null, version: moderation.version }
  const result = moderation.type === 'topic'
    ? await state.run(() => forumApi.adminTopicVisibility(moderation.id, body))
    : await state.run(() => forumApi.adminReplyVisibility(moderation.id, body))
  if (result) { moderation.open = false; flash(moderation.visible ? '内容已恢复' : '内容已隐藏'); await load() }
  else moderation.error = state.error.value?.message ?? '处理失败，请稍后重试。'
}
onMounted(load)
</script>
