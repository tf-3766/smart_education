<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">学习任务</h1><p class="page-subtitle">查看作业要求，在线填写文字或代码，也可以上传本地文件后提交。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :empty="!items.length" empty-text="暂无学习任务" :retry="load" />
    <div class="assignment-list">
      <section v-for="item in items" :key="item.assignment.assignmentId" class="panel assignment-card" :class="{ 'notification-resource-target': item.assignment.assignmentId === selectedAssignmentId }">
        <div class="assignment-summary">
          <div><div class="row wrap"><h2 class="panel-title">{{ item.assignment.title }}</h2><StatusBadge :tone="tone(item)" :label="label(item)" /></div><p class="muted">{{ courseName(item.assignment.courseId) }} · 截止 {{ formatTime(item.assignment.dueAt) }} · 满分 {{ item.assignment.maxScore }}</p></div>
          <AppButton variant="secondary" @click="toggle(item.assignment.assignmentId)">{{ expanded[item.assignment.assignmentId] ? '收起详情' : '查看作业' }}</AppButton>
        </div>
        <div v-if="item.submission?.score != null" class="notice feedback-card"><div><div class="spread"><strong>教师反馈</strong><StatusBadge tone="green" :label="`${item.submission.score} 分`" /></div><p>{{ item.submission.teacherComment || '教师暂未填写评语' }}</p></div></div>
        <div v-if="expanded[item.assignment.assignmentId]" class="assignment-detail">
          <section class="requirements"><p class="eyebrow">作业要求</p><p class="pre-line">{{ item.assignment.description || '教师未填写补充要求。' }}</p><div v-if="item.assignment.attachments.length" class="attachment-list"><a v-for="attachment in item.assignment.attachments" :key="attachment.attachmentId" :href="attachmentUrl(attachment)" target="_blank" rel="noreferrer"><Paperclip :size="14" />{{ attachment.name }}</a></div></section>
          <section v-if="locked(item)" class="submitted-answer">
            <p class="eyebrow">我的提交</p>
            <pre v-if="item.submission?.content" class="answer-content">{{ item.submission.content }}</pre>
            <div v-if="Object.keys(item.submission?.answers || {}).length" class="submitted-quiz">
              <div v-for="(question, index) in item.assignment.questions || []" :key="question.questionId">
                <strong>{{ index + 1 }}. {{ question.stem }}</strong>
                <p>答：{{ answerText(question, item.submission?.answers[question.questionId]) }}</p>
              </div>
            </div>
            <p v-if="!item.submission?.content && !Object.keys(item.submission?.answers || {}).length" class="muted">本次提交未填写在线内容。</p>
            <a v-if="submissionFileUrl(item)" class="file-chip" :href="submissionFileUrl(item)" target="_blank" rel="noreferrer"><FileDown :size="15" />查看已提交文件</a>
            <p class="muted answer-note">作业提交后仍可查看；如需修改已提交内容，请联系授课教师退回。</p>
          </section>
          <section v-else class="answer-editor">
            <template v-if="item.assignment.responseMode === 'QUIZ'">
              <p class="eyebrow">题目作答</p>
              <article v-for="(question, index) in item.assignment.questions || []" :key="question.questionId" class="student-question">
                <strong>{{ index + 1 }}. {{ question.stem }}（{{ question.score }} 分）</strong>
                <div v-if="choiceQuestion(question.questionType)" class="choice-list">
                  <label v-for="(option, optionIndex) in question.options" :key="optionIndex">
                    <input
                      :type="question.questionType === 'MULTI_CHOICE' ? 'checkbox' : 'radio'"
                      :name="'student-question-' + item.assignment.assignmentId + '-' + question.questionId"
                      :checked="(answers[item.assignment.assignmentId]?.[question.questionId] || []).includes(String(optionIndex))"
                      @change="setChoiceAnswer(item.assignment.assignmentId, question, optionIndex)"
                    />
                    <span>{{ String.fromCharCode(65 + optionIndex) }}. {{ option }}</span>
                  </label>
                </div>
                <textarea
                  v-else-if="question.questionType === 'SHORT_ANSWER'"
                  :value="answers[item.assignment.assignmentId]?.[question.questionId]?.[0] || ''"
                  class="textarea push-top"
                  rows="4"
                  placeholder="填写简答内容"
                  @input="setTextAnswer(item.assignment.assignmentId, question.questionId, $event)"
                />
                <input
                  v-else
                  :value="answers[item.assignment.assignmentId]?.[question.questionId]?.[0] || ''"
                  class="input push-top"
                  placeholder="填写答案"
                  @input="setTextAnswer(item.assignment.assignmentId, question.questionId, $event)"
                />
              </article>
            </template>
            <template v-else>
              <label class="field-label" :for="'answer-' + item.assignment.assignmentId">{{ item.assignment.responseMode === 'CODE' ? '在线代码' : '在线回答' }}</label>
              <textarea :id="'answer-' + item.assignment.assignmentId" v-model="drafts[item.assignment.assignmentId]" class="textarea code-answer" rows="9" :placeholder="item.assignment.responseMode === 'CODE' ? '在此粘贴代码；也可上传本地代码文件。' : '在此填写作业回答。'" />
              <label v-if="item.assignment.responseMode === 'MIXED' || item.assignment.responseMode === 'CODE'" class="upload-box">
                <UploadCloud :size="20" /><span><strong>{{ uploadNames[item.assignment.assignmentId] || '选择本地文件' }}</strong><small>支持代码、PDF、Word、图片、压缩包等作业文件</small></span>
                <input type="file" @change="pickFile(item, $event)" />
              </label>
            </template>
            <div class="form-actions">
              <AppButton variant="secondary" :loading="uploading[item.assignment.assignmentId]" @click="save(item)">保存草稿</AppButton>
              <AppButton variant="primary" :disabled="!canSubmit(item) || uploading[item.assignment.assignmentId]" @click="submit(item)">确认提交</AppButton>
            </div>
          </section>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FileDown, Paperclip, UploadCloud } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { assignmentsApi, filesApi, studentLearningApi } from '@/services/api'
import type { AssignmentAttachmentVO, AssignmentQuestion, AssignmentQuestionType, StudentAssignmentDetailVO, StudentCourseListItemVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const route = useRoute()
const items = ref<StudentAssignmentDetailVO[]>([])
const courses = ref<StudentCourseListItemVO[]>([])
const drafts = reactive<Record<string, string>>({})
const answers = reactive<Record<string, Record<string, string[]>>>({})
const fileIds = reactive<Record<string, string>>({})
const uploadNames = reactive<Record<string, string>>({})
const uploading = reactive<Record<string, boolean>>({})
const expanded = reactive<Record<string, boolean>>({})
const message = ref('')
const selectedAssignmentId = computed(() => typeof route.query.assignmentId === 'string' ? route.query.assignmentId : '')
const courseName = (id: string) => courses.value.find((item) => item.courseId === id)?.name ?? id
const formatTime = formatDateTime
const locked = (item: StudentAssignmentDetailVO) => ['SUBMITTED', 'GRADED'].includes(item.submission?.submissionStatus.code ?? '')
const label = (item: StudentAssignmentDetailVO) => item.submission?.submissionStatus.label ?? item.assignment.availabilityStatus.label
const tone = (item: StudentAssignmentDetailVO): 'green' | 'blue' | 'gray' => item.submission?.score != null ? 'green' : locked(item) ? 'blue' : 'gray'
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2400) }
function toggle(id: string) { expanded[id] = !expanded[id] }
function attachmentUrl(attachment: AssignmentAttachmentVO) { return attachment.fileId ? filesApi.contentUrl(attachment.fileId) : attachment.fileUrl || '#' }
function submissionFileUrl(item: StudentAssignmentDetailVO) { return item.submission?.fileId ? filesApi.contentUrl(item.submission.fileId) : item.submission?.fileUrl || '' }
async function load() {
  const result = await state.run(async () => {
    const coursePage = await studentLearningApi.myCourses({ page: 1, size: 100 })
    const listPages = await Promise.all(coursePage.records.map((course) => assignmentsApi.studentList(course.courseId, { page: 1, size: 100 })))
    const details = await Promise.all(listPages.flatMap((page) => page.records).map((item) => assignmentsApi.studentDetail(item.assignmentId)))
    return { courses: coursePage.records, details }
  })
  if (result) {
    courses.value = result.courses; items.value = result.details
    items.value.forEach((item) => { const id = item.assignment.assignmentId; drafts[id] = item.submission?.content ?? ''; answers[id] = { ...(item.submission?.answers ?? {}) }; fileIds[id] = item.submission?.fileId ?? ''; if (id === selectedAssignmentId.value) expanded[id] = true })
  }
}
function choiceQuestion(type: AssignmentQuestionType) { return ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE'].includes(type) }
function setChoiceAnswer(assignmentId: string, question: AssignmentQuestion, optionIndex: number) {
  answers[assignmentId] ||= {}
  const value = String(optionIndex)
  const current = answers[assignmentId][question.questionId] ?? []
  answers[assignmentId][question.questionId] = question.questionType === 'MULTI_CHOICE'
    ? (current.includes(value) ? current.filter((item) => item !== value) : [...current, value])
    : [value]
}
function setTextAnswer(assignmentId: string, questionId: string, event: Event) {
  answers[assignmentId] ||= {}
  answers[assignmentId][questionId] = [(event.target as HTMLInputElement | HTMLTextAreaElement).value]
}
function answerText(question: AssignmentQuestion, answer: string[] | undefined) {
  if (!answer?.length) return '未作答'
  return choiceQuestion(question.questionType) ? answer.map((item) => question.options[Number(item)] ?? item).join('、') : answer.join('；')
}
function canSubmit(item: StudentAssignmentDetailVO) {
  if (item.assignment.availabilityStatus.code !== 'OPEN') return false
  const id = item.assignment.assignmentId
  if (item.assignment.responseMode === 'QUIZ') {
    return (item.assignment.questions ?? []).every((question) => (answers[id]?.[question.questionId]?.[0] ?? '').trim())
  }
  return Boolean(drafts[id]?.trim() || fileIds[id])
}
function answerPayload(id: string) { return Object.fromEntries(Object.entries(answers[id] ?? {}).map(([key, values]) => [key, [...values]])) }
async function pickFile(item: StudentAssignmentDetailVO, event: Event) {
  const id = item.assignment.assignmentId; const file = (event.target as HTMLInputElement).files?.[0]; if (!file) return
  uploading[id] = true
  try { const stored = await filesApi.upload(file, 'SUBMISSION'); fileIds[id] = stored.fileId; uploadNames[id] = stored.originalName; flash('文件已上传，保存草稿或确认提交后生效') } finally { uploading[id] = false }
}
async function save(item: StudentAssignmentDetailVO) {
  const id = item.assignment.assignmentId
  const saved = await state.run(() => assignmentsApi.saveDraft(id, { content: drafts[id] ?? '', answers: answerPayload(id), fileId: fileIds[id] || null, version: item.submission?.version ?? null }))
  if (saved) { flash('草稿已保存'); await load(); expanded[id] = true }
}
async function submit(item: StudentAssignmentDetailVO) {
  const id = item.assignment.assignmentId
  const saved = await state.run(() => assignmentsApi.submit(id, { content: drafts[id] ?? '', answers: answerPayload(id), fileId: fileIds[id] || null, version: item.submission?.version ?? null }))
  if (saved) { flash('作业已提交，等待教师批改'); await load(); expanded[id] = true }
}
onMounted(load)
</script>

<style scoped>
.assignment-list{display:grid;gap:14px}.assignment-card{padding:0;overflow:hidden}.assignment-summary{padding:20px 22px;display:flex;justify-content:space-between;align-items:center;gap:20px}.assignment-summary .panel-title{margin:0}.feedback-card{margin:0 22px 18px}.assignment-detail{border-top:1px solid var(--line);padding:20px 22px 22px;background:#fafbfd;display:grid;gap:18px}.eyebrow{margin:0 0 8px;color:var(--primary);font-size:12px;font-weight:700;letter-spacing:.05em}.requirements,.submitted-answer,.answer-editor{padding:18px;background:#fff;border:1px solid var(--line)}.attachment-list{display:flex;flex-wrap:wrap;gap:8px;margin-top:13px}.attachment-list a,.file-chip{display:inline-flex;align-items:center;gap:6px;padding:7px 10px;color:var(--primary);background:var(--primary-soft);text-decoration:none}.answer-content{margin:0;padding:14px;white-space:pre-wrap;overflow-x:auto;color:var(--ink);background:#f6f8fb;border:1px solid var(--line);font-family:Consolas,monospace;line-height:1.65}.answer-note{margin:14px 0 0;font-size:12px}.submitted-quiz{display:grid;gap:12px;margin-top:12px}.submitted-quiz p{margin:4px 0 0}.student-question{padding:14px 0;border-bottom:1px solid var(--line)}.student-question:last-child{border-bottom:0}.choice-list{display:grid;gap:8px;margin-top:12px}.choice-list label{display:flex;gap:8px;align-items:flex-start}.code-answer{font-family:Consolas,monospace;line-height:1.6}.upload-box{margin-top:12px;padding:14px;display:flex;gap:11px;align-items:center;border:1px dashed #aab8cc;cursor:pointer;background:#f8faff}.upload-box span{display:grid;gap:3px}.upload-box small{color:var(--muted)}.upload-box input{display:none}@media(max-width:720px){.assignment-summary{align-items:flex-start;flex-direction:column}}
</style>