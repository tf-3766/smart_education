<template>
  <div><div class="page-header"><div><h1 class="page-title">用户管理</h1><p class="page-subtitle">审核教师注册并维护管理员授权，所有操作由后端审计。</p></div></div><div v-if="message" class="toast">{{ message }}</div>
    <div class="tab-bar"><button v-for="tab in roleTabs" :key="tab.value" class="tab" :class="{ 'tab-active': roleTab === tab.value }" @click="roleTab = tab.value">{{ tab.label }}<span class="tab-count">{{ countByRole(tab.value) }}</span></button></div>
    <div class="filter-bar"><label class="filter-field grow"><span>搜索用户</span><span class="field-with-icon"><Search :size="16" /><input v-model="keyword" class="input" placeholder="用户名或姓名" /></span></label><label class="filter-field"><span>状态</span><select v-model="status" class="select" @change="load"><option value="">全部</option><option value="PENDING">待审核</option><option value="ENABLED">已启用</option><option value="REJECTED">已驳回</option></select></label></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel flush"><div class="panel-head"><h2>用户列表</h2><span class="count">共 {{ filtered.length }} 人</span></div><div class="table-scroll"><table class="table"><thead><tr><th>用户</th><th>用户名</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody><tr v-for="user in filtered" :key="user.userId"><td><span class="row"><span class="user-avatar" style="width: 28px; height: 28px; font-size: 12px">{{ user.displayName.slice(0, 1) }}</span><span class="cell-strong">{{ user.displayName }}</span></span></td><td>{{ user.username }}</td><td><StatusBadge tone="blue" :label="user.roles.join(' / ')" /></td><td><StatusBadge :tone="user.userStatus === 'ENABLED' ? 'green' : user.userStatus === 'PENDING' ? 'amber' : 'red'" :label="statusLabel(user.userStatus)" /></td><td>{{ formatTime(user.createdAt) }}</td><td class="cell-actions"><button v-if="user.userStatus === 'PENDING'" class="text-link" @click="approve(user)">通过教师申请</button><button v-if="user.userStatus === 'PENDING'" class="text-link" @click="reject(user)">驳回</button><button v-if="user.userStatus === 'ENABLED' && !user.roles.includes('ADMIN')" class="text-link" @click="grant(user)">授予管理员</button><button v-if="user.roles.includes('ADMIN') && !user.superAdministrator" class="text-link" @click="revoke(user)">撤销管理员</button></td></tr><tr v-if="!filtered.length"><td colspan="6" class="list-empty">暂无匹配用户</td></tr></tbody></table></div></section>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, ref } from 'vue'; import { Search } from 'lucide-vue-next'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { adminUsersApi } from '@/services/api'; import type { AdminUserVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
const state = usePageState(); const users = ref<AdminUserVO[]>([]); const keyword = ref(''); const status = ref(''); const message = ref('')
// 按角色分组展示，避免三类用户混在一张表里。ADMIN 归入「管理员」，含 SUPER_ADMIN。
const roleTabs = [
  { value: '', label: '全部' },
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'ADMIN', label: '管理员' },
] as const
const roleTab = ref<string>('')
const matchRole = (user: AdminUserVO, role: string) => !role || user.roles.includes(role)
const countByRole = (role: string) => users.value.filter((user) => matchRole(user, role)).length
const filtered = computed(() => users.value.filter((user) =>
  matchRole(user, roleTab.value)
  && (!keyword.value || `${user.username}${user.displayName}`.toLowerCase().includes(keyword.value.toLowerCase()))))
const formatTime = formatDateTime; const statusLabel = (value: string) => value === 'ENABLED' ? '已启用' : value === 'PENDING' ? '待审核' : '已驳回'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => adminUsersApi.list({ page: 1, size: 100, status: status.value || undefined })); if (page) users.value = page.records }
async function run(op: () => Promise<AdminUserVO>, text: string) { const result = await state.run(op); if (result) { flash(text); await load() } }
const approve = (user: AdminUserVO) => run(() => adminUsersApi.approveTeacher(user.userId), '教师申请已通过')
const reject = (user: AdminUserVO) => run(() => adminUsersApi.rejectTeacher(user.userId), '教师申请已驳回')
const grant = (user: AdminUserVO) => run(() => adminUsersApi.grantAdministrator(user.userId), '管理员权限已授予')
const revoke = (user: AdminUserVO) => run(() => adminUsersApi.revokeAdministrator(user.userId), '管理员权限已撤销')
onMounted(load)
</script>

<style scoped>
.tab-bar { display: flex; gap: 6px; margin-bottom: 14px; border-bottom: 1px solid var(--line); }
.tab { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border: 0; background: transparent; color: var(--muted); font-size: 14px; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tab:hover { color: var(--ink); }
.tab-active { color: var(--brand); border-bottom-color: var(--brand); font-weight: 600; }
.tab-count { min-width: 18px; padding: 0 6px; border-radius: 9px; background: var(--brand-weak); color: var(--brand-ink); font-size: 12px; line-height: 18px; text-align: center; }
</style>
