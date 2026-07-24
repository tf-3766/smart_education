<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">作业批改</h1><p class="page-subtitle">创建并发布作业，从提交列表进入批改；评分与评语由教师确认后发布。</p></div><AppButton variant="primary" :disabled="!courseId" @click="openAssignmentForm"><span class="row"><Plus :size="16" />新建作业</span></AppButton></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="filter-bar"><label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="onCourseChange"><option value="">选择课程</option><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label><label class="filter-field"><span>作业</span><select v-model="assignmentId" class="select" @change="loadSubmissions"><option value="">全部作业</option><option v-for="item in assignments" :key="item.assignmentId" :value="item.assignmentId">{{ item.title }}</option></select></label></div>
    <section class="panel flush">
      <div class="panel-head"><h2>课程作业</h2><span class="count">共 {{ visibleAssignments.length }} 项</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>标题</th><th>状态</th><th>截止时间</th><th class="num">满分</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in visibleAssignments" :key="item.assignmentId" data-test="assignment-row" :class="{ 'notification-resource-target': item.assignmentId === targetAssignmentId }">
            <td class="cell-strong">{{ item.title }}</td>
            <td><StatusBadge :tone="assignmentTone(item)" :label="assignmentLabel(item)" /></td>
            <td>{{ formatTime(item.dueAt) }}</td><td class="num">{{ item.maxScore }}</td>
            <td class="cell-actions">
              <button v-if="item.assignmentStatus.code !== 'CLOSED'" class="text-link" @click="openEditAssignment(item)">编辑</button>
              <button v-if="item.assignmentStatus.code === 'DRAFT'" class="text-link" @click="assignmentAction(item.assignmentId, 'publish')">发布</button>
              <button v-if="item.assignmentStatus.code === 'PUBLISHED'" class="text-link" @click="assignmentAction(item.assignmentId, 'close')">截止</button>
              <button class="text-link" @click="viewSubmissions(item.assignmentId)">查看提交</button>
            </td>
          </tr>
          <tr v-if="!visibleAssignments.length"><td colspan="5" class="list-empty">当前筛选条件下暂无作业。</td></tr>
        </tbody>
      </table></div>
    </section>
    <AppModal :open="assignmentForm.open" :title="assignmentForm.assignmentId ? '编辑作业' : '新建作业'" description="创建后为草稿，发布后学生可见并可提交。" @close="assignmentForm.open = false">
      <label class="field-label" for="gw-assign-title">作业标题</label><input id="gw-assign-title" v-model="assignmentForm.title" data-test="assignment-title" class="input" placeholder="作业标题" />
      <label class="field-label push-top" for="gw-assign-desc">作业内容与要求</label><textarea id="gw-assign-desc" v-model="assignmentForm.description" class="textarea" rows="4" placeholder="说明任务目标、要求与评分标准" />
      <label class="field-label push-top" for="gw-assign-mode">学生完成方式</label>
      <select id="gw-assign-mode" v-model="assignmentForm.responseMode" class="select">
        <option value="MIXED">在线回答或上传附件（可任选或同时提交）</option>
        <option value="TEXT">仅在线文本回答</option>
        <option value="QUIZ">题目作答（选择、判断、填空、简答）</option>
      </select>
      <section v-if="assignmentForm.responseMode === 'QUIZ'" class="quiz-builder push-top">
        <div class="spread"><div><strong>作业题目</strong><p class="muted">为每道题设置题型、题干、分值与参考答案。</p></div><AppButton variant="secondary" @click="addQuestion">+ 添加题目</AppButton></div>
        <article v-for="(question, questionIndex) in assignmentForm.questions" :key="question.questionId" class="question-editor">
          <div class="spread"><strong>第 {{ questionIndex + 1 }} 题</strong><button class="text-link" @click="removeQuestion(questionIndex)">删除</button></div>
          <div class="form-grid push-top">
            <select v-model="question.questionType" class="select" @change="resetQuestion(question)">
              <option value="SINGLE_CHOICE">单选题</option><option value="MULTI_CHOICE">多选题</option><option value="TRUE_FALSE">判断题</option><option value="FILL_BLANK">填空题</option><option value="SHORT_ANSWER">简答题</option>
            </select>
            <input v-model.number="question.score" class="input" type="number" min="0.01" placeholder="分值" />
          </div>
          <textarea v-model="question.stem" class="textarea push-top" rows="2" placeholder="输入题干" />
          <template v-if="choiceQuestion(question.questionType)">
            <div v-for="(_, optionIndex) in question.options" :key="optionIndex" class="option-editor">
              <input :type="question.questionType === 'MULTI_CHOICE' ? 'checkbox' : 'radio'" :name="'assignment-question-' + question.questionId" :checked="question.correctAnswers.includes(String(optionIndex))" @change="toggleCorrect(question, optionIndex)" />
              <span>{{ String.fromCharCode(65 + optionIndex) }}</span>
              <input v-model="question.options[optionIndex]" class="input" :placeholder="'选项 ' + String.fromCharCode(65 + optionIndex)" />
              <button v-if="question.questionType !== 'TRUE_FALSE' && question.options.length > 2" class="text-link" @click="removeOption(question, optionIndex)">删除</button>
            </div>
            <button v-if="question.questionType !== 'TRUE_FALSE' && question.options.length < 8" class="text-link push-top" @click="question.options.push('')">+ 添加选项</button>
          </template>
          <label v-else-if="question.questionType === 'FILL_BLANK'" class="field-label push-top">参考答案<input v-model="question.correctAnswers[0]" class="input" placeholder="用于教师批改参考" /></label>
          <p v-else class="muted push-top">简答题由教师人工评分，可在题干中写明评分要点。</p>
        </article>
        <p v-if="!assignmentForm.questions.length" class="list-empty">尚未添加题目。</p>
      </section>
      <div class="form-grid push-top">
        <div><label class="field-label" for="gw-assign-max">满分</label><input id="gw-assign-max" v-model.number="assignmentForm.maxScore" class="input" type="number" min="1" /></div>
        <div><label class="field-label" for="gw-assign-due">截止时间</label><input id="gw-assign-due" v-model="assignmentForm.dueAt" data-test="assignment-due" class="input" type="datetime-local" /></div>
      </div>
      <div class="form-actions"><AppButton variant="secondary" @click="assignmentForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!assignmentForm.title.trim() || !assignmentForm.dueAt" @click="saveAssignment">{{ assignmentForm.assignmentId ? '保存修改' : '保存作业' }}</AppButton></div>
    </AppModal>
    <AppModal :open="rosterOpen" :title="selectedAssignment ? selectedAssignment.title + ' · 提交情况' : '学生提交情况'" description="按选课名单展示全班学生，可直接区分已提交、未提交和已批改状态。" wide @close="rosterOpen = false">
      <div class="roster-summary">
        <span><strong>{{ roster.length }}</strong> 名学生</span>
        <span class="summary-ok"><strong>{{ submittedCount }}</strong> 已提交</span>
        <span class="summary-warn"><strong>{{ missingCount }}</strong> 未提交</span>
        <span><strong>{{ gradedCount }}</strong> 已批改</span>
      </div>
      <section class="batch-grading-launch">
        <div><span class="ai-batch-chip"><Sparkles :size="14" />AI P1 辅助批改</span><strong>按评分标准批量生成建议</strong><p>低置信度、空答案和异常结果自动进入人工复核；不会批量保存或发布成绩。</p></div>
        <AppButton variant="secondary" :disabled="!batchCandidates.length" @click="openBatchGrading">{{ batchPanelOpen ? '收起批量辅助' : `辅助批改 ${batchCandidates.length} 份` }}</AppButton>
      </section>
      <section v-if="batchPanelOpen" class="batch-grading-panel" aria-labelledby="batch-grading-title">
        <div class="spread wrap"><div><h3 id="batch-grading-title">批量辅助批改设置</h3><p>评分标准越具体，建议越可复核。已发布成绩不会加入本次批次。</p></div><span class="safety-note"><ShieldCheck :size="14" />仅生成草稿</span></div>
        <label class="field-label" for="batch-rubric">评分标准<textarea id="batch-rubric" v-model="batchRubric" class="textarea" rows="3" placeholder="例如：概念准确 40 分，步骤完整 30 分，边界情况 20 分，表达规范 10 分" /></label>
        <div class="batch-settings">
          <label class="field-label" for="batch-review-threshold">人工复核阈值<select id="batch-review-threshold" v-model.number="batchThreshold" class="select"><option :value="0.7">70%</option><option :value="0.75">75%</option><option :value="0.8">80%</option><option :value="0.85">85%</option></select></label>
          <div class="batch-generate"><span>{{ batchCandidates.length }} 份待处理</span><AppButton variant="primary" :loading="batchLoading" :disabled="!batchRubric.trim() || !batchCandidates.length" @click="generateBatchGrading">生成批改建议</AppButton></div>
        </div>
        <AiGenerationProgress :active="batchLoading" label="正在生成批改建议" class="push-top" />
        <p v-if="batchError" class="form-error" role="alert">{{ batchError }}</p>
        <template v-if="batchDraft">
          <div class="batch-summary"><span><strong>{{ batchDraft.totalCount }}</strong> 已分析</span><span class="review"><strong>{{ batchDraft.reviewCount }}</strong> 需人工复核</span><span><strong>{{ batchDraft.totalCount - batchDraft.reviewCount }}</strong> 可优先检查</span></div>
          <div class="batch-results">
            <article v-for="item in batchDraft.items" :key="item.submissionId" class="batch-result" :class="{ review: item.reviewRequired }" data-test="batch-grade-result">
              <div class="spread wrap"><div><strong>{{ batchStudentName(item.submissionId) }}</strong><p>建议 {{ item.suggestedScore ?? '—' }} / {{ item.maxScore }} 分</p></div><StatusBadge :tone="item.reviewRequired ? 'amber' : 'green'" :label="item.reviewRequired ? '人工复核' : '优先检查'" /></div>
              <div class="confidence"><span>置信度 {{ Math.round(item.confidence * 100) }}%</span><i><b :style="{ width: `${Math.round(item.confidence * 100)}%` }" /></i></div>
              <p class="batch-comment">{{ item.comment }}</p>
              <ul v-if="item.reviewReasons.length" class="review-reasons"><li v-for="reason in item.reviewReasons" :key="reason">{{ reason }}</li></ul>
              <button class="text-link" type="button" @click="adoptBatchSuggestion(item)">打开原提交并人工确认</button>
            </article>
          </div>
        </template>
      </section>
      <div class="roster-grid">
        <article v-for="item in roster" :key="item.studentId" class="roster-card" data-test="roster-item">
          <div class="student-avatar" aria-hidden="true">{{ (item.studentName || '学').slice(0, 1) }}</div>
          <div class="roster-main">
            <div class="spread"><div><strong>{{ item.studentName }}</strong><p class="muted">学号 {{ item.studentId }}</p></div><StatusBadge :tone="item.submitted ? 'green' : 'amber'" :label="item.submitted ? '已提交' : '未提交'" /></div>
            <template v-if="item.submission">
              <p class="roster-meta">{{ item.submission.submittedAt ? formatTime(item.submission.submittedAt) : '已保存草稿' }} · {{ item.submission.gradeStatus?.label || '未评分' }}</p>
              <div class="spread"><span>{{ item.submission.score ?? '—' }} / {{ item.submission.maxScore }} 分</span><button class="text-link" @click="open(item.submission)">{{ item.submission.score == null ? '进入批改' : '查看或修改批改' }}</button></div>
            </template>
            <p v-else class="roster-meta">该学生尚未提交本次作业。</p>
          </div>
        </article>
        <p v-if="!roster.length" class="list-empty">当前课程还没有学生选课记录。</p>
      </div>
      <div class="form-actions"><AppButton variant="secondary" @click="rosterOpen = false">关闭</AppButton></div>
    </AppModal>    <AppModal :open="Boolean(current)" title="批改作业" :description="current ? `${current.studentName || current.studentId} · ${current.submissionStatus.label}` : ''" @close="current = null">
      <section v-if="current" class="notice"><div>
        <strong>学生提交内容</strong>
        <p class="pre-line">{{ current.content || (Object.keys(current.answers || {}).length ? '已完成题目作答' : '仅提交了附件') }}</p>
        <div v-if="Object.keys(current.answers || {}).length" class="answer-review">
          <div v-for="(question, index) in assignmentQuestions(current.assignmentId)" :key="question.questionId">
            <strong>{{ index + 1 }}. {{ question.stem }}</strong>
            <p>答：{{ answerText(question, current.answers[question.questionId]) }}</p>
          </div>
        </div>
        <a v-if="current.fileUrl" class="text-link" :href="current.fileUrl" target="_blank" rel="noopener">查看学生附件</a>
      </div></section>
      <label class="field-label push-top" for="gw-grade-score">得分（满分 {{ current?.maxScore }}）</label><input id="gw-grade-score" v-model.number="score" class="input" type="number" min="0" :max="current?.maxScore" />
      <div class="spread" style="margin-top: 16px"><label class="field-label" for="gw-grade-comment" style="margin: 0">教师评语</label><AiAssistButton label="AI 评语草稿" :loading="aiLoading" @click="draftComment" /></div>
      <AiGenerationProgress :active="aiLoading" label="正在生成评语草稿" class="push-top" />
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
import { Plus, ShieldCheck, Sparkles } from 'lucide-vue-next'
import AiAssistButton from '@/components/AiAssistButton.vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'; import AiGenerationProgress from '@/components/AiGenerationProgress.vue'; import AiResultPanel from '@/components/AiResultPanel.vue'
import { aiApi, assignmentsApi, teacherCoursesApi } from '@/services/api'; import type { AssignmentDetailVO, AssignmentQuestion, AssignmentQuestionType, BatchGradingDraftItemVO, BatchGradingDraftVO, TeacherCourseListItemVO, TeacherSubmissionGradeVO, TeacherSubmissionRosterVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
import { aiDraftToResult } from '@/services/aiDraft'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const route = useRoute(); const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const assignments = ref<AssignmentDetailVO[]>([]); const roster = ref<TeacherSubmissionRosterVO[]>([])
const courseId = ref(''); const assignmentId = ref(''); const rosterOpen = ref(false); const current = ref<TeacherSubmissionGradeVO | null>(null); const score = ref<number | null>(null); const comment = ref(''); const message = ref('')
const visibleAssignments = computed(() => assignmentId.value ? assignments.value.filter((item) => item.assignmentId === assignmentId.value) : assignments.value)
const selectedAssignment = computed(() => assignments.value.find((item) => item.assignmentId === assignmentId.value) ?? null)
const submittedCount = computed(() => roster.value.filter((item) => item.submitted).length)
const missingCount = computed(() => roster.value.length - submittedCount.value)
const gradedCount = computed(() => roster.value.filter((item) => item.submission?.gradeStatus?.code === 'PUBLISHED' || item.submission?.submissionStatus.code === 'GRADED').length)
const targetAssignmentId = computed(() => typeof route.query.assignmentId === 'string' ? route.query.assignmentId : '')
const targetCourseId = computed(() => typeof route.query.courseId === 'string' ? route.query.courseId : '')
const formatTime = formatDateTime
const batchPanelOpen = ref(false); const batchRubric = ref(''); const batchThreshold = ref(0.75); const batchLoading = ref(false); const batchError = ref(''); const batchDraft = ref<BatchGradingDraftVO | null>(null)
const batchCandidates = computed(() => roster.value.flatMap((item) => item.submission && item.submission.gradeStatus?.code !== 'PUBLISHED' ? [item.submission] : []))
function openBatchGrading() { batchPanelOpen.value = !batchPanelOpen.value; batchError.value = ''; if (!batchRubric.value) batchRubric.value = selectedAssignment.value?.description?.trim() || '概念准确、步骤完整、边界情况与表达规范；结合满分按完成质量给出建议。' }
function batchStudentName(submissionId: string) { return roster.value.find((item) => item.submission?.submissionId === submissionId)?.studentName || submissionId }

function assignmentTone(item: AssignmentDetailVO) { return item.assignmentStatus.code === 'PUBLISHED' ? 'green' : item.assignmentStatus.code === 'CLOSED' ? 'gray' : 'amber' }
function assignmentLabel(item: AssignmentDetailVO) { return item.assignmentStatus.code === 'DRAFT' && item.source === 'AI' ? 'AI 草稿' : item.assignmentStatus.label }
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }
async function load() { const page = await state.run(() => teacherCoursesApi.listFormal({ page: 1, size: 100 })); if (page) { courses.value = page.records; courseId.value = page.records.some((item) => item.courseId === targetCourseId.value) ? targetCourseId.value : courseId.value || page.records[0]?.courseId || ''; assignmentId.value = targetAssignmentId.value || assignmentId.value; await loadAssignments() } }
async function onCourseChange() { assignmentId.value = ''; roster.value = []; await loadAssignments() }
async function loadAssignments() {
  if (!courseId.value) { assignments.value = []; assignmentId.value = ''; return }
  const page = await state.run(() => assignmentsApi.teacherList(courseId.value, { page: 1, size: 100 }))
  if (!page) return
  assignments.value = page.records
  if (!page.records.some((item) => item.assignmentId === assignmentId.value)) assignmentId.value = ''
  await loadSubmissions()
}
async function loadSubmissions() {
  if (!assignmentId.value) { roster.value = []; return }
  const rows = await state.run(() => assignmentsApi.submissionRoster(assignmentId.value))
  if (rows) roster.value = rows
}
async function viewSubmissions(id: string) {
  assignmentId.value = id
  rosterOpen.value = true
  await loadSubmissions()
}const aiDraft = ref<AiResult | null>(null); const aiLoading = ref(false); const aiError = ref('')
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
const assignmentForm = reactive({ open: false, assignmentId: '', version: 0, title: '', description: '', responseMode: 'MIXED' as AssignmentDetailVO['responseMode'], questions: [] as AssignmentQuestion[], maxScore: 100, dueAt: '' })
function newQuestion(type: AssignmentQuestionType = 'SINGLE_CHOICE'): AssignmentQuestion {
  return { questionId: 'q-' + Date.now() + '-' + Math.random().toString(36).slice(2, 7), questionType: type, stem: '', options: ['', '', '', ''], score: 10, correctAnswers: ['0'] }
}
function openAssignmentForm() { Object.assign(assignmentForm, { open: true, assignmentId: '', version: 0, title: '', description: '', responseMode: 'MIXED', questions: [], maxScore: 100, dueAt: '' }) }
function addQuestion() { assignmentForm.questions.push(newQuestion()) }
function removeQuestion(index: number) { assignmentForm.questions.splice(index, 1) }
function choiceQuestion(type: AssignmentQuestionType) { return ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE'].includes(type) }
async function generateBatchGrading() {
  if (!batchCandidates.value.length || !batchRubric.value.trim()) return
  batchLoading.value = true; batchError.value = ''
  try {
    batchDraft.value = await aiApi.batchGradingDraft({
      submissionIds: batchCandidates.value.map((item) => item.submissionId),
      rubric: batchRubric.value.trim(),
      reviewThreshold: batchThreshold.value,
      instruction: '只生成建议，不保存、不发布成绩；异常答案必须说明复核原因。',
    })
  } catch (caught) { batchError.value = aiErrorMessage(caught) }
  finally { batchLoading.value = false }
}
function adoptBatchSuggestion(item: BatchGradingDraftItemVO) {
  const submission = batchCandidates.value.find((candidate) => candidate.submissionId === item.submissionId)
  if (!submission) return
  open(submission)
  score.value = item.suggestedScore ?? null
  comment.value = item.comment
}

function resetQuestion(question: AssignmentQuestion) {
  if (question.questionType === 'TRUE_FALSE') { question.options = ['正确', '错误']; question.correctAnswers = ['0']; return }
  if (choiceQuestion(question.questionType)) { question.options = ['', '', '', '']; question.correctAnswers = ['0']; return }
  question.options = []
  question.correctAnswers = question.questionType === 'FILL_BLANK' ? [''] : []
}
function toggleCorrect(question: AssignmentQuestion, optionIndex: number) {
  const value = String(optionIndex)
  if (question.questionType !== 'MULTI_CHOICE') { question.correctAnswers = [value]; return }
  question.correctAnswers = question.correctAnswers.includes(value) ? question.correctAnswers.filter((item) => item !== value) : [...question.correctAnswers, value]
}
function removeOption(question: AssignmentQuestion, optionIndex: number) {
  question.options.splice(optionIndex, 1)
  question.correctAnswers = question.correctAnswers.filter((item) => Number(item) !== optionIndex).map((item) => String(Number(item) > optionIndex ? Number(item) - 1 : Number(item)))
}
function assignmentQuestions(id: string) { return assignments.value.find((item) => item.assignmentId === id)?.questions ?? [] }
function answerText(question: AssignmentQuestion, answer: string[] | undefined) {
  if (!answer?.length) return '未作答'
  return choiceQuestion(question.questionType) ? answer.map((item) => question.options[Number(item)] ?? item).join('、') : answer.join('；')
}
function openEditAssignment(item: AssignmentDetailVO) {
  Object.assign(assignmentForm, {
    open: true, assignmentId: item.assignmentId, version: item.version,
    title: item.title, description: item.description ?? '', responseMode: item.responseMode === 'CODE' ? 'MIXED' : (item.responseMode ?? 'MIXED'),
    questions: (item.questions ?? []).map((question) => ({ ...question, options: [...question.options], correctAnswers: [...question.correctAnswers] })),
    maxScore: item.maxScore, dueAt: item.dueAt ? item.dueAt.slice(0, 16) : '',
  })
}
async function saveAssignment() {
  const body = {
    title: assignmentForm.title.trim(), description: assignmentForm.description.trim() || null,
    responseMode: assignmentForm.responseMode,
    questions: assignmentForm.responseMode === 'QUIZ' ? assignmentForm.questions : [],
    maxScore: assignmentForm.maxScore, dueAt: new Date(assignmentForm.dueAt).toISOString(),
  }
  const saved = await state.run(() => assignmentForm.assignmentId
    ? assignmentsApi.update(assignmentForm.assignmentId, { ...body, version: assignmentForm.version })
    : assignmentsApi.create(courseId.value, body))
  if (saved) {
    const editing = Boolean(assignmentForm.assignmentId)
    assignmentForm.open = false
    if (!editing) assignmentId.value = saved.assignmentId
    flash(editing ? '作业已更新' : '作业已创建（草稿）')
    await loadAssignments()
  }
}
async function assignmentAction(id: string, action: 'publish' | 'close') {
  const result = await state.run(() => action === 'publish' ? assignmentsApi.publish(id) : assignmentsApi.close(id))
  if (result) { flash(action === 'publish' ? '作业已发布' : '作业已截止'); await loadAssignments() }
}
async function grade(publishNow: boolean) { if (!current.value || score.value == null) return; const item = current.value; const graded = await state.run(() => assignmentsApi.grade(item.submissionId, { score: score.value!, maxScore: item.maxScore, teacherComment: comment.value, publishNow, version: item.version })); if (graded) { current.value = null; flash(publishNow ? '成绩已发布' : '评分已保存'); await loadSubmissions() } }
onMounted(load)
watch([targetAssignmentId, targetCourseId], () => { void load() })
</script>

<style scoped>
.quiz-builder { padding: 16px; border: 1px solid var(--line); background: #f8fafc; }
.quiz-builder .muted { margin: 4px 0 0; font-size: 12px; }
.question-editor { margin-top: 14px; padding: 14px; border: 1px solid var(--line); background: #fff; }
.option-editor { display: grid; grid-template-columns: 20px 22px minmax(0, 1fr) auto; gap: 8px; align-items: center; margin-top: 8px; }
.answer-review { display: grid; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--line); }
.answer-review p { margin: 4px 0 0; }

.roster-summary { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; }
.roster-summary span { min-width: 118px; padding: 12px 14px; border: 1px solid var(--line); background: #f8fafc; }
.roster-summary strong { margin-right: 4px; font-size: 20px; color: var(--ink); }
.roster-summary .summary-ok { background: #ecfdf5; border-color: #bbf7d0; }
.roster-summary .summary-warn { background: #fff7ed; border-color: #fed7aa; }
.roster-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; max-height: min(58vh, 640px); overflow: auto; padding-right: 4px; }
.roster-card { display: grid; grid-template-columns: 44px minmax(0, 1fr); gap: 12px; padding: 16px; border: 1px solid var(--line); background: #fff; }
.student-avatar { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 50%; background: #e7f0ff; color: var(--primary); font-weight: 800; }
.roster-main { min-width: 0; }
.roster-main p { margin: 4px 0 0; }
.roster-meta { margin: 12px 0 !important; color: var(--muted); font-size: 13px; }
@media (max-width: 760px) { .roster-grid { grid-template-columns: 1fr; } }
.batch-grading-launch { display: flex; justify-content: space-between; align-items: center; gap: 18px; margin-bottom: 16px; padding: 16px; border: 1px solid #cfe0f5; background: linear-gradient(120deg,#f0f7ff,#fff 62%,#f5f3ff); }
.batch-grading-launch > div { display: grid; gap: 5px; }
.batch-grading-launch strong { color: var(--ink); }
.batch-grading-launch p, .batch-grading-panel p { margin: 0; color: var(--muted); font-size: 12px; }
.ai-batch-chip { display: inline-flex; align-items: center; gap: 5px; width: fit-content; color: #2458bd; font-size: 11px; font-weight: 800; }
.batch-grading-panel { display: grid; gap: 14px; margin-bottom: 18px; padding: 18px; border: 1px solid #cbd9ed; background: #f8fbff; }
.batch-grading-panel h3 { margin: 0 0 4px; color: var(--ink); font-size: 16px; }
.safety-note { display: inline-flex; align-items: center; gap: 5px; color: #15704f; font-size: 12px; font-weight: 750; }
.batch-settings { display: grid; grid-template-columns: minmax(160px,.35fr) 1fr; gap: 14px; align-items: end; }
.batch-generate { display: flex; justify-content: flex-end; align-items: center; gap: 12px; color: var(--muted); font-size: 12px; }
.batch-summary { display: flex; flex-wrap: wrap; gap: 10px; }.batch-summary span { padding: 8px 10px; border: 1px solid var(--line); background: #fff; }.batch-summary strong { color: var(--ink); }.batch-summary .review { color: #9a5c00; background: #fff8e7; border-color: #f4d795; }
.batch-results { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 10px; max-height: 420px; overflow: auto; }
.batch-result { display: grid; gap: 10px; padding: 14px; border: 1px solid #cfe3d9; background: #fff; }.batch-result.review { border-color: #f1cf86; background: #fffdfa; }.batch-result p { margin: 3px 0 0; }.batch-comment { line-height: 1.55; }
.confidence { display: grid; gap: 5px; color: var(--muted); font-size: 11px; }.confidence i { height: 5px; overflow: hidden; border-radius: 99px; background: #e2e8f0; }.confidence b { display: block; height: 100%; border-radius: inherit; background: #3974dd; }.review-reasons { margin: 0; padding-left: 17px; color: #946200; font-size: 11px; line-height: 1.55; }
@media (max-width:760px) { .batch-grading-launch { align-items: stretch; flex-direction: column; }.batch-settings,.batch-results { grid-template-columns:1fr; }.batch-generate { justify-content:space-between; } }
</style>
