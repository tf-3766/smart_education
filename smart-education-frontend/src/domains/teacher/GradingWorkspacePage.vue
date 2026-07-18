<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">作业批改</h1><p class="page-subtitle">创建并发布作业，从提交列表进入批改；评分与评语由教师确认后发布。</p></div><AppButton variant="primary" :disabled="!courseId" @click="openAssignmentForm"><span class="row"><Plus :size="16" />新建作业</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="filter-bar"><label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="loadAssignments"><option value="">选择课程</option><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label><label class="filter-field"><span>作业</span><select v-model="assignmentId" class="select" @change="loadSubmissions"><option value="">选择作业</option><option v-for="item in assignments" :key="item.assignmentId" :value="item.assignmentId">{{ item.title }}</option></select></label></div>
    <section class="panel flush">
      <div class="panel-head"><h2>课程作业</h2><span class="count">共 {{ assignments.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>标题</th><th>状态</th><th>截止时间</th><th class="num">满分</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in assignments" :key="item.assignmentId" data-test="assignment-row" :class="{ 'notification-resource-target': item.assignmentId === targetAssignmentId }">
            <td class="cell-strong">{{ item.title }}</td>
            <td><StatusBadge :tone="item.assignmentStatus.code === 'PUBLISHED' ? 'green' : item.assignmentStatus.code === 'CLOSED' ? 'gray' : 'amber'" :label="item.assignmentStatus.label" /></td>
            <td>{{ formatTime(item.dueAt) }}</td><td class="num">{{ item.maxScore }}</td>
            <td class="cell-actions">
              <button v-if="item.assignmentStatus.code !== 'CLOSED'" class="text-link" @click="openEditAssignment(item)">编辑</button>
              <button v-if="item.assignmentStatus.code === 'DRAFT'" class="text-link" @click="assignmentAction(item.assignmentId, 'publish')">发布</button>
              <button v-if="item.assignmentStatus.code === 'PUBLISHED'" class="text-link" @click="assignmentAction(item.assignmentId, 'close')">截止</button>
              <button class="text-link" @click="assignmentId = item.assignmentId; loadSubmissions()">查看提交</button>
            </td>
          </tr>
          <tr v-if="!assignments.length"><td colspan="5" class="list-empty">当前课程暂无作业，点击右上角新建。</td></tr>
        </tbody>
      </table></div>
    </section>
    <AppModal :open="assignmentForm.open" :title="assignmentForm.assignmentId ? '编辑作业' : '新建作业'" description="创建后为草稿，发布后学生可见并可提交。" @close="assignmentForm.open = false">
      <label class="field-label" for="gw-assign-title">作业标题</label><input id="gw-assign-title" v-model="assignmentForm.title" data-test="assignment-title" class="input" placeholder="作业标题" />
      <label class="field-label push-top" for="gw-assign-desc">作业内容与要求</label><textarea id="gw-assign-desc" v-model="assignmentForm.description" class="textarea" rows="5" placeholder="作业题目、要求与评分标准——学生按此内容作答并提交文本/附件" />
      <div class="form-grid push-top">
        <div><label class="field-label" for="gw-assign-max">满分</label><input id="gw-assign-max" v-model.number="assignmentForm.maxScore" class="input" type="number" min="1" /></div>
        <div><label class="field-label" for="gw-assign-due">截止时间</label><input id="gw-assign-due" v-model="assignmentForm.dueAt" data-test="assignment-due" class="input" type="datetime-local" /></div>
      </div>
      <div class="form-actions"><AppButton variant="secondary" @click="assignmentForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!assignmentForm.title.trim() || !assignmentForm.dueAt" @click="saveAssignment">{{ assignmentForm.assignmentId ? '保存修改' : '保存作业' }}</AppButton></div>
    </AppModal>
    <section class="panel flush">
      <div class="panel-head"><h2>学生提交列表</h2><span class="count">共 {{ submissions.length }} 份</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>学生</th><th>提交时间</th><th>提交状态</th><th class="num">得分</th><th>成绩状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in submissions" :key="item.submissionId"><td class="cell-strong">{{ item.studentName || item.studentId }}</td><td>{{ item.submittedAt ? formatTime(item.submittedAt) : '—' }}</td><td><StatusBadge tone="blue" :label="item.submissionStatus.label" /></td><td class="num">{{ item.score ?? '—' }} / {{ item.maxScore }}</td><td><StatusBadge :tone="item.gradeStatus?.code === 'PUBLISHED' ? 'green' : 'gray'" :label="item.gradeStatus?.label || '未评分'" /></td><td><button class="text-link" @click="open(item)">批改</button></td></tr><tr v-if="!submissions.length"><td colspan="6" class="list-empty">当前作业暂无学生提交</td></tr></tbody>
      </table></div>
    </section>
    <AppModal :open="Boolean(current)" title="批改作业" :description="current ? `${current.studentName || current.studentId} · ${current.submissionStatus.label}` : ''" @close="current = null">
      <section v-if="current" class="notice"><div><strong>学生提交内容</strong><p class="pre-line">{{ current.content || '仅提交了附件' }}</p></div></section>
      <label class="field-label push-top" for="gw-grade-score">得分（满分 {{ current?.maxScore }}）</label><input id="gw-grade-score" v-model.number="score" class="input" type="number" min="0" :max="current?.maxScore" />
      <div class="spread" style="margin-top: 16px"><label class="field-label" for="gw-grade-comment" style="margin: 0">教师评语</label><AiAssistButton label="AI 评语草稿" :loading="aiLoading" @click="draftComment" /></div>
      <textarea id="gw-grade-comment" v-model="comment" class="textarea" placeholder="填写正式评语" />
      <p v-if="aiError" class="form-error" role="alert">{{ aiError }}</p>
      <AiResultPanel v-if="aiDraft" :result="aiDraft" adopt-label="采用到评语" class="push-top" @adopt="comment = $event" @regenerate="draftComment" />
      <div class="form-actions"><AppButton variant="secondary" :loading="state.loading.value" @click="grade(false)">保存评分</AppButton><AppButton variant="primary" :loading="state.loading.value" @click="grade(true)">评分并发布</AppButton></div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from 'lucide-vue-next'
import AiAssistButton from '@/components/AiAssistButton.vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'; import AiResultPanel from '@/components/AiResultPanel.vue'
import { aiApi, assignmentsApi, teacherCoursesApi } from '@/services/api'; import type { AssignmentDetailVO, TeacherCourseListItemVO, TeacherSubmissionGradeVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
import { aiDraftToResult } from '@/services/aiDraft'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const route = useRoute(); const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const assignments = ref<AssignmentDetailVO[]>([]); const submissions = ref<TeacherSubmissionGradeVO[]>([])
const courseId = ref(''); const assignmentId = ref(''); const current = ref<TeacherSubmissionGradeVO | null>(null); const score = ref<number | null>(null); const comment = ref(''); const message = ref('')
const targetAssignmentId = computed(() => typeof route.query.assignmentId === 'string' ? route.query.assignmentId : '')
const targetCourseId = computed(() => typeof route.query.courseId === 'string' ? route.query.courseId : '')
const formatTime = formatDateTime
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => teacherCoursesApi.list({ page: 1, size: 100 })); if (page) { courses.value = page.records; courseId.value = page.records.some((item) => item.courseId === targetCourseId.value) ? targetCourseId.value : courseId.value || page.records[0]?.courseId || ''; assignmentId.value = targetAssignmentId.value || assignmentId.value; await loadAssignments() } }
async function loadAssignments() { if (!courseId.value) return; const page = await state.run(() => assignmentsApi.teacherList(courseId.value, { page: 1, size: 100 })); if (page) { assignments.value = page.records; if (!page.records.some((item) => item.assignmentId === assignmentId.value)) assignmentId.value = page.records[0]?.assignmentId ?? ''; await loadSubmissions() } }
async function loadSubmissions() { if (!assignmentId.value) { submissions.value = []; return } const page = await state.run(() => assignmentsApi.listSubmissions(assignmentId.value, { page: 1, size: 100 })); if (page) submissions.value = page.records }
const aiDraft = ref<AiResult | null>(null); const aiLoading = ref(false); const aiError = ref('')
function open(item: TeacherSubmissionGradeVO) { current.value = item; score.value = item.score ?? null; comment.value = item.teacherComment ?? ''; aiDraft.value = null; aiError.value = '' }
async function draftComment() {
  if (!current.value) return
  aiLoading.value = true; aiError.value = ''
  try {
    const draft = await aiApi.commentDraft(current.value.submissionId)
    aiDraft.value = aiDraftToResult(draft, 'feedback', 'AI 评语草稿')
  } catch (caught) {
    aiError.value = aiErrorMessage(caught)
  } finally { aiLoading.value = false }
}
const assignmentForm = reactive({ open: false, assignmentId: '', version: 0, title: '', description: '', maxScore: 100, dueAt: '' })
function openAssignmentForm() { Object.assign(assignmentForm, { open: true, assignmentId: '', version: 0, title: '', description: '', maxScore: 100, dueAt: '' }) }
function openEditAssignment(item: AssignmentDetailVO) {
  Object.assign(assignmentForm, {
    open: true, assignmentId: item.assignmentId, version: item.version,
    title: item.title, description: item.description ?? '', maxScore: item.maxScore,
    dueAt: item.dueAt ? item.dueAt.slice(0, 16) : '',
  })
}
async function saveAssignment() {
  const body = {
    title: assignmentForm.title.trim(), description: assignmentForm.description.trim() || null,
    maxScore: assignmentForm.maxScore, dueAt: new Date(assignmentForm.dueAt).toISOString(),
  }
  const saved = await state.run(() => assignmentForm.assignmentId
    ? assignmentsApi.update(assignmentForm.assignmentId, { ...body, version: assignmentForm.version })
    : assignmentsApi.create(courseId.value, body))
  if (saved) { assignmentForm.open = false; flash(assignmentForm.assignmentId ? '作业已更新' : '作业已创建（草稿）'); await loadAssignments() }
}
async function assignmentAction(id: string, action: 'publish' | 'close') {
  const result = await state.run(() => action === 'publish' ? assignmentsApi.publish(id) : assignmentsApi.close(id))
  if (result) { flash(action === 'publish' ? '作业已发布' : '作业已截止'); await loadAssignments() }
}
async function grade(publishNow: boolean) { if (!current.value || score.value == null) return; const item = current.value; const graded = await state.run(() => assignmentsApi.grade(item.submissionId, { score: score.value!, maxScore: item.maxScore, teacherComment: comment.value, publishNow, version: item.version })); if (graded) { current.value = null; flash(publishNow ? '成绩已发布' : '评分已保存'); await loadSubmissions() } }
onMounted(load)
watch([targetAssignmentId, targetCourseId], () => { void load() })
</script>
