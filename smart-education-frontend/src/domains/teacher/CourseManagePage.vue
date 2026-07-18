<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">课程管理</h1><p class="page-subtitle">维护课程、提交审核并发布或下线课程。</p></div><AppButton variant="primary" @click="startCreate"><span class="row"><Plus :size="16" />新建课程</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <section v-if="invitations.length" class="panel flush invite-panel">
      <div class="panel-head"><h2>待我确认的协作邀请</h2><span class="count">{{ invitations.length }} 条</span></div>
      <div class="table-scroll"><table class="table">
        <tbody>
          <tr v-for="inv in invitations" :key="inv.courseId">
            <td class="cell-strong">《{{ inv.courseName }}》</td>
            <td>{{ inv.inviterName }} 邀请你加入协作团队</td>
            <td class="cell-actions">
              <button class="text-link" @click="respondInvitation(inv, true)">接受</button>
              <button class="text-link" @click="respondInvitation(inv, false)">拒绝</button>
            </td>
          </tr>
        </tbody>
      </table></div>
    </section>
    <div class="filter-bar"><label class="filter-field grow"><span>搜索课程</span><span class="field-with-icon"><Search :size="16" /><input v-model="keyword" class="input" placeholder="课程名称或编码" /></span></label><label class="filter-field"><span>状态</span><select v-model="statusFilter" class="select"><option value="">全部状态</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下线</option></select></label></div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel flush">
      <div class="panel-head"><h2>负责课程</h2><span class="count">共 {{ filtered.length }} 门</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>课程名称</th><th>编码</th><th>学期</th><th>课程状态</th><th>审核状态</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="course in filtered" :key="course.courseId"><td class="cell-strong">{{ course.name }}</td><td>{{ course.courseCode }}</td><td>{{ course.term || '—' }}</td><td><StatusBadge :tone="course.status.code === 'PUBLISHED' ? 'green' : 'gray'" :label="course.status.label" /></td><td><StatusBadge :tone="course.reviewStatus.code === 'PENDING' ? 'amber' : course.reviewStatus.code === 'APPROVED' ? 'green' : 'gray'" :label="course.reviewStatus.label" /></td><td>{{ formatTime(course.updatedAt) }}</td><td class="cell-actions"><RouterLink class="text-link" :to="`/teacher/courses/${course.courseId}/content`">内容管理</RouterLink><button class="text-link" @click="startEdit(course.courseId)">编辑</button><button class="text-link" @click="openTeam(course)">课程团队</button><button v-if="canSubmitReview(course)" class="text-link" @click="mutate(course.courseId, 'review')">{{ course.reviewStatus.code === 'REJECTED' ? '重新提交审核' : '提交审核' }}</button><button v-if="course.reviewStatus.code === 'REJECTED'" class="text-link" @click="showReason(course)">驳回原因</button><button v-if="course.reviewStatus.code === 'APPROVED' && course.status.code !== 'PUBLISHED'" class="text-link" @click="mutate(course.courseId, 'publish')">发布</button><button v-if="course.status.code === 'PUBLISHED'" class="text-link" @click="mutate(course.courseId, 'offline')">下线</button></td></tr>
          <tr v-if="!filtered.length"><td colspan="7" class="list-empty">未找到匹配课程</td></tr>
        </tbody>
      </table></div>
    </section>
    <AppModal :open="openCreate" title="新建课程" description="可从内置课程库选用历年课程，也可自定义新课（会自动沉淀进内置库）。" @close="closeCreate">
      <label class="field-label" for="cm-template">课程</label>
      <select id="cm-template" v-model="templateId" class="select" @change="applyTemplate">
        <option value="">自定义新课程</option>
        <option v-for="tpl in templates" :key="tpl.templateId" :value="tpl.templateId">{{ tpl.name }}（{{ tpl.courseCode }}）</option>
      </select>
      <label class="field-label push-top" for="cm-code">课程编码</label><input id="cm-code" v-model.trim="form.courseCode" class="input" placeholder="字母、数字与 . _ - ，如 WEB2026" />
      <label class="field-label push-top" for="cm-name">课程名称</label><input id="cm-name" v-model="form.name" class="input" placeholder="课程名称" />
      <label class="field-label push-top" for="cm-summary">课程简介</label><textarea id="cm-summary" v-model="form.summary" class="textarea" placeholder="课程目标与内容" />
      <div class="form-grid push-top"><div><label class="field-label" for="cm-term">学期</label><select id="cm-term" v-model="form.term" class="select"><option value="">未设置</option><option v-for="term in termOptions" :key="term" :value="term">{{ term }}</option></select></div><div><label class="field-label" for="cm-credit">学分</label><input id="cm-credit" v-model.number="form.credit" class="input" type="number" min="0" /></div></div>
      <template v-if="termWindow && (termWindow.enrollmentOpenAt || termWindow.enrollmentCloseAt)">
        <div class="field-label push-top">选课时间</div>
        <p class="muted" style="margin: 6px 0 0; font-size: 12.5px">本学期选课时间由管理员统一设置：{{ windowText(termWindow) }}，创建后自动套用，无需单独填写。</p>
      </template>
      <template v-else>
        <div class="form-grid push-top"><div><label class="field-label" for="cm-enroll-open">选课开始</label><input id="cm-enroll-open" v-model="form.enrollmentOpenAt" class="input" type="datetime-local" /></div><div><label class="field-label" for="cm-enroll-close">选课截止</label><input id="cm-enroll-close" v-model="form.enrollmentCloseAt" class="input" type="datetime-local" /></div></div>
        <p class="muted" style="margin: 6px 0 0; font-size: 12.5px">该学期管理员暂未设置统一选课窗口，可自定义；均留空表示不限时。</p>
      </template>
      <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
      <div class="form-actions"><AppButton variant="secondary" @click="closeCreate">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!form.courseCode.trim() || !form.name.trim()" @click="create">创建草稿</AppButton></div>
    </AppModal>
    <AppModal :open="teamModal.open" title="课程团队" :description="`管理《${teamModal.name}》的主讲与协作教师`" @close="teamModal.open = false">
      <div class="table-scroll"><table class="table">
        <thead><tr><th>教师</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in teamModal.teachers" :key="t.relationId"><td class="cell-strong">{{ t.teacherName }}<span class="muted"> · ID {{ t.teacherId }}</span></td><td><StatusBadge :tone="t.role.code === 'OWNER' ? 'green' : 'blue'" :label="t.role.label" /></td><td><StatusBadge v-if="t.role.code !== 'OWNER'" :tone="t.status.code === 'ACTIVE' ? 'green' : 'amber'" :label="t.status.label" /><span v-else class="muted">—</span></td><td class="cell-actions"><button v-if="t.role.code !== 'OWNER'" class="text-link" @click="removeTeam(t)">{{ t.status.code === 'PENDING' ? '撤回邀请' : '移除' }}</button><span v-else class="muted">主讲不可移除</span></td></tr>
          <tr v-if="!teamModal.teachers.length"><td colspan="4" class="list-empty">暂无团队成员</td></tr>
        </tbody>
      </table></div>
      <label class="field-label push-top" for="cm-team-id">邀请协作教师</label>
      <div class="row" style="gap: 8px">
        <select id="cm-team-id" v-model="teamModal.teacherIdInput" class="select">
          <option value="">请选择教师</option>
          <option v-for="t in addableTeachers" :key="t.teacherId" :value="t.teacherId">{{ t.teacherName }}（ID {{ t.teacherId }}）</option>
        </select>
        <AppButton variant="primary" :loading="state.loading.value" :disabled="!teamModal.teacherIdInput" @click="addTeam">邀请</AppButton>
      </div>
      <p class="muted" style="margin: 6px 0 0; font-size: 12.5px">邀请发出后需对方在其「课程管理」页确认接受，才会正式加入团队。</p>
      <p v-if="!addableTeachers.length" class="muted" style="margin: 6px 0 0">暂无其他可添加的教师。</p>
      <p v-if="teamModal.error" class="form-error" role="alert">{{ teamModal.error }}</p>
    </AppModal>
    <AppModal :open="reasonModal.open" title="驳回原因" :description="`《${reasonModal.name}》未通过审核`" @close="reasonModal.open = false">
      <p class="pre-line">{{ reasonModal.text || '管理员未填写具体驳回原因，请联系管理员了解详情。' }}</p>
      <div class="form-actions"><AppButton variant="primary" @click="reasonModal.open = false">知道了</AppButton></div>
    </AppModal>
    <AppModal :open="editOpen" title="编辑课程" description="课程编码不可修改；其余信息保存后立即生效。" @close="closeEdit">
      <label class="field-label" for="ce-name">课程名称</label><input id="ce-name" v-model="editForm.name" class="input" />
      <label class="field-label push-top" for="ce-summary">课程简介</label><textarea id="ce-summary" v-model="editForm.summary" class="textarea" />
      <div class="form-grid push-top"><div><label class="field-label" for="ce-term">学期</label><select id="ce-term" v-model="editForm.term" class="select"><option value="">未设置</option><option v-for="term in termOptions" :key="term" :value="term">{{ term }}</option></select></div><div><label class="field-label" for="ce-credit">学分</label><input id="ce-credit" v-model.number="editForm.credit" class="input" type="number" min="0" /></div></div>
      <div class="form-grid push-top"><div><label class="field-label" for="ce-enroll-open">选课开始</label><input id="ce-enroll-open" v-model="editForm.enrollmentOpenAt" class="input" type="datetime-local" /></div><div><label class="field-label" for="ce-enroll-close">选课截止</label><input id="ce-enroll-close" v-model="editForm.enrollmentCloseAt" class="input" type="datetime-local" /></div></div>
      <p v-if="editError" class="form-error" role="alert">{{ editError }}</p>
      <div class="form-actions"><AppButton variant="secondary" @click="closeEdit">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!editForm.name.trim()" @click="saveEdit">保存修改</AppButton></div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Search } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { teacherCoursesApi } from '@/services/api'; import type { CollabInvitationVO, CourseTeacherVO, CourseTemplateVO, TeacherCourseListItemVO, TeacherOptionVO, TermEnrollmentWindowVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'

const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const keyword = ref(''); const statusFilter = ref(''); const openCreate = ref(false); const message = ref(''); const createError = ref('')
const templates = ref<CourseTemplateVO[]>([]); const templateId = ref('')
// 管理员按学期设置的统一选课窗口：选好学期后自动套用其选课时间，无需教师自定义。
const termWindows = ref<TermEnrollmentWindowVO[]>([])
const termWindow = computed(() => termWindows.value.find((w) => w.term === form.term) ?? null)
function windowText(w: TermEnrollmentWindowVO) {
  const open = w.enrollmentOpenAt ? formatDateTime(w.enrollmentOpenAt) : '开始不限'
  const close = w.enrollmentCloseAt ? formatDateTime(w.enrollmentCloseAt) : '截止不限'
  return `${open} ~ ${close}`
}
// 该学期是否已有管理员配置的选课窗口（决定隐藏自定义输入并直接套用窗口时间）。
const useTermWindow = computed(() => !!(termWindow.value && (termWindow.value.enrollmentOpenAt || termWindow.value.enrollmentCloseAt)))
function applyTemplate() {
  const tpl = templates.value.find((item) => item.templateId === templateId.value)
  if (!tpl) return
  form.courseCode = tpl.courseCode; form.name = tpl.name; form.summary = tpl.summary ?? ''
}
// 学期统一为「YYYY 春季/秋季」，避免自由文本导致按学期筛选对不上。范围取当年 ±1。
const termOptions = computed(() => {
  const year = new Date().getFullYear()
  const options: string[] = []
  for (let y = year - 1; y <= year + 1; y += 1) options.push(`${y} 春季`, `${y} 秋季`)
  return options
})
const currentTerm = `${new Date().getFullYear()} ${new Date().getMonth() + 1 <= 6 ? '春季' : '秋季'}`
const form = reactive({ courseCode: '', name: '', summary: '', term: currentTerm, credit: 2, enrollmentOpenAt: '', enrollmentCloseAt: '' })
const filtered = computed(() => courses.value.filter((course) => (!statusFilter.value || course.status.code === statusFilter.value) && (!keyword.value || `${course.name}${course.courseCode}`.toLowerCase().includes(keyword.value.toLowerCase()))))
const formatTime = formatDateTime
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => teacherCoursesApi.list({ page: 1, size: 100 })); if (page) courses.value = page.records }
// 我收到的待确认协作邀请（顶部提醒区）。
const invitations = ref<CollabInvitationVO[]>([])
async function loadInvitations() { const rows = await state.run(() => teacherCoursesApi.listInvitations()); if (rows) invitations.value = rows }
async function respondInvitation(inv: CollabInvitationVO, accept: boolean) {
  const ok = await state.run(async () => {
    if (accept) await teacherCoursesApi.acceptInvitation(inv.courseId)
    else await teacherCoursesApi.rejectInvitation(inv.courseId)
    return true
  })
  if (ok) { flash(accept ? `已接受《${inv.courseName}》的协作邀请` : '已拒绝该邀请'); await loadInvitations(); if (accept) await load() }
}
async function mutate(id: string, action: 'review' | 'publish' | 'offline') { const result = await state.run(() => action === 'review' ? teacherCoursesApi.submitReview(id) : action === 'publish' ? teacherCoursesApi.publish(id) : teacherCoursesApi.offline(id)); if (result) { flash('课程状态已更新'); await load() } }
// 审核状态为「未提交」或「已驳回」时才可提交/重新提交；「待审核」「已通过」不再显示提交入口。
function canSubmitReview(course: TeacherCourseListItemVO) {
  return ['NOT_SUBMITTED', 'REJECTED'].includes(course.reviewStatus.code) && course.status.code !== 'PUBLISHED'
}
// 课程团队：主讲（OWNER）+ 协作教师（COLLABORATOR）。后端无教师目录接口，按用户 ID 添加。
const teamModal = reactive({ open: false, courseId: '', name: '', teacherIdInput: '', error: '', teachers: [] as CourseTeacherVO[] })
// 教师目录（全部启用教师），下拉里排除已在团队中的成员。
const teacherDirectory = ref<TeacherOptionVO[]>([])
const addableTeachers = computed(() => {
  const inTeam = new Set(teamModal.teachers.map((t) => t.teacherId))
  return teacherDirectory.value.filter((t) => !inTeam.has(t.teacherId))
})
async function openTeam(course: TeacherCourseListItemVO) {
  Object.assign(teamModal, { open: true, courseId: course.courseId, name: course.name, teacherIdInput: '', error: '', teachers: [] })
  await reloadTeam()
  // 教师目录为辅助能力：接口不可用（如后端未重构建）时静默降级为空列表，不打断团队管理。
  teacherDirectory.value = []
  void teacherCoursesApi.teacherDirectory().then((dir) => { teacherDirectory.value = dir }).catch(() => { teacherDirectory.value = [] })
}
async function reloadTeam() {
  const rows = await state.run(() => teacherCoursesApi.listTeachers(teamModal.courseId))
  if (rows) teamModal.teachers = rows
}
async function addTeam() {
  teamModal.error = ''
  const result = await state.run(() => teacherCoursesApi.addTeacher(teamModal.courseId, { teacherId: teamModal.teacherIdInput }))
  if (result) { teamModal.teacherIdInput = ''; flash('邀请已发出，等待对方确认'); await reloadTeam() }
  else teamModal.error = state.error.value?.message ?? '邀请失败，请稍后重试。'
}
async function removeTeam(t: CourseTeacherVO) {
  const msg = t.status.code === 'PENDING' ? `撤回对「${t.teacherName}」的协作邀请？` : `将「${t.teacherName}」移出课程团队？`
  if (!window.confirm(msg)) return
  const result = await state.run(async () => { await teacherCoursesApi.removeTeacher(teamModal.courseId, t.teacherId); return true })
  if (result) await reloadTeam()
  else teamModal.error = state.error.value?.message ?? '移除失败。'
}
// 驳回原因只在课程详情（latestReviewReason）里，列表 VO 不带，故按需拉取详情。
const reasonModal = reactive({ open: false, name: '', text: '' })
async function showReason(course: TeacherCourseListItemVO) {
  reasonModal.name = course.name; reasonModal.text = ''; reasonModal.open = true
  const detail = await state.run(() => teacherCoursesApi.getDetail(course.courseId))
  if (detail) reasonModal.text = detail.latestReviewReason ?? ''
}
const editOpen = ref(false); const editError = ref('')
// 后端 update 为整体覆盖：未回传的字段会被清空，故 coverUrl/分类/院系/起止时间原样透传。
const editForm = reactive({
  courseId: '', name: '', summary: '', term: '', credit: 0, enrollmentOpenAt: '', enrollmentCloseAt: '', version: 0,
  coverUrl: null as string | null, categoryId: null as string | null, department: null as string | null,
  startAt: null as string | null, endAt: null as string | null,
})
async function startEdit(courseId: string) {
  const detail = await state.run(() => teacherCoursesApi.getDetail(courseId))
  if (!detail) return
  Object.assign(editForm, {
    courseId,
    name: detail.name,
    summary: detail.summary ?? '',
    term: detail.term ?? '',
    credit: detail.credit ?? 0,
    enrollmentOpenAt: detail.enrollmentOpenAt ? detail.enrollmentOpenAt.slice(0, 16) : '',
    enrollmentCloseAt: detail.enrollmentCloseAt ? detail.enrollmentCloseAt.slice(0, 16) : '',
    version: detail.version,
    coverUrl: detail.coverUrl ?? null,
    categoryId: detail.categoryId ?? null,
    department: detail.department ?? null,
    startAt: detail.startAt ?? null,
    endAt: detail.endAt ?? null,
  })
  editError.value = ''
  editOpen.value = true
}
function closeEdit() { editOpen.value = false; editError.value = '' }
async function saveEdit() {
  editError.value = ''
  if (editForm.enrollmentOpenAt && editForm.enrollmentCloseAt && new Date(editForm.enrollmentOpenAt) >= new Date(editForm.enrollmentCloseAt)) {
    editError.value = '选课截止时间需晚于开始时间'; return
  }
  const result = await state.run(() => teacherCoursesApi.update(editForm.courseId, {
    name: editForm.name.trim(),
    summary: editForm.summary,
    term: editForm.term,
    credit: editForm.credit,
    coverUrl: editForm.coverUrl,
    categoryId: editForm.categoryId,
    department: editForm.department,
    startAt: editForm.startAt,
    endAt: editForm.endAt,
    enrollmentOpenAt: editForm.enrollmentOpenAt ? new Date(editForm.enrollmentOpenAt).toISOString() : null,
    enrollmentCloseAt: editForm.enrollmentCloseAt ? new Date(editForm.enrollmentCloseAt).toISOString() : null,
    version: editForm.version,
  }))
  if (result) { closeEdit(); flash('课程信息已更新'); await load() }
  else editError.value = state.error.value?.message ?? '保存失败，请稍后重试。'
}
function startCreate() {
  createError.value = ''; templateId.value = ''; openCreate.value = true
  // 内置课程库为辅助能力：接口不可用时静默降级为自定义新课。
  void teacherCoursesApi.templates().then((list) => { templates.value = list }).catch(() => { templates.value = [] })
  // 载入学期选课窗口（选好学期后自动套用其选课时间，见 create()）。
  void teacherCoursesApi.termWindows().then((list) => { termWindows.value = list }).catch(() => { termWindows.value = [] })
}
function closeCreate() { openCreate.value = false; createError.value = '' }
async function create() {
  createError.value = ''
  const code = form.courseCode.trim()
  if (!/^[A-Za-z0-9._-]+$/.test(code)) {
    createError.value = '课程编码只能包含字母、数字和 . _ - ，不能有中文或空格。'; return
  }
  if (code.length > 64) {
    createError.value = '课程编码不能超过 64 个字符。'; return
  }
  // 有管理员窗口时直接套用其原始时间（不经 datetime-local 转换，避免时区偏移）；否则用自定义时间并校验。
  const win = termWindow.value
  const enrollmentOpenAt = useTermWindow.value
    ? (win!.enrollmentOpenAt ?? null)
    : (form.enrollmentOpenAt ? new Date(form.enrollmentOpenAt).toISOString() : null)
  const enrollmentCloseAt = useTermWindow.value
    ? (win!.enrollmentCloseAt ?? null)
    : (form.enrollmentCloseAt ? new Date(form.enrollmentCloseAt).toISOString() : null)
  if (!useTermWindow.value && form.enrollmentOpenAt && form.enrollmentCloseAt && new Date(form.enrollmentOpenAt) >= new Date(form.enrollmentCloseAt)) {
    createError.value = '选课截止时间需晚于开始时间'; return
  }
  const result = await state.run(() => teacherCoursesApi.create({
    courseCode: form.courseCode.trim(),
    name: form.name.trim(),
    summary: form.summary,
    term: form.term,
    credit: form.credit,
    enrollmentOpenAt,
    enrollmentCloseAt,
  }))
  if (result) {
    closeCreate(); flash('课程草稿已创建')
    form.courseCode = ''; form.name = ''; form.summary = ''; form.enrollmentOpenAt = ''; form.enrollmentCloseAt = ''
    await load()
  } else {
    createError.value = state.error.value?.message ?? '创建失败，请稍后重试。'
  }
}
onMounted(() => { load(); loadInvitations() })
</script>
