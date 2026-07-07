<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Clock, Collection, MagicStick, Reading, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppShell from '../../components/layout/AppShell.vue'
import MetricCard from '../../components/common/MetricCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'
import LineTrendChart from '../../components/charts/LineTrendChart.vue'
import type { MetricItem } from '../../types/ui'

const router = useRouter()
const aiPlanning = ref(false)
const aiPlanVisible = ref(false)

const metrics: MetricItem[] = [
  { label: '在学课程', value: '5', detail: '本学期', tone: 'blue', icon: Reading },
  { label: '本周待办', value: '4', detail: '2 项今天截止', tone: 'teal', icon: Tickets },
  { label: '即将考试', value: '1', detail: '周五 09:00', tone: 'orange', icon: Calendar },
  { label: '本周学习', value: '8.5', detail: '小时 · +1.2h', tone: 'purple', icon: Clock },
]

const tasks = [
  { type: '作业', tone: 'primary', title: '数据结构 第3章作业', course: '数据结构', time: '今天 23:59', meta: '剩余 6 小时' },
  { type: '测验', tone: 'warning', title: '大学物理 期中测验', course: '大学物理（上）', time: '周五 09:00', meta: '时长 90 分钟' },
  { type: '作业', tone: 'info', title: '线性代数 第2章作业', course: '线性代数', time: '周六 23:59', meta: '剩余 3 天' },
  { type: '讨论', tone: 'neutral', title: '操作系统课程讨论', course: '操作系统', time: '周日', meta: '12 条新回复' },
] as const

const courseProgress = [
  { name: '数据结构', value: 42, tone: 'teal' },
  { name: '大学英语', value: 28, tone: 'orange' },
  { name: '大学物理（上）', value: 65, tone: 'blue' },
  { name: '线性代数', value: 55, tone: 'teal' },
]

const generatePlan = () => {
  aiPlanning.value = true
  setTimeout(() => {
    aiPlanning.value = false
    aiPlanVisible.value = true
    ElMessage.success('学习计划已生成，可加入本周计划')
  }, 900)
}
</script>

<template>
  <AppShell role="student">
    <div class="page-container student-dashboard">
      <div class="page-title-row">
        <div>
          <h1>早上好，李同学</h1>
          <p>今天有 2 项任务待完成，下一场考试在周五。</p>
        </div>
        <el-button :icon="Collection" plain>学习日历</el-button>
      </div>

      <div class="metrics-grid">
        <MetricCard v-for="item in metrics" :key="item.label" :item="item" />
      </div>

      <section class="dashboard-main-grid">
        <article class="panel continue-panel">
          <div class="panel-heading">
            <h2>继续学习</h2>
            <a @click="router.push('/student/courses/data-structures/lessons/binary-tree')">查看课程</a>
          </div>
          <div class="continue-content">
            <div class="course-cover" aria-hidden="true">
              <span class="tree-node tree-node--top"></span>
              <span class="tree-node tree-node--left"></span>
              <span class="tree-node tree-node--right"></span>
              <span class="tree-line tree-line--left"></span>
              <span class="tree-line tree-line--right"></span>
              <el-icon><Reading /></el-icon>
            </div>
            <div class="continue-copy">
              <StatusTag label="进行中" tone="success" />
              <h2>数据结构</h2>
              <p>第 3 章 · 树与二叉树</p>
              <strong>3.2 二叉树的性质</strong>
              <div class="course-progress-line">
                <div class="progress-track"><div class="progress-fill" style="width: 42%"></div></div>
                <span>42%</span>
              </div>
              <div class="course-meta">
                <span>已完成 8 / 20 课时</span>
                <span>预计剩余 1.5 小时</span>
              </div>
            </div>
            <el-button type="primary" size="large" @click="router.push('/student/courses/data-structures/lessons/binary-tree')">继续学习</el-button>
          </div>
        </article>

        <article id="tasks" class="panel task-panel">
          <div class="panel-heading">
            <h2>本周待办</h2>
            <a>查看全部（4）</a>
          </div>
          <button v-for="task in tasks" :key="task.title" class="task-row" type="button">
            <span class="task-type"><StatusTag :label="task.type" :tone="task.tone" /></span>
            <span class="task-copy"><strong>{{ task.title }}</strong><small>{{ task.course }}</small></span>
            <span class="task-time" :class="{ urgent: task.meta.includes('6 小时') }"><strong>{{ task.time }}</strong><small>{{ task.meta }}</small></span>
            <span class="task-arrow">›</span>
          </button>
        </article>
      </section>

      <section id="progress" class="learning-grid">
        <article class="panel progress-panel">
          <div class="panel-heading"><h2>学习进度</h2><a>查看全部</a></div>
          <div class="progress-list">
            <div v-for="course in courseProgress" :key="course.name" class="progress-item">
              <div><span>{{ course.name }}</span><strong>{{ course.value }}%</strong></div>
              <div class="progress-track"><div class="progress-fill" :class="`progress-fill--${course.tone}`" :style="{ width: `${course.value}%` }"></div></div>
            </div>
          </div>
        </article>

        <article class="panel trend-panel">
          <div class="panel-heading">
            <h2>成绩趋势 / 学习时长</h2>
            <div class="mini-tabs"><button class="active">成绩趋势</button><button>学习时长</button></div>
          </div>
          <div class="chart-legend"><span class="teal">数据结构</span><span class="blue">大学物理</span><span class="orange">大学英语</span></div>
          <LineTrendChart />
        </article>

        <article class="panel risk-panel">
          <div class="risk-title">
            <div><span class="risk-icon">!</span><h2>学业风险提醒</h2></div>
            <StatusTag label="中等风险" tone="warning" />
          </div>
          <div class="risk-summary">
            <strong>大学英语</strong>
            <ul>
              <li>当前平均分 56（课程平均 72）</li>
              <li>近 2 次作业平均分低于 60</li>
              <li>单元测验正确率 58%</li>
            </ul>
          </div>
          <div class="ai-plan-block">
            <div class="ai-plan-title"><el-icon><MagicStick /></el-icon><strong>AI 学习建议</strong></div>
            <template v-if="aiPlanVisible">
              <p>建议周二前完成第 5 章阅读，周四安排 20 分钟词汇复习，并补交阅读作业 4。</p>
              <div class="ai-plan-actions"><el-button size="small" type="primary">加入本周计划</el-button><el-button size="small" plain>向 AI 追问</el-button></div>
            </template>
            <template v-else>
              <p>根据你的课程进度、作业与成绩，生成一份本周可执行计划。</p>
              <el-button class="ai-button" :loading="aiPlanning" @click="generatePlan">{{ aiPlanning ? '正在分析…' : '生成学习计划' }}</el-button>
            </template>
          </div>
        </article>
      </section>

      <section class="dashboard-bottom-grid">
        <article class="panel reminder-panel">
          <div class="panel-heading"><h2>作业与考试提醒</h2><a>查看全部</a></div>
          <div class="reminder-row"><StatusTag label="考试" tone="warning" /><span><strong>大学物理 期中测验</strong><small>周五 09:00–10:30 · 教二楼 301</small></span><b>倒计时 2 天</b></div>
          <div class="reminder-row"><StatusTag label="作业" tone="primary" /><span><strong>数据结构 第3章作业</strong><small>今天 23:59 截止提交</small></span><b class="urgent">剩余 6 小时</b></div>
        </article>
        <article id="announcements" class="panel announcement-panel">
          <div class="panel-heading"><h2>学校与课程公告</h2><a>查看全部</a></div>
          <div class="announcement-row"><i></i><span>关于 2026 年暑期学校课程报名的通知</span><small>教务处 · 07-04</small></div>
          <div class="announcement-row"><i></i><span>数据结构课程资料更新（第3章）</span><small>张老师 · 07-03</small></div>
          <div class="announcement-row"><i></i><span>大学英语口语考试安排通知</span><small>外国语学院 · 07-02</small></div>
        </article>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.dashboard-main-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(360px, .9fr); gap: 18px; margin-bottom: 18px; }
.continue-panel { min-height: 284px; }
.continue-content { min-height: 230px; display: grid; grid-template-columns: 150px 1fr auto; align-items: center; gap: 22px; padding: 20px; }
.course-cover { position: relative; width: 150px; height: 160px; display: grid; place-items: center; overflow: hidden; border-radius: 12px; background: linear-gradient(145deg, #0f9b8b, #08736d); color: rgba(255,255,255,.18); font-size: 76px; }
.tree-node { position: absolute; z-index: 2; width: 16px; height: 16px; border: 3px solid #fff; border-radius: 50%; }
.tree-node--top { top: 35px; left: 67px; }.tree-node--left { top: 95px; left: 35px; }.tree-node--right { top: 95px; right: 35px; }
.tree-line { position: absolute; z-index: 1; top: 57px; width: 66px; height: 3px; background: #fff; transform-origin: left center; }
.tree-line--left { left: 43px; transform: rotate(60deg); }.tree-line--right { left: 78px; transform: rotate(120deg); }
.continue-copy h2 { margin: 10px 0 4px; font-size: 24px; }.continue-copy p { margin: 0 0 12px; color: var(--muted); font-size: 14px; }.continue-copy > strong { font-size: 15px; }
.course-progress-line { display: grid; grid-template-columns: 1fr 42px; align-items: center; gap: 12px; margin-top: 22px; font-size: 12px; color: var(--muted); }
.course-meta { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 14px; color: var(--muted); font-size: 12px; }
.task-panel { overflow: hidden; }
.task-row { width: 100%; min-height: 56px; display: grid; grid-template-columns: 54px 1fr 108px 18px; align-items: center; gap: 8px; padding: 9px 14px; border: 0; border-bottom: 1px solid var(--divider); background: #fff; text-align: left; cursor: pointer; }
.task-row:last-child { border-bottom: 0; }.task-row:hover { background: #fafcff; }
.task-copy, .task-time { display: grid; gap: 4px; }.task-copy strong, .task-time strong { font-size: 12px; font-weight: 600; }.task-copy small, .task-time small { color: var(--muted); font-size: 10px; }.task-time { text-align: right; }.task-time.urgent strong, .urgent { color: var(--danger) !important; }.task-arrow { color: var(--weak); font-size: 20px; }
.learning-grid { display: grid; grid-template-columns: .78fr 1.35fr 1.05fr; gap: 18px; margin-bottom: 18px; }
.progress-list { padding: 16px 18px 18px; }.progress-item + .progress-item { margin-top: 15px; }.progress-item > div:first-child { display: flex; justify-content: space-between; margin-bottom: 7px; font-size: 12px; }.progress-item strong { color: var(--muted); font-size: 11px; }
.mini-tabs { display: flex; gap: 4px; padding: 3px; border: 1px solid var(--border); border-radius: 7px; }.mini-tabs button { border: 0; border-radius: 5px; padding: 4px 9px; background: transparent; color: var(--muted); font-size: 11px; cursor: pointer; }.mini-tabs button.active { color: var(--primary); background: var(--primary-soft); }
.chart-legend { display: flex; gap: 18px; padding: 13px 18px 0; font-size: 10px; }.chart-legend span::before { content: ""; display: inline-block; width: 7px; height: 7px; margin-right: 6px; border-radius: 50%; }.chart-legend .teal::before { background: var(--teal); }.chart-legend .blue::before { background: var(--primary); }.chart-legend .orange::before { background: #f59e0b; }
.trend-panel .line-chart { padding: 0 10px 6px; }
.risk-panel { padding: 17px; border-color: #f1c79f; background: #fffdfb; }.risk-title, .risk-title > div { display: flex; align-items: center; justify-content: space-between; gap: 9px; }.risk-title h2 { margin: 0; font-size: 15px; color: #b45309; }.risk-icon { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: #f59e0b; color: #fff; font-weight: 700; }
.risk-summary { display: grid; grid-template-columns: 82px 1fr; gap: 14px; margin: 16px 0; padding: 14px; border-radius: 10px; background: #fff7ed; }.risk-summary > strong { display: grid; place-items: center; border-radius: 8px; background: #ffedd5; color: #c2410c; }.risk-summary ul { margin: 0; padding-left: 18px; color: var(--text-regular); font-size: 11px; line-height: 1.9; }
.ai-plan-block { padding-top: 14px; border-top: 1px solid #f3dfcd; }.ai-plan-title { display: flex; align-items: center; gap: 7px; color: var(--ai); font-size: 13px; }.ai-plan-block p { margin: 8px 0 12px; color: var(--muted); font-size: 11px; line-height: 1.7; }.ai-plan-actions { display: flex; gap: 8px; }
.dashboard-bottom-grid { display: grid; grid-template-columns: 1.35fr .85fr; gap: 18px; }
.reminder-row { min-height: 58px; display: grid; grid-template-columns: 56px 1fr auto; align-items: center; gap: 12px; padding: 9px 18px; border-bottom: 1px solid var(--divider); }.reminder-row:last-child { border-bottom: 0; }.reminder-row > span { display: grid; gap: 4px; }.reminder-row strong { font-size: 12px; }.reminder-row small { color: var(--muted); font-size: 10px; }.reminder-row b { padding: 6px 9px; border-radius: 6px; color: var(--primary); background: var(--primary-soft); font-size: 10px; font-weight: 600; }
.announcement-row { min-height: 43px; display: grid; grid-template-columns: 8px 1fr auto; align-items: center; gap: 9px; padding: 0 16px; border-bottom: 1px solid var(--divider); font-size: 11px; }.announcement-row:last-child { border-bottom: 0; }.announcement-row i { width: 5px; height: 5px; border-radius: 50%; background: var(--primary); }.announcement-row small { color: var(--muted); }
@media (max-width: 1350px) { .learning-grid { grid-template-columns: .8fr 1.25fr; }.risk-panel { grid-column: 1 / -1; }.continue-content { grid-template-columns: 120px 1fr; }.course-cover { width: 120px; height: 145px; }.continue-content > .el-button { grid-column: 2; justify-self: start; }.tree-node--top { left: 52px; }.tree-line--left { left: 28px; }.tree-line--right { left: 63px; } }
@media (max-width: 980px) { .dashboard-main-grid, .dashboard-bottom-grid { grid-template-columns: 1fr; }.learning-grid { grid-template-columns: 1fr; }.risk-panel { grid-column: auto; } }
@media (max-width: 620px) { .continue-content { grid-template-columns: 1fr; }.course-cover { width: 100%; height: 118px; }.continue-content > .el-button { grid-column: auto; width: 100%; }.tree-node, .tree-line { display: none; }.task-row { grid-template-columns: 48px 1fr 84px; }.task-arrow { display: none; }.dashboard-main-grid { grid-template-columns: 1fr; }.reminder-row { grid-template-columns: 50px 1fr; }.reminder-row b { grid-column: 2; justify-self: start; }.risk-summary { grid-template-columns: 1fr; }.risk-summary > strong { min-height: 42px; } }
</style>
