<template>
  <div>
    <div class="page-header">
      <div><p class="page-subtitle"><RouterLink to="/teacher/courses" class="text-link">← 返回课程管理</RouterLink></p><h1 class="page-title">{{ course?.name || '课程内容' }}</h1><p class="page-subtitle">维护章节、课时与资料；发布后学生端可见。</p></div>
      <div class="row wrap">
        <span v-if="knowledgeStatus" class="muted">知识库 {{ knowledgeStatus.indexedChunks }} 个片段</span>
        <AppButton variant="secondary" :loading="knowledgeSyncing" :title="knowledgeStatus?.vectorStoreConfigured ? '抽取课时及已发布资料正文，分片后同步到课程知识库' : '点击后会显示知识库的具体配置问题'" @click="syncKnowledgeBase">同步知识库</AppButton>
        <AppButton variant="primary" @click="openChapterForm()"><span class="row"><Plus :size="16" />新建章节</span></AppButton>
      </div>
    </div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />
    <section class="panel flush">
      <div class="panel-head"><h2>章节</h2><span class="count">共 {{ chapters.length }} 章 · 点击某一章可在下方管理其课时</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>标题</th><th>描述</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="chapter in chapters" :key="chapter.chapterId" :class="{ 'row-active': chapter.chapterId === selectedChapterId }" style="cursor: pointer" @click="selectChapter(chapter.chapterId)">
            <td class="cell-strong">{{ chapter.title }}</td><td>{{ chapter.description || '—' }}</td><td>{{ chapter.sortOrder }}</td>
            <td><StatusBadge :tone="chapter.status.code === 'PUBLISHED' ? 'green' : chapter.status.code === 'OFFLINE' ? 'gray' : 'amber'" :label="chapter.status.label" /></td>
            <td class="cell-actions">
              <button class="text-link" @click.stop="openChapterForm(chapter)">编辑</button>
              <button v-if="chapter.status.code !== 'PUBLISHED'" class="text-link" @click.stop="chapterAction(chapter.chapterId, 'publish')">发布</button>
              <button v-else class="text-link" @click.stop="chapterAction(chapter.chapterId, 'offline')">下线</button>
              <button class="text-link" @click.stop="removeChapter(chapter)">删除</button>
            </td>
          </tr>
          <tr v-if="!chapters.length"><td colspan="5" class="list-empty">尚无章节，点击右上角新建。</td></tr>
        </tbody>
      </table></div>
    </section>
    <section v-if="selectedChapter" class="panel flush">
      <div class="panel-head"><h2>课时 · {{ selectedChapter.title }}</h2><AppButton variant="secondary" @click="openLessonForm()"><span class="row"><Plus :size="14" />新建课时</span></AppButton></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>标题</th><th>类型</th><th>时长</th><th>解锁</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="lesson in lessons" :key="lesson.lessonId">
            <td class="cell-strong">{{ lesson.title }}</td><td>{{ lesson.contentType.label }}</td><td>{{ lesson.estimatedMinutes ? `${lesson.estimatedMinutes} 分钟` : '—' }}</td>
            <td>{{ lesson.unlockType.code === 'SCHEDULED' ? `定时 ${formatTime(lesson.unlockAt)}` : lesson.unlockType.label }}</td>
            <td><StatusBadge :tone="lesson.status.code === 'PUBLISHED' ? 'green' : lesson.status.code === 'OFFLINE' ? 'gray' : 'amber'" :label="lesson.status.label" /></td>
            <td class="cell-actions">
              <button class="text-link" @click="openLessonForm(lesson)">编辑</button>
              <button v-if="lesson.status.code !== 'PUBLISHED'" class="text-link" @click="lessonAction(lesson.lessonId, 'publish')">发布</button>
              <button v-else class="text-link" @click="lessonAction(lesson.lessonId, 'offline')">下线</button>
              <button class="text-link" @click="draftSummary(lesson)">AI 摘要</button>
              <button class="text-link" @click="removeLesson(lesson)">删除</button>
            </td>
          </tr>
          <tr v-if="!lessons.length"><td colspan="6" class="list-empty">本章尚无课时。</td></tr>
        </tbody>
      </table></div>
    </section>
    <section class="panel flush">
      <div class="panel-head"><h2>课程资料</h2><AppButton variant="secondary" @click="openMaterialForm()"><span class="row"><Plus :size="14" />新增资料</span></AppButton></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>名称</th><th>类型</th><th>可见性</th><th>状态</th><th>大小</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="material in materials" :key="material.materialId">
            <td class="cell-strong">{{ material.name }}</td><td>{{ material.materialType.label }}</td><td>{{ material.visibility.label }}</td>
            <td><StatusBadge :tone="material.status.code === 'PUBLISHED' ? 'green' : 'amber'" :label="material.status.label" /></td>
            <td>{{ material.fileSize ? `${Math.round(material.fileSize / 1024)} KB` : '—' }}</td>
            <td class="cell-actions">
              <button class="text-link" @click="openMaterialForm(material)">编辑</button>
              <button v-if="material.status.code !== 'PUBLISHED'" class="text-link" @click="materialAction(material, 'PUBLISHED')">发布</button>
              <button v-else class="text-link" @click="materialAction(material, 'OFFLINE')">下线</button>
              <button class="text-link" @click="removeMaterial(material)">删除</button>
            </td>
          </tr>
          <tr v-if="!materials.length"><td colspan="6" class="list-empty">尚无资料。</td></tr>
        </tbody>
      </table></div>
    </section>
    <AppModal :open="materialForm.open" :title="materialForm.materialId ? '编辑资料' : '新增资料'" :description="materialForm.materialId ? '已发布资料修改后会转为草稿，确认无误后请重新发布。' : '可上传文件或填写外部链接，二选一。'" @close="materialForm.open = false">
      <label class="field-label" for="cc-mat-name">资料名称</label><input id="cc-mat-name" v-model="materialForm.name" data-test="material-name" class="input" placeholder="资料名称" />
      <div class="form-grid push-top">
        <div><label class="field-label" for="cc-mat-source">来源</label><select id="cc-mat-source" v-model="materialForm.source" class="select"><option value="link">外部链接</option><option value="upload">上传文件</option></select></div>
        <div><label class="field-label" for="cc-mat-visibility">归属范围</label><select id="cc-mat-visibility" v-model="materialForm.visibility" data-test="material-visibility" class="select" @change="onMaterialScope"><option value="COURSE">整门课程</option><option value="CHAPTER">指定章节</option><option value="LESSON">指定课时</option></select></div>
      </div>
      <div v-if="materialForm.visibility !== 'COURSE'" class="form-grid push-top">
        <div><label class="field-label" for="cc-mat-chapter">所属章节</label><select id="cc-mat-chapter" v-model="materialForm.chapterId" data-test="material-chapter" class="select" @change="onMaterialChapter"><option value="">请选择章节</option><option v-for="c in chapters" :key="c.chapterId" :value="c.chapterId">{{ c.title }}</option></select></div>
        <div v-if="materialForm.visibility === 'LESSON'"><label class="field-label" for="cc-mat-lesson">所属课时</label><select id="cc-mat-lesson" v-model="materialForm.lessonId" data-test="material-lesson" class="select"><option value="">请选择课时</option><option v-for="l in materialLessons" :key="l.lessonId" :value="l.lessonId">{{ l.title }}</option></select></div>
      </div>
      <template v-if="materialForm.source === 'link'">
        <label class="field-label push-top" for="cc-mat-url">链接地址</label><input id="cc-mat-url" v-model="materialForm.fileUrl" data-test="material-url" class="input" placeholder="https://…" />
      </template>
      <template v-else>
        <label class="field-label push-top" for="cc-mat-file">选择教学文件</label><input id="cc-mat-file" class="input" type="file" accept=".pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.txt,.md,.mp4,.webm,.mov,.png,.jpg,.jpeg,.zip,.rar" @change="onMaterialFile" /><p class="muted" style="font-size:12px">支持 PDF、PPT、Word、Excel、视频、图片、代码文本与压缩包。</p>
        <p v-if="materialForm.uploading" class="muted">正在上传…</p><p v-else-if="materialForm.fileId" class="muted">已上传，文件编号 {{ materialForm.fileId }}</p>
      </template>
      <div class="form-actions"><AppButton variant="secondary" @click="materialForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!materialForm.name.trim() || (materialForm.source === 'link' ? !materialForm.fileUrl.trim() : !materialForm.fileId) || (materialForm.visibility !== 'COURSE' && !materialForm.chapterId) || (materialForm.visibility === 'LESSON' && !materialForm.lessonId)" @click="saveMaterial">保存资料</AppButton></div>
    </AppModal>
    <AppModal :open="lessonForm.open" :title="lessonForm.lessonId ? '编辑课时' : '新建课时'" description="课时须发布且所在章节已发布后学生才可学习。" @close="lessonForm.open = false">
      <label class="field-label" for="cc-lesson-title">课时标题</label><input id="cc-lesson-title" v-model="lessonForm.title" data-test="lesson-title" class="input" placeholder="课时标题" />
      <div class="form-grid push-top">
        <div><label class="field-label" for="cc-lesson-minutes">预计学习时长（分钟）</label><input id="cc-lesson-minutes" v-model.number="lessonForm.estimatedMinutes" class="input" type="number" min="0" /></div>
        <div><label class="field-label" for="cc-lesson-sort-main">排序值</label><input id="cc-lesson-sort-main" v-model.number="lessonForm.sortOrder" class="input" type="number" min="0" /></div>
      </div>
      <label class="field-label push-top" for="cc-lesson-content">课时说明</label>
      <textarea id="cc-lesson-content" v-model="lessonForm.content" data-test="lesson-content" class="textarea" rows="4" placeholder="简要说明学习目标与内容范围；具体教学内容请通过课程资料上传。" />
      <p class="muted" style="font-size:12px">课时用于组织资料；视频、PPT、Word、PDF 等学习内容请作为资料归属到本课时。</p>
      <div class="form-grid push-top">
        <div><label class="field-label" for="cc-lesson-unlock">解锁方式</label><select id="cc-lesson-unlock" v-model="lessonForm.unlockType" class="select"><option value="IMMEDIATE">立即解锁</option><option value="SCHEDULED">定时解锁</option></select></div>
      </div>
      <template v-if="lessonForm.unlockType === 'SCHEDULED'">
        <label class="field-label push-top" for="cc-lesson-unlockat">解锁时间</label><input id="cc-lesson-unlockat" v-model="lessonForm.unlockAt" class="input" type="datetime-local" />
      </template>
      <div class="form-actions"><AppButton variant="secondary" @click="lessonForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!lessonForm.title.trim()" @click="saveLesson">保存课时</AppButton></div>
    </AppModal>
    <AppModal :open="Boolean(summaryLesson)" title="AI 课时摘要" :description="summaryLesson ? `根据《${summaryLesson.title}》的课时说明和已发布资料生成摘要草稿。` : ''" @close="closeSummary">
      <p class="muted">摘要不会自动改写课时或资料，教师确认后可复制到课程公告、导学说明等位置。</p>
      <p v-if="aiLoading" class="notice push-top">正在读取课时及其资料并生成摘要…</p>
      <p v-if="aiError" class="form-error" role="alert">{{ aiError }}</p>
      <AiResultPanel v-if="aiDraft" :result="aiDraft" :allow-adopt="false" class="push-top" @regenerate="summaryLesson && draftSummary(summaryLesson)" />
      <div class="form-actions"><AppButton variant="secondary" @click="closeSummary">关闭</AppButton></div>
    </AppModal>    <AppModal :open="chapterForm.open" :title="chapterForm.chapterId ? '编辑章节' : '新建章节'" description="发布章节后其中已发布课时对学生可见。" @close="chapterForm.open = false">
      <label class="field-label" for="cc-chapter-title">章节标题</label><input id="cc-chapter-title" v-model="chapterForm.title" data-test="chapter-title" class="input" placeholder="例如 第一章 基础语法" />
      <label class="field-label push-top" for="cc-chapter-desc">章节描述</label><textarea id="cc-chapter-desc" v-model="chapterForm.description" class="textarea" placeholder="本章目标（可选）" />
      <label class="field-label push-top" for="cc-chapter-sort">排序值</label><input id="cc-chapter-sort" v-model.number="chapterForm.sortOrder" class="input" type="number" min="0" />
      <div class="form-actions"><AppButton variant="secondary" @click="chapterForm.open = false">取消</AppButton><AppButton variant="primary" :loading="state.loading.value" :disabled="!chapterForm.title.trim()" @click="saveChapter">保存章节</AppButton></div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from 'lucide-vue-next'
import AppButton from '@/components/AppButton.vue'; import AppModal from '@/components/AppModal.vue'; import AsyncState from '@/components/AsyncState.vue'; import StatusBadge from '@/components/StatusBadge.vue'; import AiResultPanel from '@/components/AiResultPanel.vue'; import AiAssistButton from '@/components/AiAssistButton.vue'
import { aiApi, courseContentApi, filesApi, teacherCoursesApi } from '@/services/api'; import type { AiKnowledgeBaseStatusVO, ChapterDetailVO, CourseDetailVO, CourseMaterialVO, LessonDetailVO } from '@/services/api/types'; import { usePageState } from '@/services/pageState'
import { aiDraftToResult } from '@/services/aiDraft'
import { aiErrorMessage } from '@/services/aiHint'
import type { AiResult } from '@/types/domain'

const route = useRoute(); const courseId = String(route.params.courseId)
const state = usePageState(); const course = ref<CourseDetailVO | null>(null); const chapters = ref<ChapterDetailVO[]>([]); const message = ref('')
const knowledgeStatus = ref<AiKnowledgeBaseStatusVO | null>(null); const knowledgeSyncing = ref(false)
const selectedChapterId = ref(''); const lessons = ref<LessonDetailVO[]>([])
const selectedChapter = computed(() => chapters.value.find((chapter) => chapter.chapterId === selectedChapterId.value) ?? null)
const chapterForm = reactive({ open: false, chapterId: '', title: '', description: '', sortOrder: 10, version: 0 })
const lessonForm = reactive({ open: false, lessonId: '', title: '', contentType: 'RICH_TEXT', content: '', videoUrl: '', estimatedMinutes: 30, sortOrder: 10, unlockType: 'IMMEDIATE', unlockAt: '', version: 0 })
const materials = ref<CourseMaterialVO[]>([])
// visibility 即资料归属范围：COURSE 整门课程 / CHAPTER 指定章节 / LESSON 指定课时（对齐后端 MaterialVisibility 枚举）。
const materialForm = reactive({ open: false, materialId: '', name: '', source: 'link', visibility: 'COURSE', chapterId: '', lessonId: '', fileUrl: '', fileId: '', fileSize: 0, mimeType: '', materialType: 'DOCUMENT', sortOrder: 10, status: 'DRAFT', version: 0, uploading: false })
const materialLessons = ref<LessonDetailVO[]>([])
const formatTime = formatDateTime
function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

async function syncKnowledgeBase() {
  knowledgeSyncing.value = true
  try {
    const result = await state.run(() => aiApi.syncKnowledgeBase(courseId))
    if (result) { knowledgeStatus.value = result; flash(`AI 知识库已同步，共 ${result.indexedChunks} 个片段`) }
  } finally { knowledgeSyncing.value = false }
}

async function load() {
  const result = await state.run(async () => ({
    detail: await teacherCoursesApi.getDetail(courseId),
    chapters: await courseContentApi.listChapters(courseId),
    materials: await courseContentApi.listMaterials(courseId, { page: 1, size: 100 }),
    knowledge: await aiApi.knowledgeBaseStatus(courseId).catch(() => null),
  }))
  if (result) {
    course.value = result.detail; chapters.value = result.chapters; materials.value = result.materials.records; knowledgeStatus.value = result.knowledge
    // 默认选中首个章节，让「课时」区块直接可见（否则未选章节时该区块隐藏，易被误认为缺失）。
    // 用 void 不阻塞 load 主流程：章节列表已就绪即可返回，课时在后台异步加载。
    if (chapters.value.length && !chapters.value.some((c) => c.chapterId === selectedChapterId.value)) {
      void selectChapter(chapters.value[0].chapterId)
    }
  }
}

function openChapterForm(chapter?: ChapterDetailVO) {
  Object.assign(chapterForm, chapter
    ? { open: true, chapterId: chapter.chapterId, title: chapter.title, description: chapter.description ?? '', sortOrder: chapter.sortOrder, version: chapter.version }
    : { open: true, chapterId: '', title: '', description: '', sortOrder: (chapters.value.at(-1)?.sortOrder ?? 0) + 10, version: 0 })
}

async function saveChapter() {
  const body = { title: chapterForm.title.trim(), description: chapterForm.description.trim() || null, sortOrder: chapterForm.sortOrder }
  const result = await state.run(() => chapterForm.chapterId
    ? courseContentApi.updateChapter(chapterForm.chapterId, { ...body, version: chapterForm.version })
    : courseContentApi.createChapter(courseId, body))
  if (result) { chapterForm.open = false; flash('章节已保存'); await load() }
}

async function chapterAction(chapterId: string, action: 'publish' | 'offline') {
  const result = await state.run(() => action === 'publish' ? courseContentApi.publishChapter(chapterId) : courseContentApi.offlineChapter(chapterId))
  if (result) { flash(action === 'publish' ? '章节已发布' : '章节已下线'); await load() }
}

async function removeChapter(chapter: ChapterDetailVO) {
  if (!window.confirm(`删除章节「${chapter.title}」？其中的课时将一并不可见。`)) return
  const result = await state.run(async () => { await courseContentApi.deleteChapter(chapter.chapterId); return true })
  if (result) { if (selectedChapterId.value === chapter.chapterId) { selectedChapterId.value = ''; lessons.value = [] } flash('章节已删除'); await load() }
}

async function selectChapter(chapterId: string) {
  selectedChapterId.value = chapterId
  const rows = await state.run(() => courseContentApi.listLessons(chapterId))
  if (rows) lessons.value = rows
}

async function reloadLessons() { if (selectedChapterId.value) await selectChapter(selectedChapterId.value) }

function openLessonForm(lesson?: LessonDetailVO) {
  Object.assign(lessonForm, lesson
    ? { open: true, lessonId: lesson.lessonId, title: lesson.title, contentType: lesson.contentType.code, content: lesson.content ?? '', videoUrl: lesson.videoUrl ?? '', estimatedMinutes: lesson.estimatedMinutes ?? 30, sortOrder: lesson.sortOrder, unlockType: lesson.unlockType.code, unlockAt: lesson.unlockAt ? lesson.unlockAt.slice(0, 16) : '', version: lesson.version }
    : { open: true, lessonId: '', title: '', contentType: 'RICH_TEXT', content: '', videoUrl: '', estimatedMinutes: 30, sortOrder: (lessons.value.at(-1)?.sortOrder ?? 0) + 10, unlockType: 'IMMEDIATE', unlockAt: '', version: 0 })
  aiDraft.value = null; aiError.value = ''
}

const aiDraft = ref<AiResult | null>(null); const aiLoading = ref(false); const aiError = ref(''); const summaryLesson = ref<LessonDetailVO | null>(null)
async function draftSummary(lesson: LessonDetailVO) {
  summaryLesson.value = lesson
  aiDraft.value = null; aiLoading.value = true; aiError.value = ''
  try {
    const draft = await aiApi.lessonSummaryDraft(lesson.lessonId, { courseId })
    aiDraft.value = aiDraftToResult(draft, 'summary', 'AI 课时摘要草稿')
  } catch (caught) { aiError.value = aiErrorMessage(caught) }
  finally { aiLoading.value = false }
}
function closeSummary() { summaryLesson.value = null; aiDraft.value = null; aiError.value = '' }

async function saveLesson() {
  const body = {
    title: lessonForm.title.trim(),
    contentType: 'RICH_TEXT',
    content: lessonForm.content.trim() || null,
    videoUrl: null,
    estimatedMinutes: lessonForm.estimatedMinutes || null,
    sortOrder: lessonForm.sortOrder,
    unlockType: lessonForm.unlockType,
    unlockAt: lessonForm.unlockType === 'SCHEDULED' && lessonForm.unlockAt ? new Date(lessonForm.unlockAt).toISOString() : null,
  }
  const result = await state.run(() => lessonForm.lessonId
    ? courseContentApi.updateLesson(lessonForm.lessonId, { ...body, version: lessonForm.version })
    : courseContentApi.createLesson(selectedChapterId.value, body))
  if (result) { lessonForm.open = false; flash('课时已保存'); await reloadLessons() }
}

async function lessonAction(lessonId: string, action: 'publish' | 'offline') {
  const result = await state.run(() => action === 'publish' ? courseContentApi.publishLesson(lessonId) : courseContentApi.offlineLesson(lessonId))
  if (result) { flash(action === 'publish' ? '课时已发布' : '课时已下线'); await reloadLessons() }
}

async function removeLesson(lesson: LessonDetailVO) {
  if (!window.confirm(`删除课时「${lesson.title}」？`)) return
  const result = await state.run(async () => { await courseContentApi.deleteLesson(lesson.lessonId); return true })
  if (result) { flash('课时已删除'); await reloadLessons() }
}

async function openMaterialForm(material?: CourseMaterialVO) {
  if (!material) {
    Object.assign(materialForm, { open: true, materialId: '', name: '', source: 'link', visibility: 'COURSE', chapterId: '', lessonId: '', fileUrl: '', fileId: '', fileSize: 0, mimeType: '', materialType: 'DOCUMENT', sortOrder: (materials.value.at(-1)?.sortOrder ?? 0) + 10, status: 'DRAFT', version: 0, uploading: false })
    materialLessons.value = []
    return
  }
  Object.assign(materialForm, {
    open: true,
    materialId: material.materialId,
    name: material.name,
    source: material.fileId ? 'upload' : 'link',
    visibility: material.visibility.code,
    chapterId: material.chapterId ?? '',
    lessonId: material.lessonId ?? '',
    fileUrl: material.fileId ? '' : material.fileUrl ?? '',
    fileId: material.fileId ?? '',
    fileSize: material.fileSize ?? 0,
    mimeType: material.mimeType ?? '',
    materialType: material.materialType.code,
    sortOrder: material.sortOrder,
    status: material.status.code,
    version: material.version,
    uploading: false,
  })
  materialLessons.value = []
  if (material.chapterId) {
    const rows = await state.run(() => courseContentApi.listLessons(material.chapterId as string))
    if (rows) materialLessons.value = rows
  }
}

// 范围切回整门课程时清空章节/课时选择；切到章节/课时时保留已选章节。
function onMaterialScope() {
  if (materialForm.visibility === 'COURSE') { materialForm.chapterId = ''; materialForm.lessonId = ''; materialLessons.value = [] }
}

async function onMaterialChapter() {
  materialForm.lessonId = ''
  materialLessons.value = []
  if (!materialForm.chapterId) return
  const rows = await state.run(() => courseContentApi.listLessons(materialForm.chapterId))
  if (rows) materialLessons.value = rows
}

async function onMaterialFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  materialForm.uploading = true
  const stored = await state.run(() => filesApi.upload(file, 'COURSE_MATERIAL'))
  materialForm.uploading = false
  if (stored) { materialForm.fileId = stored.fileId; materialForm.fileSize = stored.fileSize; materialForm.mimeType = stored.mimeType ?? ''; if (!materialForm.name.trim()) materialForm.name = stored.originalName }
}

function inferMaterialType(mimeType: string, name: string) {
  const mime = mimeType.toLowerCase(); const lower = name.toLowerCase()
  if (mime.startsWith('video/') || /\.(mp4|webm|mov)$/.test(lower)) return 'VIDEO'
  if (mime.includes('presentation') || /\.(ppt|pptx)$/.test(lower)) return 'COURSEWARE'
  return 'DOCUMENT'
}
async function saveMaterial() {
  const isLink = materialForm.source === 'link'
  const fields = {
    chapterId: materialForm.visibility !== 'COURSE' && materialForm.chapterId ? materialForm.chapterId : null,
    lessonId: materialForm.visibility === 'LESSON' && materialForm.lessonId ? materialForm.lessonId : null,
    name: materialForm.name.trim(),
    materialType: isLink ? 'LINK' : (materialForm.mimeType ? inferMaterialType(materialForm.mimeType, materialForm.name) : materialForm.materialType),
    fileUrl: isLink ? materialForm.fileUrl.trim() : null,
    fileId: isLink ? null : materialForm.fileId,
    fileSize: isLink ? null : materialForm.fileSize,
    mimeType: isLink ? null : materialForm.mimeType || null,
    visibility: materialForm.visibility,
    sortOrder: materialForm.sortOrder,
  }
  const result = await state.run(() => materialForm.materialId
    ? courseContentApi.updateMaterial(materialForm.materialId, { ...fields, status: 'DRAFT', version: materialForm.version })
    : courseContentApi.createMaterial(courseId, fields))
  if (result) {
    const edited = Boolean(materialForm.materialId)
    materialForm.open = false
    flash(edited ? '资料修改已保存为草稿，请重新发布' : '资料已保存')
    await load()
  }
}

// 资料默认草稿，须发布后学生才可见（后端 canAccessMaterial 要求 status=PUBLISHED）。
// 无独立发布接口，走整体更新接口回写 status（整体覆盖，需带上全部原字段与 version）。
async function materialAction(material: CourseMaterialVO, status: 'PUBLISHED' | 'OFFLINE') {
  const usesManagedFile = Boolean(material.fileId)
  const result = await state.run(() => courseContentApi.updateMaterial(material.materialId, {
    chapterId: material.chapterId ?? null,
    lessonId: material.lessonId ?? null,
    name: material.name,
    materialType: material.materialType.code,
    fileId: usesManagedFile ? material.fileId : null,
    fileKey: usesManagedFile ? null : material.fileKey ?? null,
    fileUrl: usesManagedFile ? null : material.fileUrl ?? null,
    fileSize: material.fileSize ?? null,
    mimeType: material.mimeType ?? null,
    visibility: material.visibility.code,
    sortOrder: material.sortOrder,
    status,
    version: material.version,
  }))
  if (result) { flash(status === 'PUBLISHED' ? '资料已发布，学生端可见' : '资料已下线'); await load() }
}

async function removeMaterial(material: CourseMaterialVO) {
  if (!window.confirm(`删除资料「${material.name}」？`)) return
  const result = await state.run(async () => { await courseContentApi.deleteMaterial(material.materialId); return true })
  if (result) { flash('资料已删除'); await load() }
}

onMounted(load)
</script>
