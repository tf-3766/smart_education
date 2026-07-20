<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">考试题库</h1><p class="page-subtitle">维护题库与题目，创建考试并组卷发布，交卷后在此阅卷。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <div class="filter-bar"><label class="filter-field"><span>课程</span><select v-model="courseId" class="select" @change="loadResources"><option v-for="course in courses" :key="course.courseId" :value="course.courseId">{{ course.name }}</option></select></label></div>

    <div class="grid cols-2">
      <section class="panel flush">
        <div class="panel-head"><h2>题库</h2><AppButton variant="secondary" @click="bankForm.open = true"><span class="row"><Plus :size="14" />新建题库</span></AppButton></div>
        <div class="table-scroll"><table class="table">
          <thead><tr><th>题库名称</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="bank in banks" :key="bank.bankId" :class="{ 'row-active': bank.bankId === selectedBankId }" style="cursor: pointer" @click="selectBank(bank.bankId)">
              <td class="cell-strong">{{ bank.name }}</td><td><StatusBadge :tone="bank.status === 'ACTIVE' ? 'green' : 'gray'" :label="bank.status === 'ACTIVE' ? '启用' : '归档'" /></td>
            </tr>
            <tr v-if="!banks.length"><td colspan="2" class="list-empty">暂无题库</td></tr>
          </tbody>
        </table></div>
      </section>
      <section class="panel flush">
        <div class="panel-head"><h2>考试安排</h2><AppButton variant="secondary" @click="openExamForm"><span class="row"><Plus :size="14" />新建考试</span></AppButton></div>
        <div class="table-scroll"><table class="table">
          <thead><tr><th>考试名称</th><th>窗口</th><th class="num">总分</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="exam in exams" :key="exam.examId">
              <td class="cell-strong">{{ exam.title }}</td><td>{{ formatTime(exam.startAt) }}</td><td class="num">{{ exam.totalScore }}</td>
              <td><StatusBadge :tone="exam.status === 'PUBLISHED' ? 'green' : 'amber'" :label="exam.status" /></td>
              <td class="cell-actions">
                <button v-if="exam.status !== 'PUBLISHED'" class="text-link" @click="openPaperForm(exam)">组卷</button>
                <button v-if="draftPapers[exam.examId]" class="text-link" @click="publishPaper(exam.examId)">发布试卷</button>
                <button class="text-link" @click="openAttempts(exam)">答卷</button>
              </td>
            </tr>
            <tr v-if="!exams.length"><td colspan="5" class="list-empty">暂无考试安排</td></tr>
          </tbody>
        </table></div>
      </section>
    </div>

    <section v-if="selectedBank" class="panel flush">
      <div class="panel-head"><h2>题目 · {{ selectedBank.name }}</h2><AppButton variant="secondary" @click="openQuestionForm()"><span class="row"><Plus :size="14" />新建题目</span></AppButton></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>题干</th><th>题型</th><th>难度</th><th class="num">分值</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="question in questions" :key="question.questionId">
            <td class="cell-strong">{{ question.stem }}</td><td>{{ typeLabel(question.questionType) }}</td><td>{{ difficultyLabel(question.difficulty) }}</td><td class="num">{{ question.score }}</td>
            <td class="cell-actions"><button class="text-link" @click="openQuestionForm(question)">编辑</button><button class="text-link" @click="removeQuestion(question)">删除</button></td>
          </tr>
          <tr v-if="!questions.length"><td colspan="5" class="list-empty">题库暂无题目——考试内容来自题目，先为该题库添加题目，再到「考试安排」组卷。<button class="text-link" style="margin-left: 8px" @click="openQuestionForm()">+ 新建题目</button></td></tr>
        </tbody>
      </table></div>
    </section>

    <section v-if="attemptExam" class="panel flush">
      <div class="panel-head"><h2>答卷 · {{ attemptExam.title }}</h2><span class="count">共 {{ attempts.length }} 份</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>学生</th><th>状态</th><th>交卷时间</th><th class="num">得分</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="attempt in attempts" :key="attempt.attemptId">
            <td class="cell-strong">{{ studentName(attempt.studentId) }}</td>
            <td><StatusBadge :tone="attempt.status === 'GRADED' ? 'green' : attempt.status === 'SUBMITTED' ? 'blue' : 'gray'" :label="attempt.status === 'GRADED' ? '已评分' : attempt.status === 'SUBMITTED' ? '待评分' : '答题中'" /></td>
            <td>{{ attempt.submittedAt ? formatTime(attempt.submittedAt) : '—' }}</td><td class="num">{{ attempt.score ?? '—' }}</td>
            <td class="cell-actions"><button v-if="attempt.status === 'SUBMITTED'" class="text-link" @click="openGrading(attempt)">阅卷</button></td>
          </tr>
          <tr v-if="!attempts.length"><td colspan="5" class="list-empty">暂无答卷。</td></tr>
        </tbody>
      </table></div>
    </section>

    <AppModal :open="bankForm.open" title="新建题库" @close="bankForm.open = false">
      <label class="field-label" for="qb-bank-name">题库名称</label><input id="qb-bank-name" v-model="bankForm.name" data-test="bank-name" class="input" placeholder="题库名称" />
      <label class="field-label push-top" for="qb-bank-desc">描述</label><textarea id="qb-bank-desc" v-model="bankForm.description" class="textarea" placeholder="用途说明（可选）" />
      <div class="form-actions"><AppButton variant="secondary" @click="bankForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!bankForm.name.trim()" @click="saveBank">保存题库</AppButton></div>
    </AppModal>

    <AppModal :open="questionForm.open" :title="questionForm.questionId ? '编辑题目' : '新建题目'" @close="questionForm.open = false">
      <div class="form-grid">
        <div><label class="field-label" for="qb-q-type">题型</label><select id="qb-q-type" v-model="questionForm.questionType" class="select" @change="onTypeChange"><option value="SINGLE_CHOICE">单选题</option><option value="MULTIPLE_CHOICE">多选题</option><option value="TRUE_FALSE">判断题</option><option value="FILL_BLANK">填空题</option><option value="SHORT_ANSWER">简答题</option></select></div>
        <div><label class="field-label" for="qb-q-difficulty">难度</label><select id="qb-q-difficulty" v-model="questionForm.difficulty" class="select"><option value="EASY">易</option><option value="MEDIUM">中</option><option value="HARD">难</option></select></div>
      </div>
      <label class="field-label push-top" for="qb-q-stem">题干</label><textarea id="qb-q-stem" v-model="questionForm.stem" data-test="question-stem" class="textarea" placeholder="题目内容" />
      <label class="field-label push-top" for="qb-q-score">分值</label><input id="qb-q-score" v-model.number="questionForm.score" class="input" type="number" min="1" />
      <template v-if="!manualQuestionType(questionForm.questionType)">
        <p class="field-label push-top">选项（勾选正确项{{ questionForm.questionType === 'MULTIPLE_CHOICE' ? '，可多个' : '，恰一个' }}）</p>
        <div v-for="(option, index) in questionForm.options" :key="index" class="row push-top" style="gap: 8px; align-items: center">
          <strong>{{ letter(index) }}</strong>
          <input v-model="option.content" data-test="option-content" class="input" style="flex: 1" placeholder="选项内容" />
          <label class="row" style="gap: 4px"><input v-model="option.correct" data-test="option-correct" type="checkbox" />正确</label>
          <button v-if="questionForm.questionType !== 'TRUE_FALSE'" class="text-link" @click="questionForm.options.splice(index, 1)">删除</button>
        </div>
        <button v-if="questionForm.questionType !== 'TRUE_FALSE' && questionForm.options.length < 6" class="text-link push-top" @click="questionForm.options.push({ content: '', correct: false })">+ 添加选项</button>
      </template>
      <div class="form-actions"><AppButton variant="secondary" @click="questionForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!questionValid" @click="saveQuestion">保存题目</AppButton></div>
    </AppModal>

    <AppModal :open="examForm.open" title="新建考试" description="这里只定考试外壳（时间与总分）；具体考题在创建后点「组卷」从题库选题配分，发布试卷后学生才能作答。" @close="examForm.open = false">
      <label class="field-label" for="qb-exam-title">考试标题</label><input id="qb-exam-title" v-model="examForm.title" data-test="exam-title" class="input" placeholder="考试标题" />
      <div class="form-grid push-top">
        <div><label class="field-label" for="qb-exam-start">开始时间</label><input id="qb-exam-start" v-model="examForm.startAt" data-test="exam-start" class="input" type="datetime-local" /></div>
        <div><label class="field-label" for="qb-exam-end">结束时间</label><input id="qb-exam-end" v-model="examForm.endAt" data-test="exam-end" class="input" type="datetime-local" /></div>
      </div>
      <div class="form-grid push-top">
        <div><label class="field-label" for="qb-exam-duration">时长（分钟）</label><input id="qb-exam-duration" v-model.number="examForm.durationMinutes" class="input" type="number" min="1" /></div>
        <div><label class="field-label" for="qb-exam-total">总分</label><input id="qb-exam-total" v-model.number="examForm.totalScore" data-test="exam-total" class="input" type="number" min="1" /></div>
      </div>
      <div class="form-actions"><AppButton variant="secondary" @click="examForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!examForm.title.trim() || !examForm.startAt || !examForm.endAt" @click="saveExam">保存考试</AppButton></div>
    </AppModal>

    <AppModal :open="paperForm.open" :title="`组卷 · ${paperForm.examTitle}`" :description="`勾选题目并核对分值，合计须等于考试总分 ${paperForm.totalScore} 分。`" @close="paperForm.open = false; aiPaper = null">
      <div class="table-scroll"><table class="table">
        <thead><tr><th></th><th>题干</th><th>题型</th><th class="num">分值</th></tr></thead>
        <tbody>
          <tr v-for="item in paperForm.pool" :key="item.questionId">
            <td><input v-model="item.picked" data-test="paper-pick" type="checkbox" /></td>
            <td>{{ item.stem }}</td><td>{{ typeLabel(item.questionType) }}</td>
            <td class="num"><input v-model.number="item.score" class="input" type="number" min="1" style="width: 72px" /></td>
          </tr>
        </tbody>
      </table></div>
      <div class="spread push-top"><span class="muted">已选 {{ pickedQuestions.length }} 题，合计 {{ pickedScore }} / {{ paperForm.totalScore }} 分</span><AiAssistButton label="AI 组卷建议" :loading="aiPaperLoading" @click="draftPaperSuggestion" /></div>
      <p v-if="aiPaperError" class="form-error" role="alert">{{ aiPaperError }}</p>
      <AiResultPanel v-if="aiPaper" :result="aiPaper" class="push-top" @regenerate="draftPaperSuggestion" />
      <div class="form-actions"><AppButton variant="secondary" @click="paperForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!pickedQuestions.length || pickedScore !== paperForm.totalScore" @click="savePaper">保存试卷</AppButton></div>
    </AppModal>

    <AppModal :open="Boolean(grading)" title="阅卷" :description="grading ? `${studentName(grading.studentId)} · 客观题已自动判分` : ''" @close="grading = null">
      <template v-if="grading">
        <div v-for="question in grading.questions" :key="question.questionId" class="notice push-top"><div>
          <strong>{{ question.questionOrder }}. {{ question.stem }}（{{ question.score }} 分）</strong>
          <p class="pre-line">答：{{ answerOf(question.questionId)?.answerContent || '未作答' }}</p>
          <p v-if="!manualQuestionType(question.questionType)" class="muted">系统判分：{{ answerOf(question.questionId)?.score ?? 0 }} 分</p>
          <template v-else>
            <label class="row" style="gap: 8px">人工评分 <input v-model.number="manualScores[question.questionId]" data-test="grade-score" class="input" type="number" min="0" :max="question.score" style="width: 90px" /></label>
            <label class="field-label push-top" :for="`qb-grade-comment-${question.questionId}`">评语（可选，随成绩展示给学生）</label>
            <textarea :id="`qb-grade-comment-${question.questionId}`" v-model="manualComments[question.questionId]" data-test="grade-comment" class="textarea" rows="2" placeholder="针对该题作答的点评" />
          </template>
        </div></div>
        <div class="form-actions"><AppButton variant="secondary" @click="grading = null">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" @click="submitGrading">提交评分</AppButton></div>
      </template>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'; import AiResultPanel from '@/components/AiResultPanel.vue'; import AiAssistButton from '@/components/AiAssistButton.vue'
import { aiApi, examsApi, teacherCoursesApi } from '@/services/api'; import type { ExamAttemptVO, ExamVO, QuestionBankVO, QuestionVO, TeacherCourseListItemVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
import { aiDraftToResult } from '@/services/aiDraft'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const state = usePageState(); const courses = ref<TeacherCourseListItemVO[]>([]); const courseId = ref(''); const banks = ref<QuestionBankVO[]>([]); const exams = ref<ExamVO[]>([])
const selectedBankId = ref(''); const questions = ref<QuestionVO[]>([])
const attemptExam = ref<ExamVO | null>(null); const attempts = ref<ExamAttemptVO[]>([])
const grading = ref<ExamAttemptVO | null>(null); const manualScores = reactive<Record<string, number>>({}); const manualComments = reactive<Record<string, string>>({})
const draftPapers = reactive<Record<string, string>>({})
const message = ref('')
const selectedBank = computed(() => banks.value.find((bank) => bank.bankId === selectedBankId.value) ?? null)

const bankForm = reactive({ open: false, name: '', description: '' })
const questionForm = reactive({ open: false, questionId: '', questionType: 'SINGLE_CHOICE', stem: '', difficulty: 'EASY', score: 5, version: 0, status: '', options: [] as { content: string; correct: boolean }[] })
const examForm = reactive({ open: false, title: '', startAt: '', endAt: '', durationMinutes: 60, totalScore: 100 })
const paperForm = reactive({ open: false, examId: '', examTitle: '', totalScore: 0, pool: [] as { questionId: string; stem: string; questionType: string; score: number; picked: boolean }[] })

const formatTime = formatDateTime
const letter = (index: number) => String.fromCharCode(65 + index)
const typeLabel = (code: string) => ({ SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', TRUE_FALSE: '判断题', FILL_BLANK: '填空题', SHORT_ANSWER: '简答题' }[code] ?? code)
const difficultyLabel = (code: string) => ({ EASY: '易', MEDIUM: '中', HARD: '难' }[code] ?? code)
const manualQuestionType = (type: string) => ['FILL_BLANK', 'SHORT_ANSWER'].includes(type)
const studentName = (id: string) => ({ '4': '王一诺', '5': '刘子涵', '6': '赵晨' }[id] ?? id)
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

const pickedQuestions = computed(() => paperForm.pool.filter((item) => item.picked))
const pickedScore = computed(() => pickedQuestions.value.reduce((sum, item) => sum + (item.score || 0), 0))
const aiPaper = ref<AiResult | null>(null); const aiPaperLoading = ref(false); const aiPaperError = ref('')
async function draftPaperSuggestion() {
  aiPaperLoading.value = true; aiPaperError.value = ''
  try {
    const draft = await aiApi.paperSuggestion({ courseId: courseId.value, questionCount: Math.max(paperForm.pool.length, 1), totalScore: paperForm.totalScore || 1, requirements: null })
    aiPaper.value = aiDraftToResult(draft, 'paper', 'AI 组卷建议')
  } catch (caught) {
    aiPaperError.value = aiErrorMessage(caught)
  } finally { aiPaperLoading.value = false }
}
const questionValid = computed(() => {
  if (!questionForm.stem.trim() || questionForm.score < 1) return false
  if (manualQuestionType(questionForm.questionType)) return true
  const options = questionForm.options
  if (options.length < 2 || options.some((option) => !option.content.trim())) return false
  const correct = options.filter((option) => option.correct).length
  return questionForm.questionType === 'MULTIPLE_CHOICE' ? correct >= 2 : correct === 1
})

async function load() { const page = await state.run(() => teacherCoursesApi.list({ page: 1, size: 100 })); if (page) { courses.value = page.records; courseId.value ||= page.records[0]?.courseId ?? ''; await loadResources() } }
async function loadResources() {
  if (!courseId.value) return
  selectedBankId.value = ''; questions.value = []; attemptExam.value = null; attempts.value = []
  const result = await state.run(() => Promise.all([examsApi.listBanks(courseId.value, { page: 1, size: 100 }), examsApi.listExams(courseId.value, { page: 1, size: 100 })]))
  if (result) { banks.value = result[0].records; exams.value = result[1].records }
}
async function selectBank(bankId: string) {
  selectedBankId.value = bankId
  const page = await state.run(() => examsApi.listQuestions(bankId, { page: 1, size: 100 }))
  if (page) questions.value = page.records
}
async function saveBank() {
  const created = await state.run(() => examsApi.createBank(courseId.value, { name: bankForm.name.trim(), description: bankForm.description.trim() || null }))
  if (created) { bankForm.open = false; bankForm.name = ''; bankForm.description = ''; flash('题库已创建，请为其添加题目'); await loadResourcesKeepSelection(); await selectBank(created.bankId) }
}
async function loadResourcesKeepSelection() {
  const keepBank = selectedBankId.value; const keepExam = attemptExam.value?.examId
  const result = await state.run(() => Promise.all([examsApi.listBanks(courseId.value, { page: 1, size: 100 }), examsApi.listExams(courseId.value, { page: 1, size: 100 })]))
  if (result) {
    banks.value = result[0].records; exams.value = result[1].records
    if (keepBank && banks.value.some((bank) => bank.bankId === keepBank)) await selectBank(keepBank)
    if (keepExam) attemptExam.value = exams.value.find((exam) => exam.examId === keepExam) ?? null
  }
}

function defaultOptions(type: string) {
  if (type === 'TRUE_FALSE') return [{ content: '正确', correct: false }, { content: '错误', correct: false }]
  return [{ content: '', correct: false }, { content: '', correct: false }, { content: '', correct: false }]
}
function onTypeChange() { questionForm.options = defaultOptions(questionForm.questionType) }
function openQuestionForm(question?: QuestionVO) {
  Object.assign(questionForm, question
    ? { open: true, questionId: question.questionId, questionType: question.questionType, stem: question.stem, difficulty: question.difficulty, score: question.score, version: question.version, status: question.status, options: question.options.map((option) => ({ content: option.content, correct: option.correct })) }
    : { open: true, questionId: '', questionType: 'SINGLE_CHOICE', stem: '', difficulty: 'EASY', score: 5, version: 0, status: '', options: defaultOptions('SINGLE_CHOICE') })
}
async function saveQuestion() {
  const body = {
    questionType: questionForm.questionType, stem: questionForm.stem.trim(), difficulty: questionForm.difficulty, score: questionForm.score,
    options: manualQuestionType(questionForm.questionType) ? [] : questionForm.options.map((option, index) => ({ label: letter(index), content: option.content.trim(), correct: option.correct, sortOrder: (index + 1) * 10 })),
  }
  const saved = await state.run(() => questionForm.questionId
    ? examsApi.updateQuestion(questionForm.questionId, { ...body, status: questionForm.status || null, version: questionForm.version })
    : examsApi.createQuestion(selectedBankId.value, body))
  if (saved) { questionForm.open = false; flash('题目已保存'); await selectBank(selectedBankId.value) }
}
async function removeQuestion(question: QuestionVO) {
  if (!window.confirm(`删除题目「${question.stem.slice(0, 20)}…」？`)) return
  const done = await state.run(async () => { await examsApi.deleteQuestion(question.questionId); return true })
  if (done) { flash('题目已删除'); await selectBank(selectedBankId.value) }
}

function openExamForm() { Object.assign(examForm, { open: true, title: '', startAt: '', endAt: '', durationMinutes: 60, totalScore: 100 }) }
async function saveExam() {
  const created = await state.run(() => examsApi.createExam(courseId.value, {
    title: examForm.title.trim(), startAt: new Date(examForm.startAt).toISOString(), endAt: new Date(examForm.endAt).toISOString(),
    durationMinutes: examForm.durationMinutes, totalScore: examForm.totalScore,
  }))
  if (created) { examForm.open = false; flash('考试已创建（草稿）'); await loadResourcesKeepSelection() }
}
async function openPaperForm(exam: ExamVO) {
  const pages = await state.run(async () => {
    const bankPage = await examsApi.listBanks(courseId.value, { page: 1, size: 100 })
    const questionPages = await Promise.all(bankPage.records.map((bank) => examsApi.listQuestions(bank.bankId, { page: 1, size: 100 })))
    return questionPages.flatMap((page) => page.records)
  })
  if (!pages) return
  Object.assign(paperForm, {
    open: true, examId: exam.examId, examTitle: exam.title, totalScore: exam.totalScore,
    pool: pages.map((question) => ({ questionId: question.questionId, stem: question.stem, questionType: question.questionType, score: question.score, picked: false })),
  })
}
async function savePaper() {
  const paper = await state.run(() => examsApi.createPaper(paperForm.examId, {
    title: `${paperForm.examTitle} 试卷`,
    questions: pickedQuestions.value.map((item, index) => ({ questionId: item.questionId, questionOrder: index + 1, score: item.score })),
  }))
  if (paper) { draftPapers[paperForm.examId] = paper.paperId; paperForm.open = false; flash('试卷已保存，可发布') }
}
async function publishPaper(examId: string) {
  const published = await state.run(() => examsApi.publishPaper(draftPapers[examId]))
  if (published) { delete draftPapers[examId]; flash('试卷已发布，考试对学生可见'); await loadResourcesKeepSelection() }
}

async function openAttempts(exam: ExamVO) {
  attemptExam.value = exam
  const page = await state.run(() => examsApi.listAttempts(exam.examId, { page: 1, size: 100 }))
  if (page) attempts.value = page.records
}
function openGrading(attempt: ExamAttemptVO) {
  grading.value = attempt
  for (const question of attempt.questions) {
    if (manualQuestionType(question.questionType)) {
      const answer = attempt.answers.find((item) => item.questionId === question.questionId)
      manualScores[question.questionId] = answer?.score ?? 0
      manualComments[question.questionId] = answer?.teacherComment ?? ''
    }
  }
}
const answerOf = (questionId: string) => grading.value?.answers.find((answer) => answer.questionId === questionId)
async function submitGrading() {
  if (!grading.value) return
  const target = grading.value
  const graded = await state.run(() => examsApi.gradeAttempt(target.attemptId, {
    answers: target.questions.filter((question) => manualQuestionType(question.questionType)).map((question) => ({ questionId: question.questionId, score: manualScores[question.questionId] ?? 0, teacherComment: manualComments[question.questionId]?.trim() || null })),
    version: target.version,
  }))
  if (graded) { grading.value = null; flash('评分已提交'); await openAttempts(attemptExam.value!) }
}

onMounted(load)
</script>
