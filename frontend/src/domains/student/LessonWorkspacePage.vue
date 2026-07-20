<template>
  <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
  <div v-if="message" class="toast">{{ message }}</div>

  <div v-if="lesson && outline" class="learning-workspace">
    <aside class="panel course-tree">
      <div class="tree-heading">
        <p class="eyebrow">课程目录</p>
        <h2>{{ outline.courseName }}</h2>
      </div>

      <section v-for="(chapter, chapterIndex) in outline.chapters" :key="chapter.chapterId" class="chapter-node">
        <div class="chapter-title">
          <span>{{ String(chapterIndex + 1).padStart(2, '0') }}</span>
          <strong>{{ chapter.title }}</strong>
        </div>
        <div v-for="item in chapter.lessons" :key="item.lessonId" class="lesson-node">
          <button
            class="lesson-link"
            :class="{ active: item.lessonId === lesson.lessonId }"
            :disabled="!item.unlocked"
            @click="openLesson(item.lessonId)"
          >
            <span class="lesson-dot" />
            <span class="lesson-copy">
              <strong>{{ item.title }}</strong>
              <small>{{ item.estimatedMinutes ?? 0 }} 分钟 · {{ item.completed ? '已完成' : item.learningStatus.label }}</small>
            </span>
          </button>
          <div v-if="item.lessonId === lesson.lessonId" class="material-tree">
            <button
              v-for="material in item.materials"
              :key="material.materialId"
              class="material-link"
              :class="{ active: selectedMaterial?.materialId === material.materialId }"
              @click="selectMaterial(material)"
            >
              <component :is="materialIcon(material)" :size="15" />
              <span>{{ material.name }}</span>
            </button>
            <p v-if="!item.materials.length" class="tree-empty">暂无已发布资料</p>
          </div>
        </div>
      </section>
    </aside>

    <main class="panel lesson-stage">
      <header class="lesson-header">
        <div>
          <p class="eyebrow">{{ chapterTitle }}</p>
          <h1>{{ lesson.title }}</h1>
          <p v-if="lesson.content" class="lesson-overview">{{ lesson.content }}</p>
        </div>
        <div class="lesson-status">
          <StatusBadge :tone="completed ? 'green' : 'blue'" :label="completed ? '已完成' : '学习中'" />
          <span>预计 {{ lesson.estimatedMinutes ?? 0 }} 分钟</span>
        </div>
      </header>

      <section ref="materialStageRef" class="material-stage">
        <template v-if="selectedMaterial">
          <div class="material-toolbar">
            <div>
              <p class="eyebrow">当前资料</p>
              <strong>{{ selectedMaterial.name }}</strong>
            </div>
            <div class="row wrap material-actions">
              <span v-if="viewerKind === 'slides'" class="page-indicator">第 {{ previewPage + 1 }} / {{ previewPageCount }} 页</span>
              <button class="text-link" :disabled="previewLoading" @click="toggleFullscreen">
                <Minimize2 v-if="isFullscreen" :size="15" /><Maximize2 v-else :size="15" />
                {{ isFullscreen ? '退出全屏' : '全屏预览' }}
              </button>
            </div>
          </div>

          <div v-if="previewLoading" class="document-placeholder"><span class="preview-spinner" /><p>正在安全加载资料预览…</p></div>
          <div v-else-if="previewError" class="document-placeholder"><FileWarning :size="42" /><h3>资料暂时无法预览</h3><p>{{ previewError }}</p><AppButton variant="secondary" @click="loadViewer">重新加载</AppButton></div>
          <video v-else-if="viewerKind === 'video'" class="resource-viewer" :src="viewerUrl" controls />
          <img v-else-if="viewerKind === 'image'" class="resource-image" :src="viewerUrl" :alt="selectedMaterial.name" />
          <iframe v-else-if="viewerKind === 'pdf' || viewerKind === 'link'" class="resource-frame" :src="viewerUrl" :title="selectedMaterial.name" />
          <div v-else-if="viewerKind === 'slides'" class="slide-viewer">
            <button class="slide-nav" :disabled="previewPage <= 0 || previewLoading" aria-label="上一页" @click="changeSlide(-1)"><ChevronLeft /></button>
            <div class="slide-canvas">
              <img class="slide-image" :src="viewerUrl" :alt="selectedMaterial.name + ' 第 ' + (previewPage + 1) + ' 页'" />
            </div>
            <button class="slide-nav" :disabled="previewPage >= previewPageCount - 1 || previewLoading" aria-label="下一页" @click="changeSlide(1)"><ChevronRight /></button>
          </div>
          <pre v-else-if="viewerKind === 'document'" class="document-text">{{ documentText }}</pre>
          <div v-else class="document-placeholder">
            <component :is="materialIcon(selectedMaterial)" :size="42" />
            <h3>{{ selectedMaterial.name }}</h3>
            <p>该格式暂不支持网页内渲染。教师可优先上传 PDF、PPTX、图片或视频格式。</p>
          </div>
        </template>
        <div v-else class="document-placeholder empty-stage">
          <FolderOpen :size="44" />
          <h3>本课时暂无已发布资料</h3>
          <p>教师发布 PDF、视频、PPT、Word 或外部链接后会显示在这里。</p>
        </div>
      </section>
      <footer class="completion-bar">
        <div>
          <strong>{{ completed ? '本课时已完成' : completionHint }}</strong>
          <p>{{ completed ? '学习进度已更新。' : '系统会依据进入课时后的学习时长进行校验。' }}</p>
        </div>
        <AppButton class="complete-button" variant="primary" :disabled="completed || remainingSeconds > 0" @click="complete">
          {{ completed ? '已完成' : remainingSeconds > 0 ? formatRemaining(remainingSeconds) : '标记完成' }}
        </AppButton>
      </footer>
    </main>

    <aside class="panel ai-sidebar">
      <div class="spread">
        <p class="panel-title"><span class="ai-chip">AI</span> 课程答疑</p>
      </div>
      <p class="muted ai-scope">回答范围：{{ outline.courseName }} / {{ lesson.title }} / 已发布资料</p>
      <textarea v-model="question" class="textarea" placeholder="针对当前课时资料提问，例如：请解释快速排序的分区过程" />
      <div class="form-actions">
        <AppButton variant="primary" :disabled="!question.trim() || asking" @click="ask">{{ asking ? '生成中…' : '发送问题' }}</AppButton>
      </div>
      <p v-if="activity" class="muted ai-activity"><Search :size="13" /> {{ activity }}</p>
      <AiResultPanel v-if="answer" :result="answer" :allow-adopt="false" class="push-top" @regenerate="ask" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, ChevronRight, File, FileImage, FileText, FileWarning, Film, FolderOpen, Link2, Maximize2, Minimize2, Presentation, Search } from 'lucide-vue-next'
import AiResultPanel from '@/components/AiResultPanel.vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { aiApi, filesApi, studentLearningApi } from '@/services/api'
import type { AiCitationVO, AiToolEvent, CourseOutlineVO, MaterialAccessVO, StudentCourseListItemVO, StudentLessonDetailVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'
import { RuntimeError } from '@/services/runtime'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const state = usePageState()
const lesson = ref<StudentLessonDetailVO | null>(null)
const outline = ref<CourseOutlineVO | null>(null)
const selectedMaterial = ref<MaterialAccessVO | null>(null)
const materialStageRef = ref<HTMLElement | null>(null)
const viewerUrl = ref('')
const documentText = ref('')
const previewLoading = ref(false)
const previewError = ref('')
const previewPage = ref(0)
const previewPageCount = ref(1)
const isFullscreen = ref(false)
const question = ref('')
const asking = ref(false)
const answer = ref<AiResult>()
const activity = ref('')
const conversationId = ref('')
const message = ref('')
const localActiveSeconds = ref(0)
const heartbeatBusy = ref(false)
let heartbeatTicks = 0
let timer: number | undefined

const completed = computed(() => lesson.value?.learningRecord?.status.code === 'COMPLETED')
const chapterTitle = computed(() => outline.value?.chapters.find((item) => item.chapterId === lesson.value?.chapterId)?.title ?? '课程内容')
const requiredSeconds = computed(() => Math.max(0, (lesson.value?.estimatedMinutes ?? 0) * 60))
const studiedSeconds = computed(() => {
  const stored = lesson.value?.learningRecord?.studySeconds ?? 0
  return stored + localActiveSeconds.value
})
const remainingSeconds = computed(() => completed.value ? 0 : Math.max(0, requiredSeconds.value - studiedSeconds.value))
const completionHint = computed(() => remainingSeconds.value > 0 ? `达到预计学习时长后可完成，还需 ${formatRemaining(remainingSeconds.value)}` : '已达到预计学习时长，可以标记完成')
const viewerKind = computed(() => {
  const material = selectedMaterial.value
  if (!material) return 'none'
  const mime = (material.mimeType ?? '').toLowerCase()
  const name = material.name.toLowerCase()
  if (mime.startsWith('video/') || /\.(mp4|webm|ogg|mov)$/.test(name)) return 'video'
  if (mime.startsWith('image/') || /\.(png|jpe?g|gif|webp|svg)$/.test(name)) return 'image'
  if (mime.includes('pdf') || name.endsWith('.pdf')) return 'pdf'
  if (mime.includes('presentation') || mime.includes('powerpoint') || /\.(ppt|pptx)$/.test(name) || material.materialType.code === 'COURSEWARE') return 'slides'
  if (material.materialType.code === 'LINK' || material.accessMode === 'EXTERNAL_LINK') return 'link'
  return 'document'
})

function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2600) }
function formatRemaining(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}
function materialIcon(material: MaterialAccessVO) {
  const mime = (material.mimeType ?? '').toLowerCase()
  const name = material.name.toLowerCase()
  if (material.materialType.code === 'LINK') return Link2
  if (mime.startsWith('video/') || /\.(mp4|webm|mov)$/.test(name)) return Film
  if (mime.startsWith('image/')) return FileImage
  if (/\.(ppt|pptx)$/.test(name)) return Presentation
  if (/\.(pdf|doc|docx|txt|md)$/.test(name)) return FileText
  return File
}
function openLesson(lessonId: string) {
  if (lessonId !== lesson.value?.lessonId) router.push(`/student/lessons/${lessonId}`)
}
function materialFileId(material: MaterialAccessVO | null): string | null {
  if (!material?.accessUrl) return null
  return material.accessUrl.match(/\/files\/(\d+)\/content/)?.[1] ?? null
}
function releaseViewerUrl() {
  if (viewerUrl.value.startsWith('blob:')) URL.revokeObjectURL(viewerUrl.value)
  viewerUrl.value = ''
  documentText.value = ''
}
async function loadViewer() {
  releaseViewerUrl()
  previewError.value = ''
  previewLoading.value = true
  try {
    const material = selectedMaterial.value
    if (!material) return
    const fileId = materialFileId(material)
    if (material.materialType.code === 'LINK' || material.accessMode === 'EXTERNAL_LINK' || !fileId) {
      viewerUrl.value = material.accessUrl
      return
    }
    if (viewerKind.value === 'document') {
      const preview = await filesApi.textPreview(fileId)
      documentText.value = preview.text || preview.message
    } else if (viewerKind.value === 'slides') {
      const preview = await filesApi.previewObjectUrl(fileId, previewPage.value)
      viewerUrl.value = preview.url
      previewPageCount.value = preview.pageCount
    } else {
      viewerUrl.value = await filesApi.contentObjectUrl(fileId)
    }
  } catch (error) {
    previewError.value = error instanceof Error ? error.message : '资料预览加载失败'
  } finally { previewLoading.value = false }
}
async function selectMaterial(material: MaterialAccessVO) {
  selectedMaterial.value = material
  previewPage.value = 0
  previewPageCount.value = 1
  try { selectedMaterial.value = await studentLearningApi.materialAccess(material.materialId) } catch { /* list data remains usable */ }
  await loadViewer()
}
async function changeSlide(offset: number) {
  const target = Math.max(0, Math.min(previewPageCount.value - 1, previewPage.value + offset))
  if (target === previewPage.value) return
  previewPage.value = target
  await loadViewer()
}
async function toggleFullscreen() {
  if (!materialStageRef.value) return
  if (document.fullscreenElement) await document.exitFullscreen()
  else await materialStageRef.value.requestFullscreen()
}
function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === materialStageRef.value
}
function onPreviewKeydown(event: KeyboardEvent) {
  if (!isFullscreen.value || viewerKind.value !== 'slides') return
  if (event.key === 'ArrowLeft') { event.preventDefault(); void changeSlide(-1) }
  if (event.key === 'ArrowRight' || event.key === ' ') { event.preventDefault(); void changeSlide(1) }
}
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
    const initial = await studentLearningApi.lessonDetail(realId)
    await studentLearningApi.startLesson(realId)
    const detail = await studentLearningApi.lessonDetail(realId)
    return { detail, outline: await studentLearningApi.outline(initial.courseId) }
  })
  if (data) {
    lesson.value = data.detail
    outline.value = data.outline
    selectedMaterial.value = data.detail.materials[0] ?? null
    previewPage.value = 0
    await loadViewer()
    localActiveSeconds.value = 0
    heartbeatTicks = 0
    conversationId.value = `lesson-${data.detail.lessonId}-${Date.now().toString(36)}`
  }
}
async function complete() {
  if (!lesson.value) return
  try {
    await syncHeartbeat()
    const record = await studentLearningApi.completeLesson(lesson.value.lessonId)
    lesson.value.learningRecord = record
    outline.value = await studentLearningApi.outline(lesson.value.courseId)
    flash('课时已完成，课程进度已更新')
  } catch (error) {
    flash(error instanceof RuntimeError ? error.message : '暂时无法标记完成')
  }
}
async function syncHeartbeat() {
  if (!lesson.value || completed.value || heartbeatBusy.value) return
  heartbeatBusy.value = true
  try {
    const record = await studentLearningApi.heartbeatLesson(lesson.value.lessonId)
    lesson.value.learningRecord = record
    localActiveSeconds.value = 0
    heartbeatTicks = 0
  } finally { heartbeatBusy.value = false }
}

async function onVisibilityChange() {
  if (!lesson.value || completed.value) return
  if (document.visibilityState === 'hidden') {
    await syncHeartbeat()
  } else {
    const record = await studentLearningApi.startLesson(lesson.value.lessonId)
    lesson.value.learningRecord = record
    localActiveSeconds.value = 0
    heartbeatTicks = 0
  }
}

function activeStudyTick() {
  if (!lesson.value || completed.value || document.visibilityState !== 'visible') return
  localActiveSeconds.value += 1
  heartbeatTicks += 1
  if (heartbeatTicks >= 15) void syncHeartbeat()
}

async function ask() {
  if (!lesson.value || !question.value.trim()) return
  asking.value = true
  activity.value = ''
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

onMounted(() => {
  load()
  timer = window.setInterval(activeStudyTick, 1000)
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('keydown', onPreviewKeydown)
})
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('keydown', onPreviewKeydown)
  void syncHeartbeat()
  releaseViewerUrl()
})
watch(() => route.params.lessonId, load)
</script>

<style scoped>
.learning-workspace { display: grid; grid-template-columns: 280px minmax(460px, 1fr) 320px; gap: 16px; align-items: start; }
.course-tree { padding: 0; overflow: hidden; position: sticky; top: 92px; max-height: calc(100vh - 116px); overflow-y: auto; }
.tree-heading { padding: 20px 20px 16px; border-bottom: 1px solid var(--line); }
.tree-heading h2 { margin: 5px 0 0; font-size: 17px; }
.eyebrow { margin: 0; color: var(--primary); font-size: 12px; font-weight: 700; letter-spacing: .06em; }
.chapter-node { padding: 16px 12px 8px; }
.chapter-node + .chapter-node { border-top: 1px solid var(--line); }
.chapter-title { display: flex; gap: 9px; align-items: center; padding: 0 8px 10px; }
.chapter-title > span { color: var(--primary); font-size: 11px; font-weight: 800; }
.chapter-title strong { font-size: 14px; }
.lesson-link, .material-link { width: 100%; border: 0; background: transparent; color: var(--ink); cursor: pointer; text-align: left; }
.lesson-link { display: flex; gap: 10px; padding: 10px 9px; border-radius: 5px; }
.lesson-link:hover, .lesson-link.active { background: var(--primary-soft); }
.lesson-dot { width: 7px; height: 7px; margin-top: 6px; border: 2px solid var(--primary); border-radius: 50%; flex: 0 0 auto; }
.lesson-copy { min-width: 0; display: grid; gap: 4px; }
.lesson-copy strong { font-size: 14px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }
.lesson-copy small { color: var(--muted); font-size: 11.5px; }
.material-tree { margin: 3px 0 8px 20px; padding-left: 10px; border-left: 1px solid var(--line); }
.material-link { display: flex; align-items: center; gap: 7px; padding: 8px; color: var(--muted); font-size: 12.5px; border-radius: 4px; }
.material-link:hover, .material-link.active { color: var(--primary); background: #f3f7ff; }
.material-link span { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tree-empty { color: var(--muted); font-size: 12px; margin: 8px; }
.lesson-stage { padding: 0; overflow: hidden; min-height: 660px; }
.lesson-header { padding: 22px 24px 18px; display: flex; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--line); }
.lesson-header h1 { margin: 5px 0; font-size: 25px; }
.lesson-overview { color: var(--muted); margin: 7px 0 0; line-height: 1.65; max-width: 700px; }
.lesson-status { display: grid; gap: 8px; justify-items: end; color: var(--muted); font-size: 12px; white-space: nowrap; }
.material-stage { min-height: 500px; background: #f5f7fb; padding: 18px; }
.material-toolbar { min-height: 52px; padding: 12px 15px; display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid var(--line); border-bottom: 0; }
.resource-frame, .resource-viewer { width: 100%; height: 520px; display: block; border: 1px solid var(--line); background: #111827; }
.resource-image { display: block; max-width: 100%; max-height: 520px; margin: 0 auto; border: 1px solid var(--line); background: #fff; object-fit: contain; }
.document-text { min-height: 470px; max-height: 620px; overflow: auto; margin: 0; padding: 24px; white-space: pre-wrap; color: #27364b; background: #fff; border: 1px solid var(--line); font: 14px/1.8 ui-monospace, SFMono-Regular, Consolas, monospace; }
.document-placeholder { min-height: 470px; display: grid; place-content: center; justify-items: center; gap: 11px; color: var(--muted); text-align: center; background: #fff; border: 1px solid var(--line); }
.document-placeholder h3 { color: var(--ink); margin: 0; }
.document-placeholder p { max-width: 430px; margin: 0 0 8px; }
.empty-stage { min-height: 540px; }
.completion-bar { padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; gap: 20px; border-top: 1px solid var(--line); }
.completion-bar p { margin: 3px 0 0; color: var(--muted); font-size: 12px; }
.complete-button { min-width: 112px; }
.ai-sidebar { position: sticky; top: 92px; }
.ai-sidebar .panel-title { margin: 0; }
.ai-scope { margin: 12px 0; font-size: 12.5px; line-height: 1.6; }
.ai-activity { display: flex; align-items: center; gap: 5px; margin-top: 10px; font-size: 12.5px; }
@media (max-width: 1280px) { .learning-workspace { grid-template-columns: 260px minmax(440px, 1fr); } .ai-sidebar { grid-column: 2; position: static; } }
@media (max-width: 860px) { .learning-workspace { grid-template-columns: 1fr; } .course-tree, .ai-sidebar { position: static; max-height: none; } .ai-sidebar { grid-column: auto; } .lesson-header { flex-direction: column; } .lesson-status { justify-items: start; } }

.material-actions { justify-content: flex-end; }
.material-actions .text-link { display: inline-flex; align-items: center; gap: 5px; }
.page-indicator { color: var(--muted); font-size: 12px; }
.slide-viewer { min-height: 500px; display: grid; grid-template-columns: 48px minmax(0, 1fr) 48px; align-items: center; gap: 10px; background: #111827; padding: 16px; }
.slide-canvas { min-width: 0; min-height: 0; height: 100%; display: grid; place-items: center; overflow: hidden; }
.slide-image { display: block; max-width: 100%; max-height: 620px; object-fit: contain; background: #fff; box-shadow: 0 10px 30px rgba(0,0,0,.25); }
.slide-nav { display: grid; place-items: center; width: 42px; height: 42px; border: 1px solid rgba(255,255,255,.35); border-radius: 50%; color: #fff; background: rgba(255,255,255,.12); cursor: pointer; }
.slide-nav:disabled { opacity: .3; cursor: not-allowed; }
.preview-spinner { width: 28px; height: 28px; border: 3px solid #dbeafe; border-top-color: var(--primary); border-radius: 50%; animation: preview-spin .8s linear infinite; }
.material-stage:fullscreen { box-sizing: border-box; width: 100vw; height: 100vh; min-height: 0; overflow: hidden; padding: 16px; display: flex; flex-direction: column; background: #0f172a; }
.material-stage:fullscreen .material-toolbar { flex: 0 0 auto; position: relative; z-index: 2; }
.material-stage:fullscreen .slide-viewer { flex: 1 1 auto; min-height: 0; height: calc(100vh - 96px); grid-template-columns: 56px minmax(0, 1fr) 56px; }
.material-stage:fullscreen .slide-image { width: auto; height: auto; max-width: 100%; max-height: calc(100vh - 128px); }
.material-stage:fullscreen .resource-image, .material-stage:fullscreen .resource-frame, .material-stage:fullscreen .resource-viewer { max-height: calc(100vh - 100px); height: calc(100vh - 100px); }
@keyframes preview-spin { to { transform: rotate(360deg); } }</style>