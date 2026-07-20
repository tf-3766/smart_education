<template>
  <div class="student-home">
    <LiquidGlass class="dashboard-glass home-hero" interactive>
      <div class="hero-copy">
        <p class="eyebrow"><Sparkles :size="15" /> {{ todayLabel }}</p>
        <h1>{{ greeting }}，{{ session.currentUser.name }}</h1>
        <p class="hero-summary">
          {{ pendingTasks.length ? `今天还有 ${pendingTasks.length} 项任务，先完成最接近截止时间的一项。` : '今天没有临期任务，可以安心推进课程进度。' }}
        </p>
      </div>

      <button class="hero-action pressable" type="button" @click="resumeLearning">
        <span class="hero-action-icon"><BookOpen :size="21" /></span>
        <span>
          <small>下一步</small>
          <strong>{{ nextCourse?.title ?? '浏览我的课程' }}</strong>
          <em v-if="nextCourse">已完成 {{ nextCourse.progress }}%</em>
          <em v-else>选择一门课程开始学习</em>
        </span>
        <ArrowRight :size="20" />
      </button>
    </LiquidGlass>

    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />

    <template v-if="!state.loading.value && !state.error.value">
      <LiquidGlass class="dashboard-glass overview-strip" interactive aria-label="学习概览">
        <article>
          <span class="overview-icon blue"><BookOpen :size="18" /></span>
          <div><strong>{{ overview.courses.length }}</strong><span>门在学课程</span></div>
        </article>
        <article>
          <span class="overview-icon amber"><ClipboardList :size="18" /></span>
          <div><strong>{{ pendingTasks.length }}</strong><span>项待完成</span></div>
        </article>
        <article>
          <span class="overview-icon violet"><FileQuestion :size="18" /></span>
          <div><strong>{{ overview.exams.length }}</strong><span>场近期考试</span></div>
        </article>
        <article>
          <span class="overview-icon green"><TrendingUp :size="18" /></span>
          <div><strong>{{ avgProgress }}%</strong><span>平均进度</span></div>
        </article>
      </LiquidGlass>

      <div class="home-layout">
        <LiquidGlass class="dashboard-glass home-section course-section" interactive>
          <div class="section-heading">
            <div>
              <p class="section-kicker">继续学习</p>
              <h2>保持你的学习节奏</h2>
            </div>
            <button class="quiet-link pressable" type="button" @click="router.push('/student/courses')">
              全部课程 <ArrowRight :size="16" />
            </button>
          </div>

          <div v-if="overview.courses.length" class="course-grid">
            <button
              v-for="(course, index) in overview.courses"
              :key="course.id"
              class="course-card pressable"
              type="button"
              :style="{ '--course-index': index }"
              @click="enterCourse(course.id, course.nextLessonId)"
            >
              <span class="course-orb" aria-hidden="true">{{ course.title.slice(0, 1) }}</span>
              <span class="course-copy">
                <small>{{ course.code }} · {{ course.teacher }}</small>
                <strong>{{ course.title }}</strong>
                <span class="course-progress-meta"><span>课程进度</span><b>{{ course.progress }}%</b></span>
                <span class="course-progress" aria-hidden="true"><i :style="{ width: `${course.progress}%` }" /></span>
              </span>
              <span class="course-enter"><ArrowRight :size="17" /></span>
            </button>
          </div>
          <div v-else class="home-empty">
            <BookOpen :size="24" />
            <strong>还没有在学课程</strong>
            <p>去选课中心找到感兴趣的课程，开启今天的学习。</p>
            <button class="empty-action pressable" type="button" @click="router.push('/student/enroll')">前往选课中心</button>
          </div>
        </LiquidGlass>

        <LiquidGlass class="dashboard-glass home-section task-section" interactive>
          <div class="section-heading compact">
            <div>
              <p class="section-kicker">今日节奏</p>
              <h2>待办任务</h2>
            </div>
            <span class="task-count">{{ pendingTasks.length }}</span>
          </div>

          <div v-if="pendingTasks.length" class="task-list">
            <button
              v-for="task in pendingTasks.slice(0, 4)"
              :key="task.id"
              class="task-row pressable"
              type="button"
              @click="router.push('/student/assignments')"
            >
              <span class="task-status"><Clock3 :size="16" /></span>
              <span class="task-copy">
                <strong>{{ task.title }}</strong>
                <small>{{ courseName(task.courseId) }}</small>
              </span>
              <time>{{ formatTime(task.dueAt) }}</time>
            </button>
          </div>
          <div v-else class="task-done">
            <span><CheckCircle2 :size="24" /></span>
            <strong>今天的任务都处理好了</strong>
            <p>做得不错。继续保持稳定的学习节奏。</p>
          </div>

          <button class="task-footer pressable" type="button" @click="router.push('/student/assignments')">
            <CalendarDays :size="17" /> 查看全部学习任务
          </button>
        </LiquidGlass>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Clock3,
  FileQuestion,
  Sparkles,
  TrendingUp,
} from 'lucide-vue-next'
import AsyncState from '@/components/AsyncState.vue'
import LiquidGlass from '@/components/LiquidGlass.vue'
import { studentLearningApi } from '@/services/api'
import { loadStudentOverview, type StudentOverview } from '@/services/adapters/studentAdapter'
import { usePageState } from '@/services/pageState'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()
const state = usePageState()
const overview = reactive<StudentOverview>({ courses: [], assignments: [], exams: [], grades: [], topics: [] })

const pendingTasks = computed(() => overview.assignments
  .filter((item) => item.status === 'OPEN' && !['SUBMITTED', 'GRADED'].includes(item.submissionStatus))
  .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime()))
const avgProgress = computed(() => overview.courses.length
  ? Math.round(overview.courses.reduce((sum, item) => sum + item.progress, 0) / overview.courses.length)
  : 0)
const nextCourse = computed(() => [...overview.courses]
  .filter((course) => course.progress < 100)
  .sort((a, b) => b.progress - a.progress)[0] ?? overview.courses[0])
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date()))

const courseName = (id: string) => overview.courses.find((item) => item.id === id)?.title ?? id
const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN', {
  month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
})

async function load() {
  const data = await state.run(loadStudentOverview)
  if (data) Object.assign(overview, data)
}

async function resumeLearning() {
  if (nextCourse.value) return enterCourse(nextCourse.value.id, nextCourse.value.nextLessonId)
  return router.push('/student/courses')
}

async function enterCourse(courseId: string, lessonId: string | null) {
  if (lessonId) return router.push(`/student/lessons/${lessonId}`)
  const outline = await state.run(() => studentLearningApi.outline(courseId))
  const first = outline?.chapters.flatMap((chapter) => chapter.lessons).find((lesson) => lesson.unlocked)
  if (first) return router.push(`/student/lessons/${first.lessonId}`)
  return router.push('/student/courses')
}

onMounted(load)
</script>

<style scoped>
.student-home {
  --home-blue: #0a64e8;
  --home-ink: #172033;
  --home-muted: #667085;
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  color: var(--home-ink);
}

.student-home::before {
  content: none;
}

.student-home > * { position: relative; z-index: 1; }

.home-hero {
  min-height: 240px;
  margin-bottom: 18px;
  border-radius: 28px !important;
}
.home-hero :deep(.liquid-glass__content) {
  min-height: 240px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 430px);
  align-items: center;
  gap: clamp(28px, 5vw, 72px);
  padding: clamp(28px, 4vw, 48px);
}

.hero-copy { position: relative; z-index: 1; }
.eyebrow { display: flex; align-items: center; gap: 7px; margin: 0 0 13px; color: var(--home-blue); font-size: 13px; font-weight: 700; letter-spacing: .03em; }
.hero-copy h1 { margin: 0; color: #111827; font-size: clamp(32px, 3.3vw, 50px); font-weight: 750; line-height: 1.08; letter-spacing: -.035em; font-optical-sizing: auto; }
.hero-summary { max-width: 38rem; margin: 16px 0 0; color: var(--home-muted); font-size: 15px; line-height: 1.7; }

.pressable { transition: transform 180ms cubic-bezier(.2,.8,.2,1), box-shadow 180ms ease, background-color 180ms ease, border-color 180ms ease; }
.pressable:active { transform: scale(.975); transition-duration: 90ms; }

.hero-action {
  position: relative;
  z-index: 2;
  min-height: 112px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 22px;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border: 1px solid rgba(255,255,255,.42);
  border-radius: 22px !important;
  color: #fff;
  background: linear-gradient(145deg, #1473f3, #0755c9);
  box-shadow: 0 15px 30px rgba(10,100,232,.24), inset 0 1px 0 rgba(255,255,255,.25);
  text-align: left;
}
.hero-action:hover { transform: translateY(-2px); box-shadow: 0 20px 36px rgba(10,100,232,.28), inset 0 1px 0 rgba(255,255,255,.25); }
.hero-action:active { transform: scale(.98); }
.hero-action-icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 15px !important; background: rgba(255,255,255,.16); }
.hero-action span:nth-child(2) { min-width: 0; display: grid; gap: 3px; }
.hero-action small, .hero-action em { color: rgba(255,255,255,.72); font-size: 12px; font-style: normal; }
.hero-action strong { overflow: hidden; font-size: 16px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }

.overview-strip {
  margin-bottom: 18px;
  border-radius: 20px !important;
}
.overview-strip :deep(.liquid-glass__content) { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; background: rgba(255,255,255,.03); }
.overview-strip article { min-width: 0; display: flex; align-items: center; gap: 13px; padding: 24px 20px; border-right: 1px solid rgba(177,200,224,.38); background: rgba(255,255,255,.1); text-shadow: 0 1px 10px rgba(255,255,255,.7); }
.overview-strip article:last-child { border-right: 0; }
.overview-strip article > div { min-width: 0; display: grid; gap: 2px; }
.overview-strip strong { color: var(--home-ink); font-size: 20px; line-height: 1.1; font-variant-numeric: tabular-nums; }
.overview-strip article div span { overflow: hidden; color: var(--home-muted); font-size: 12.5px; text-overflow: ellipsis; white-space: nowrap; }
.overview-icon { width: 38px; height: 38px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 12px !important; }
.overview-icon.blue { color: #0a64e8; background: #e9f2ff; }
.overview-icon.amber { color: #b65c00; background: #fff2df; }
.overview-icon.violet { color: #6d4ed6; background: #f0ebff; }
.overview-icon.green { color: #087d55; background: #e5f7ef; }

.home-layout { min-height: 0; flex: 1 1 auto; display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(320px, .75fr); gap: 18px; align-items: stretch; }
.home-section { height: 100%; border-radius: 24px !important; }
.home-section :deep(.liquid-glass__content) { height: 100%; display: flex; flex-direction: column; }
.course-section { padding: 26px; }
.task-section { overflow: hidden; padding: 26px 26px 0; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.section-kicker { margin: 0 0 5px; color: var(--home-blue); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.section-heading h2 { margin: 0; color: var(--home-ink); font-size: 20px; font-weight: 720; line-height: 1.2; letter-spacing: -.018em; }
.quiet-link { display: inline-flex; align-items: center; gap: 6px; padding: 8px 0; border: 0; color: var(--home-blue); background: transparent; font-size: 13px; font-weight: 650; }
.quiet-link:hover { gap: 9px; }

.course-grid { flex: 1 1 auto; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-content: center; gap: 12px; }
.course-card {
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 30px;
  align-items: center;
  gap: 14px;
  padding: 17px;
  border: 1px solid rgba(255,255,255,.58);
  border-radius: 18px !important;
  color: var(--home-ink);
  background: rgba(250,252,255,.48);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.62);
  text-align: left;
}
.course-card:hover { transform: translateY(-2px); border-color: rgba(255,255,255,.86); background: rgba(255,255,255,.68); box-shadow: 0 8px 18px rgba(44,74,120,.09); }
.course-orb { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 15px !important; color: hsl(calc(210 + var(--course-index) * 28), 72%, 42%); background: hsl(calc(210 + var(--course-index) * 28), 75%, 94%); font-size: 18px; font-weight: 750; }
.course-copy { min-width: 0; display: grid; }
.course-copy > small { overflow: hidden; margin-bottom: 3px; color: var(--home-muted); font-size: 11.5px; text-overflow: ellipsis; white-space: nowrap; }
.course-copy > strong { overflow: hidden; font-size: 14.5px; text-overflow: ellipsis; white-space: nowrap; }
.course-progress-meta { display: flex; justify-content: space-between; gap: 10px; margin-top: 12px; color: var(--home-muted); font-size: 11px; }
.course-progress-meta b { color: #344054; font-variant-numeric: tabular-nums; }
.course-progress { height: 5px; margin-top: 6px; overflow: hidden; border-radius: 999px !important; background: #e9edf3; }
.course-progress i { display: block; height: 100%; border-radius: inherit !important; background: linear-gradient(90deg, #0a64e8, #5da3ff); }
.course-enter { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 50% !important; color: #7b8799; background: #f0f3f7; }
.course-card:hover .course-enter { color: #fff; background: var(--home-blue); }

.task-count { min-width: 34px; height: 34px; display: grid; place-items: center; border-radius: 11px !important; color: var(--home-blue); background: #eaf3ff; font-size: 14px; font-weight: 750; }
.task-list { display: grid; }
.task-row { width: 100%; display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 15px 0; border: 0; border-top: 1px solid #edf0f5; color: var(--home-ink); background: transparent; text-align: left; }
.task-row:hover .task-copy strong { color: var(--home-blue); }
.task-status { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 11px !important; color: #a95800; background: #fff1dc; }
.task-copy { min-width: 0; display: grid; gap: 3px; }
.task-copy strong { overflow: hidden; font-size: 13.5px; text-overflow: ellipsis; white-space: nowrap; transition: color 160ms ease; }
.task-copy small { overflow: hidden; color: var(--home-muted); font-size: 11.5px; text-overflow: ellipsis; white-space: nowrap; }
.task-row time { color: var(--home-muted); font-size: 11.5px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.task-footer { width: calc(100% + 52px); min-height: 52px; display: flex; align-items: center; justify-content: center; gap: 8px; margin: auto -26px 0; border: 0; border-top: 1px solid #edf0f5; color: var(--home-blue); background: rgba(247,250,255,.8); font-size: 13px; font-weight: 650; }
.task-footer:hover { background: #eef5ff; }
.task-done, .home-empty { display: grid; justify-items: center; padding: 30px 12px; color: var(--home-muted); text-align: center; }
.task-done > span { width: 48px; height: 48px; display: grid; place-items: center; margin-bottom: 12px; border-radius: 16px !important; color: #087d55; background: #e5f7ef; }
.task-done strong, .home-empty strong { color: var(--home-ink); }
.task-done p, .home-empty p { margin: 7px 0 0; font-size: 12.5px; line-height: 1.6; }
.home-empty { grid-column: 1 / -1; padding: 48px 16px; border: 1px dashed #d8e0eb; border-radius: 18px !important; }
.home-empty > svg { margin-bottom: 12px; color: var(--home-blue); }
.empty-action { margin-top: 18px; padding: 9px 16px; border: 0; border-radius: 11px !important; color: #fff; background: var(--home-blue); font-weight: 650; }

@media (max-width: 980px) {
  .home-hero :deep(.liquid-glass__content) { grid-template-columns: 1fr; gap: 24px; }
  .hero-action { max-width: 520px; }
  .home-layout { grid-template-columns: 1fr; }
}

@media (max-width: 820px) {
  .student-home { height: auto; display: block; }
  .home-hero { min-height: 0; border-radius: 22px !important; }
  .home-hero :deep(.liquid-glass__content) { min-height: 0; padding: 24px; }
  .hero-copy h1 { font-size: 31px; }
  .hero-action { grid-template-columns: 42px minmax(0, 1fr) 18px; min-height: 96px; padding: 16px; border-radius: 18px !important; }
  .hero-action-icon { width: 42px; height: 42px; }
  .overview-strip :deep(.liquid-glass__content) { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .course-grid { grid-template-columns: 1fr; }
  .course-section, .task-section { padding: 20px; }
  .task-footer { width: calc(100% + 40px); margin-inline: -20px; }
}

@media (min-width: 821px) and (max-height: 850px) {
  .home-hero { min-height: 190px; margin-bottom: 12px; }
  .home-hero :deep(.liquid-glass__content) { min-height: 190px; padding: 24px 30px; }
  .hero-copy h1 { font-size: clamp(32px, 3vw, 42px); }
  .hero-summary { margin-top: 10px; }
  .hero-action { min-height: 96px; padding: 16px; }
  .overview-strip { margin-bottom: 12px; }
  .overview-strip article { padding: 14px 16px; }
  .course-section { padding: 20px; }
  .task-section { padding: 20px 20px 0; }
  .section-heading { margin-bottom: 14px; }
  .course-card { padding: 13px 14px; }
  .task-row { padding: 12px 0; }
  .task-footer { width: calc(100% + 40px); min-height: 46px; margin-inline: -20px; }
}

@media (max-width: 520px) {
  .overview-strip :deep(.liquid-glass__content) { grid-template-columns: 1fr; }
  .overview-strip article { padding: 14px 16px; }
  .section-heading { align-items: flex-start; }
  .course-card { grid-template-columns: 44px minmax(0, 1fr) 26px; padding: 14px; }
  .course-orb { width: 44px; height: 44px; }
  .task-row { grid-template-columns: 32px minmax(0, 1fr); }
  .task-row time { grid-column: 2; }
}

@media (prefers-reduced-motion: reduce) {
  .student-home *, .student-home *::before, .student-home *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; }
  .pressable:hover, .pressable:active { transform: none; }
}

@media (prefers-reduced-transparency: reduce) {
  .home-hero, .overview-strip article, .home-section { background: #fff; backdrop-filter: none; }
}

@media (prefers-contrast: more) {
  .home-hero, .overview-strip, .home-section, .course-card { border-color: #667085; background: #fff; }
}
</style>
