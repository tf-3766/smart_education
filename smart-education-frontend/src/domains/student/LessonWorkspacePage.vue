<template>
  <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
  <div v-if="lesson && outline" class="workspace-3">
    <aside class="panel col-left lesson-outline-panel">
      <p class="panel-title">{{ outline.courseName }}</p>
      <p class="muted" style="font-size: 12.5px; margin: 0 0 8px">课程目录</p>
      <template v-for="chapter in outline.chapters" :key="chapter.chapterId">
        <p class="muted push-top">{{ chapter.title }}</p>
        <button v-for="item in chapter.lessons" :key="item.lessonId" class="list-nav" :class="{ active: item.lessonId === lesson.lessonId }" :disabled="!item.unlocked" @click="router.push(`/student/lessons/${item.lessonId}`)">
          <span>{{ item.title }}</span><small>{{ item.estimatedMinutes ?? '—' }} 分钟 · {{ item.completed ? '已完成' : item.learningStatus.label }}</small>
        </button>
      </template>
    </aside>

    <main class="panel">
      <div class="spread wrap"><div><h1 class="page-title">{{ lesson.title }}</h1><p class="page-subtitle">{{ chapterTitle }} · 预计 {{ lesson.estimatedMinutes ?? '—' }} 分钟</p></div><StatusBadge :tone="completed ? 'green' : 'blue'" :label="completed ? '已完成' : '学习中'" /></div>
      <video v-if="lesson.videoUrl" class="media-box" :src="lesson.videoUrl" controls />
      <div v-else class="media-box">{{ lesson.contentType.label }} / 课件内容</div>
      <section class="notice"><div><strong>本节内容</strong><p class="pre-line">{{ lesson.content || '本课时暂无文本内容，请查看课程资料。' }}</p></div></section>
      <div class="spread push-top"><span class="muted">完成后将更新课程学习进度</span><AppButton variant="primary" :disabled="completed" @click="complete">{{ completed ? '已标记完成' : '标记完成' }}</AppButton></div>
    </main>

    <aside class="panel col-right">
      <div class="spread"><p class="panel-title" style="margin: 0"><span class="ai-chip">AI</span> 课程答疑</p></div>
      <p class="muted push-top" style="font-size: 12.5px">当前范围：{{ outline.courseName }} / {{ lesson.title }} / 已发布资料</p>
      <textarea v-model="question" class="textarea push-top" placeholder="结合本节内容提问" />
      <div class="form-actions" style="margin-top: 10px"><AppButton variant="primary" :disabled="!question.trim() || asking" @click="ask">{{ asking ? '生成中…' : '发送问题' }}</AppButton></div>
      <p v-if="activity" class="muted push-top" style="font-size: 12.5px"><Search :size="13" style="vertical-align: -2px" /> {{ activity }}</p>
      <AiResultPanel v-if="answer" :result="answer" class="push-top" @regenerate="ask" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import AiResultPanel from '@/components/AiResultPanel.vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { aiApi, studentLearningApi } from '@/services/api'
import type { AiCitationVO, AiToolEvent, CourseOutlineVO, StudentCourseListItemVO, StudentLessonDetailVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const state = usePageState()
const lesson = ref<StudentLessonDetailVO | null>(null)
const outline = ref<CourseOutlineVO | null>(null)
const question = ref('')
const asking = ref(false)
const answer = ref<AiResult>()
// RAG 检索/工具调用进度，展示 AI 作答前的资料检索动作，让「引用有据」可见。
const activity = ref('')
// 一次课时学习即一段连续会话：同一 conversationId 让后端「对话记忆」串起多轮追问。
// 仅用 [A-Za-z0-9._:-]，满足后端校验 ^[A-Za-z0-9._:-]*$（≤64）。
const conversationId = ref('')
const completed = computed(() => lesson.value?.learningRecord?.status.code === 'COMPLETED')
const chapterTitle = computed(() => outline.value?.chapters.find((item) => item.chapterId === lesson.value?.chapterId)?.title ?? '课程内容')

async function resolveRealLesson(requestedId: string): Promise<string | null> {
  try { await studentLearningApi.lessonDetail(requestedId); return requestedId } catch { /* legacy route id */ }
  const courses = await studentLearningApi.myCourses({ page: 1, size: 100 })
  for (const course of courses.records as StudentCourseListItemVO[]) {
    const courseOutline = await studentLearningApi.outline(course.courseId)
    const first = courseOutline.chapters.flatMap((chapter) => chapter.lessons).find((item) => item.unlocked)
    if (first) return first.lessonId
  }
  return null
}
async function load() {
  const requestedId = String(route.params.lessonId ?? '')
  const data = await state.run(async () => {
    const realId = await resolveRealLesson(requestedId)
    if (!realId) throw new Error('暂无可学习课时。')
    if (realId !== requestedId) { await router.replace(`/student/lessons/${realId}`); return null }
    const detail = await studentLearningApi.lessonDetail(realId)
    await studentLearningApi.startLesson(realId)
    return { detail: await studentLearningApi.lessonDetail(realId), outline: await studentLearningApi.outline(detail.courseId) }
  })
  if (data) {
    lesson.value = data.detail
    outline.value = data.outline
    // 切换课时视为新会话，重置对话记忆上下文。
    conversationId.value = `lesson-${data.detail.lessonId}-${Date.now().toString(36)}`
  }
}
async function complete() {
  if (!lesson.value) return
  const record = await state.run(() => studentLearningApi.completeLesson(lesson.value!.lessonId))
  if (record) { lesson.value.learningRecord = record; outline.value = await studentLearningApi.outline(lesson.value.courseId) }
}
async function ask() {
  if (!lesson.value || !question.value.trim()) return
  asking.value = true
  activity.value = ''
  // 后端 QA 流不返回置信度，前端不再伪造固定值。
  const result: AiResult = { id: `qa-${Date.now()}`, type: 'qa', title: '课程答疑回答', content: '', citations: [], confirmed: false }
  answer.value = result
  try {
    await aiApi.qaStream(lesson.value.courseId, { question: question.value, lessonId: lesson.value.lessonId, conversationId: conversationId.value || null }, (event) => {
      if (event.type === 'tool') {
        const tool = event.data as AiToolEvent
        activity.value = tool.summary || `已调用工具 ${tool.toolName}`
      }
      if (event.type === 'delta') result.content += String(event.data ?? '')
      if (event.type === 'citation') {
        const citation = event.data as AiCitationVO
        result.citations = [...(result.citations ?? []), { source: citation.resourceType, title: citation.title }]
      }
      answer.value = { ...result }
    })
  } catch (error) { result.content = aiErrorMessage(error); answer.value = { ...result } }
  finally { asking.value = false }
}
onMounted(load)
watch(() => route.params.lessonId, load)
</script>
