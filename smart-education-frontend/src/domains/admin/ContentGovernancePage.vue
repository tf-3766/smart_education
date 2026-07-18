<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">内容治理</h1><p class="page-subtitle">维护系统公告，所有变更通过后端保存。</p></div><AppButton variant="primary" @click="openAnnounce = true"><span class="row"><Megaphone :size="16" />发布系统公告</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div><AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel flush"><div class="panel-head"><h2>系统公告</h2><span class="count">{{ announcements.length }} 项</span></div><div class="table-scroll"><table class="table"><thead><tr><th>公告</th><th>受众</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in announcements" :key="item.announcementId"><td><span class="cell-strong">{{ item.title }}</span><span class="cell-sub">{{ item.content }}</span></td><td>{{ audienceLabel(item.audience) }}</td><td><StatusBadge :tone="item.status === 'PUBLISHED' ? 'green' : 'gray'" :label="item.status === 'PUBLISHED' ? '已发布' : '已撤回'" /></td><td>{{ formatTime(item.publishedAt) }}</td><td><button v-if="item.status === 'PUBLISHED'" class="text-link" @click="withdraw(item)">撤回</button></td></tr><tr v-if="!announcements.length"><td colspan="5" class="list-empty">暂无系统公告</td></tr></tbody></table></div></section>
    <section class="panel push-top"><h2 class="panel-title">论坛内容治理</h2><p class="muted">当前后端仅提供按主题或回复 ID 修改可见性的接口，尚未提供管理员全局待治理列表查询，因此本页不展示推测数据。</p></section>
    <AppModal :open="openAnnounce" title="发布系统公告" description="公告将按受众范围展示。" @close="openAnnounce = false"><label class="field-label" for="cg-ann-title">公告标题</label><input id="cg-ann-title" v-model="announcementForm.title" class="input" placeholder="公告标题" /><label class="field-label push-top" for="cg-ann-audience">受众范围</label><select id="cg-ann-audience" v-model="announcementForm.audience" class="select"><option value="ALL">全体用户</option><option value="TEACHER">全体教师</option><option value="STUDENT">全体学生</option></select><label class="field-label push-top" for="cg-ann-content">公告内容</label><textarea id="cg-ann-content" v-model="announcementForm.content" class="textarea" placeholder="公告正文" /><div class="form-actions"><AppButton variant="secondary" @click="openAnnounce = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!announcementForm.title.trim() || !announcementForm.content.trim()" @click="publish">发布</AppButton></div></AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { onMounted, reactive, ref } from 'vue'; import { Megaphone } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { announcementsApi } from '@/services/api'; import type { AnnouncementAudience, AnnouncementVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
const state = usePageState(); const announcements = ref<AnnouncementVO[]>([]); const openAnnounce = ref(false); const message = ref('')
const announcementForm = reactive<{ title: string; content: string; audience: AnnouncementAudience }>({ title: '', content: '', audience: 'ALL' })
const formatTime = formatDateTime; const audienceLabel = (value: AnnouncementAudience) => value === 'ALL' ? '全体用户' : value === 'TEACHER' ? '全体教师' : '全体学生'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => announcementsApi.adminList({ page: 1, size: 100 })); if (page) announcements.value = page.records }
async function publish() { const result = await state.run(() => announcementsApi.adminCreate({ ...announcementForm })); if (result) { openAnnounce.value = false; announcementForm.title = ''; announcementForm.content = ''; flash('公告已发布'); await load() } }
async function withdraw(item: AnnouncementVO) { const result = await state.run(() => announcementsApi.adminWithdraw(item.announcementId, { version: item.version })); if (result) { flash('公告已撤回'); await load() } }
onMounted(load)
</script>
