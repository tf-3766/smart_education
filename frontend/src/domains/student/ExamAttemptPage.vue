<template>
  <div>
    <div class="page-header"><div><p class="page-subtitle"><RouterLink to="/student/exams" class="text-link">← 返回考试安排</RouterLink></p><h1 class="page-title">{{ examTitle }}</h1><p v-if="attempt" class="page-subtitle">共 {{ attempt.questions.length }} 题 · {{ attempt.deadlineAt ? `答题截止 ${formatTime(attempt.deadlineAt)}` : '请在考试窗口内完成' }}</p></div><StatusBadge v-if="attempt" :tone="attempt.status === 'GRADED' ? 'green' : attempt.status === 'SUBMITTED' ? 'blue' : 'amber'" :label="statusLabel" /></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />

    <template v-if="attempt && attempt.status === 'IN_PROGRESS'">
      <section v-for="question in orderedQuestions" :key="question.questionId" class="panel">
        <p class="cell-strong">{{ question.questionOrder }}. {{ question.stem }}<span class="muted">（{{ typeLabel(question.questionType) }} · {{ question.score }} 分）</span></p>
        <template v-if="question.questionType === 'MULTIPLE_CHOICE'">
          <label v-for="option in question.options" :key="option.label" class="row push-top" style="gap: 8px"><input v-model="multiAnswers[question.questionId]" type="checkbox" :value="option.label" /><span>{{ option.label }}. {{ option.content }}</span></label>
        </template>
        <template v-else-if="['FILL_BLANK', 'SHORT_ANSWER'].includes(question.questionType)">
          <textarea v-model="textAnswers[question.questionId]" class="textarea push-top" rows="4" placeholder="填写你的回答" />
        </template>
        <template v-else>
          <label v-for="option in question.options" :key="option.label" class="row push-top" style="gap: 8px"><input v-model="textAnswers[question.questionId]" type="radio" :name="question.questionId" :value="option.label" /><span>{{ option.label }}. {{ option.content }}</span></label>
        </template>
      </section>
      <div class="form-actions"><AppButton variant="primary" @click="submit">交卷</AppButton></div>
    </template>

    <template v-else-if="attempt">
      <section class="panel">
        <div class="spread"><h2 class="panel-title" style="margin: 0">答卷结果</h2><StatusBadge :tone="attempt.status === 'GRADED' ? 'green' : 'blue'" :label="attempt.status === 'GRADED' ? `总分 ${attempt.score}` : `客观题得分 ${objectiveScore}，简答题待教师评分`" /></div>
        <div v-for="question in orderedQuestions" :key="question.questionId" class="notice push-top"><div>
          <strong>{{ question.questionOrder }}. {{ question.stem }}（{{ question.score }} 分）</strong>
          <p class="pre-line">答：{{ answerOf(question.questionId)?.answerContent || '未作答' }}</p>
          <p class="muted">{{ answerOf(question.questionId)?.score != null ? `得分 ${answerOf(question.questionId)!.score}` : '待教师评分' }}</p>
          <p v-if="answerOf(question.questionId)?.teacherComment" class="muted">教师评语：{{ answerOf(question.questionId)!.teacherComment }}</p>
        </div></div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/AppButton.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'
import { examsApi } from '@/services/api'; import type { ExamAttemptVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
import { confirmDialog } from '@/services/confirmDialog'

const route = useRoute(); const examId = String(route.params.examId)
const state = usePageState(); const attempt = ref<ExamAttemptVO | null>(null); const message = ref('')
const textAnswers = reactive<Record<string, string>>({}); const multiAnswers = reactive<Record<string, string[]>>({})
const examTitle = computed(() => String(route.query.title ?? '在线考试'))
const orderedQuestions = computed(() => [...(attempt.value?.questions ?? [])].sort((a, b) => a.questionOrder - b.questionOrder))
const objectiveScore = computed(() => (attempt.value?.answers ?? []).reduce((sum, answer) => sum + (answer.score ?? 0), 0))
const statusLabel = computed(() => ({ IN_PROGRESS: '答题中', SUBMITTED: '已提交', GRADED: '已评分' }[attempt.value?.status ?? 'IN_PROGRESS']))
const typeLabel = (code: string) => ({ SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', TRUE_FALSE: '判断', FILL_BLANK: '填空', SHORT_ANSWER: '简答' }[code] ?? code)
const formatTime = formatDateTime
const answerOf = (questionId: string) => attempt.value?.answers.find((answer) => answer.questionId === questionId)

async function load() {
  const started = await state.run(() => examsApi.startAttempt(examId))
  if (started) {
    attempt.value = started
    for (const question of started.questions) {
      const existing = started.answers.find((answer) => answer.questionId === question.questionId)?.answerContent ?? ''
      if (question.questionType === 'MULTIPLE_CHOICE') multiAnswers[question.questionId] = existing ? existing.split(',') : []
      else textAnswers[question.questionId] = existing
    }
  }
}

async function submit() {
  if (!attempt.value) return
  if (!(await confirmDialog('确认交卷？交卷后不能修改答案。', { title: '提交试卷', confirmLabel: '确认交卷' }))) return
  const answers = attempt.value.questions.map((question) => ({
    questionId: question.questionId,
    answerContent: question.questionType === 'MULTIPLE_CHOICE' ? [...(multiAnswers[question.questionId] ?? [])].sort().join(',') : textAnswers[question.questionId] ?? '',
  }))
  const submitted = await state.run(() => examsApi.submitAttempt(attempt.value!.attemptId, { answers, version: attempt.value!.version }))
  if (submitted) { attempt.value = submitted; message.value = '交卷成功'; window.setTimeout(() => (message.value = ''), 2200) }
}

onMounted(load)
</script>
