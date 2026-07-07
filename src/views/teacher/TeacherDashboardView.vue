<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, CircleCheck, Collection, DataAnalysis, DocumentChecked, EditPen, MagicStick, Plus, Reading, TrendCharts, User } from '@element-plus/icons-vue'
import AppShell from '../../components/layout/AppShell.vue'
import MetricCard from '../../components/common/MetricCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'

const router = useRouter()
const analysisOpen = ref(false)

const metrics = [
  { label: '本学期课程', value: '4', detail: '2 门进行中', tone: 'blue' as const, icon: Collection },
  { label: '授课学生', value: '286', detail: '较上周 +8', tone: 'teal' as const, icon: User },
  { label: '待批改作业', value: '32', detail: '2 项即将截止', tone: 'orange' as const, icon: EditPen },
  { label: '学习风险学生', value: '12', detail: '3 人高风险', tone: 'purple' as const, icon: TrendCharts },
]

const grading = [
  { course: '数据结构', title: '二叉树遍历编程题', submitted: '46 / 52', deadline: '今天 18:00', status: '进行中', tone: 'warning' as const },
  { course: '算法设计与分析', title: '贪心算法案例分析', submitted: '61 / 65', deadline: '7月8日', status: '待批改', tone: 'primary' as const },
  { course: '程序设计基础', title: '指针与链表练习', submitted: '58 / 58', deadline: '已截止', status: '待发布成绩', tone: 'danger' as const },
]

const risks = [
  { name: '王晓雨', course: '数据结构', reason: '连续 3 次未完成章节学习', level: '高风险', tone: 'danger' as const },
  { name: '陈一鸣', course: '算法设计与分析', reason: '近两周作业平均分下降 18%', level: '中风险', tone: 'warning' as const },
  { name: '赵可欣', course: '数据结构', reason: '课程活跃度低于班级均值', level: '需关注', tone: 'info' as const },
]
</script>

<template>
  <AppShell role="teacher">
    <div class="page-container teacher-dashboard">
      <div class="page-title-row">
        <div><h1>上午好，张老师</h1><p>今天有 3 项教学任务待处理，数据结构课程有新的学习风险提醒。</p></div>
        <div class="page-actions"><el-button :icon="Bell">查看通知</el-button><el-button type="primary" :icon="Plus">新建教学任务</el-button></div>
      </div>

      <section class="metrics-grid"><MetricCard v-for="item in metrics" :key="item.label" :item="item" /></section>

      <section class="quick-panel panel">
        <div class="panel-heading"><h2>快捷操作</h2><span>常用教学流程</span></div>
        <div class="quick-grid">
          <button @click="router.push('/teacher/assignments/tree-traversal/grading/li')"><span class="quick-icon blue"><el-icon><EditPen /></el-icon></span><strong>批改作业</strong><small>32 份待处理</small></button>
          <button><span class="quick-icon teal"><el-icon><Plus /></el-icon></span><strong>发布作业</strong><small>创建课程任务</small></button>
          <button @click="router.push('/teacher/exams/data-structures-final/paper')"><span class="quick-icon purple"><el-icon><MagicStick /></el-icon></span><strong>智能组卷</strong><small>AI 推荐试题</small></button>
          <button @click="analysisOpen = true"><span class="quick-icon orange"><el-icon><DataAnalysis /></el-icon></span><strong>学情分析</strong><small>班级学习洞察</small></button>
        </div>
      </section>

      <div class="teacher-main-grid">
        <section class="panel task-panel">
          <div class="panel-heading"><h2>今日待处理</h2><a>查看全部</a></div>
          <div class="today-list">
            <article><span class="task-time"><b>10:00</b><small>上午</small></span><i class="task-line blue"/><div><strong>《数据结构》课程答疑</strong><p>线上答疑室 · 计算机 2301、2302 班</p></div><StatusTag label="即将开始" tone="primary" /></article>
            <article><span class="task-time"><b>14:30</b><small>下午</small></span><i class="task-line orange"/><div><strong>完成二叉树作业批改</strong><p>剩余 32 份 · 截止今天 18:00</p></div><el-button size="small" type="primary" @click="router.push('/teacher/assignments/tree-traversal/grading/li')">去批改</el-button></article>
            <article><span class="task-time"><b>17:00</b><small>下午</small></span><i class="task-line teal"/><div><strong>发布期中考试安排</strong><p>《算法设计与分析》· 65 名学生</p></div><StatusTag label="待发布" tone="warning" /></article>
          </div>
        </section>

        <section class="panel activity-panel">
          <div class="panel-heading"><h2>最近课程动态</h2><a>更多</a></div>
          <div class="activity-list">
            <div><span class="activity-icon blue"><el-icon><DocumentChecked /></el-icon></span><p><b>46 名学生</b>提交了“二叉树遍历编程题”<small>数据结构 · 12 分钟前</small></p></div>
            <div><span class="activity-icon teal"><el-icon><CircleCheck /></el-icon></span><p>“动态规划”章节完成率达到 <b>86%</b><small>算法设计与分析 · 1 小时前</small></p></div>
            <div><span class="activity-icon orange"><el-icon><Reading /></el-icon></span><p><b>8 条新讨论</b>需要教师回复<small>课程论坛 · 3 小时前</small></p></div>
          </div>
        </section>
      </div>

      <section class="panel grading-panel">
        <div class="panel-heading"><h2>待批改作业</h2><a>进入作业管理</a></div>
        <div class="table-wrap">
          <table><thead><tr><th>课程</th><th>作业名称</th><th>提交情况</th><th>截止时间</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in grading" :key="item.title"><td><span class="course-dot"/>{{ item.course }}</td><td><strong>{{ item.title }}</strong></td><td>{{ item.submitted }}</td><td>{{ item.deadline }}</td><td><StatusTag :label="item.status" :tone="item.tone" /></td><td><button class="text-action" @click="router.push('/teacher/assignments/tree-traversal/grading/li')">开始批改</button></td></tr></tbody></table>
        </div>
      </section>

      <section class="panel risk-panel">
        <div class="panel-heading"><div><h2>学生学习风险预警</h2><p>AI 根据进度、成绩与活跃度综合识别</p></div><a>查看完整预警</a></div>
        <div class="risk-grid"><article v-for="item in risks" :key="item.name"><div class="student-avatar">{{ item.name[0] }}</div><div><strong>{{ item.name }}</strong><small>{{ item.course }}</small><p>{{ item.reason }}</p></div><StatusTag :label="item.level" :tone="item.tone" /><button>查看详情</button></article></div>
      </section>
    </div>

    <el-dialog v-model="analysisOpen" title="AI 学情分析" width="560px">
      <div class="analysis-dialog"><span class="ai-analysis-badge"><el-icon><MagicStick /></el-icon>已分析近 30 天课程数据</span><h3>班级整体学习状态稳定</h3><p>数据结构课程平均完成率为 78%，较上周提升 6%。二叉树章节是当前主要难点，建议增加一次针对递归遍历的课堂练习。</p><div class="analysis-points"><div><b>12</b><span>风险学生</span></div><div><b>4.2h</b><span>平均学习时长</span></div><div><b>81.5</b><span>作业平均分</span></div></div></div>
      <template #footer><el-button @click="analysisOpen = false">关闭</el-button><el-button type="primary">生成教学建议</el-button></template>
    </el-dialog>
  </AppShell>
</template>

<style scoped>
.quick-panel{margin-bottom:18px}.quick-panel .panel-heading>span{color:var(--muted);font-size:11px}.quick-grid{display:grid;grid-template-columns:repeat(4,1fr);padding:16px}.quick-grid button{display:flex;align-items:center;gap:11px;min-height:64px;padding:10px 14px;border:0;border-right:1px solid var(--divider);background:#fff;text-align:left;cursor:pointer}.quick-grid button:last-child{border-right:0}.quick-grid button:hover{background:#f8fafc}.quick-icon{width:38px;height:38px;display:grid;place-items:center;flex:none;border-radius:9px;font-size:19px}.quick-icon.blue{color:var(--primary);background:var(--primary-soft)}.quick-icon.teal{color:var(--teal);background:var(--teal-soft)}.quick-icon.purple{color:var(--ai);background:var(--ai-soft)}.quick-icon.orange{color:#d97706;background:var(--orange-soft)}.quick-grid strong,.quick-grid small{display:block}.quick-grid strong{font-size:13px}.quick-grid small{margin-top:4px;color:var(--muted);font-size:10px}.teacher-main-grid{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(300px,.8fr);gap:18px;margin-bottom:18px}.today-list{padding:4px 18px}.today-list article{display:grid;grid-template-columns:60px 4px minmax(0,1fr) auto;align-items:center;gap:13px;min-height:82px;border-bottom:1px solid var(--divider)}.today-list article:last-child{border-bottom:0}.task-time b,.task-time small{display:block}.task-time b{font-size:14px}.task-time small{margin-top:3px;color:var(--muted);font-size:10px}.task-line{width:3px;height:36px;border-radius:99px}.task-line.blue{background:var(--primary)}.task-line.orange{background:#f59e0b}.task-line.teal{background:var(--teal)}.today-list article>div strong{font-size:13px}.today-list article>div p{margin:5px 0 0;color:var(--muted);font-size:10px}.activity-list{padding:4px 18px}.activity-list>div{display:flex;gap:11px;padding:14px 0;border-bottom:1px solid var(--divider)}.activity-list>div:last-child{border-bottom:0}.activity-icon{width:32px;height:32px;display:grid;place-items:center;flex:none;border-radius:8px}.activity-icon.blue{color:var(--primary);background:var(--primary-soft)}.activity-icon.teal{color:var(--teal);background:var(--teal-soft)}.activity-icon.orange{color:#d97706;background:var(--orange-soft)}.activity-list p{margin:0;color:#445168;font-size:11px;line-height:1.5}.activity-list p b{color:var(--text)}.activity-list small{display:block;margin-top:4px;color:var(--weak);font-size:9px}.grading-panel{margin-bottom:18px}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;font-size:12px}th,td{padding:13px 18px;border-bottom:1px solid var(--divider);text-align:left;white-space:nowrap}th{color:var(--muted);background:#fafbfd;font-size:10px;font-weight:600}tbody tr:last-child td{border-bottom:0}td strong{font-weight:600}.course-dot{display:inline-block;width:7px;height:7px;margin-right:8px;border-radius:50%;background:var(--primary)}.text-action{border:0;background:transparent;color:var(--primary);font-size:11px;cursor:pointer}.risk-panel .panel-heading{height:auto;min-height:56px}.risk-panel .panel-heading p{margin:3px 0 0;color:var(--muted);font-size:9px}.risk-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;padding:14px}.risk-grid article{display:grid;grid-template-columns:38px minmax(0,1fr) auto;gap:10px;align-items:start;padding:13px;border:1px solid var(--border);border-radius:9px}.student-avatar{width:38px;height:38px;display:grid;place-items:center;border-radius:50%;background:#e8eefb;color:var(--primary);font-weight:700}.risk-grid strong,.risk-grid small{display:block}.risk-grid strong{font-size:12px}.risk-grid small{margin-top:2px;color:var(--muted);font-size:9px}.risk-grid p{grid-column:1/-1;margin:8px 0 0;color:#556277;font-size:10px}.risk-grid article>button{grid-column:3;border:0;background:transparent;color:var(--primary);font-size:10px;cursor:pointer}.analysis-dialog p{color:#475569;line-height:1.7;font-size:13px}.ai-analysis-badge{display:inline-flex;align-items:center;gap:5px;padding:5px 8px;border-radius:6px;background:var(--ai-soft);color:var(--ai);font-size:10px}.analysis-dialog h3{margin:18px 0 4px}.analysis-points{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:18px}.analysis-points div{padding:12px;border-radius:8px;background:#f7f9fc;text-align:center}.analysis-points b,.analysis-points span{display:block}.analysis-points b{color:var(--primary);font-size:20px}.analysis-points span{margin-top:4px;color:var(--muted);font-size:10px}
@media(max-width:1000px){.teacher-main-grid{grid-template-columns:1fr}.risk-grid{grid-template-columns:1fr}.quick-grid{grid-template-columns:repeat(2,1fr)}.quick-grid button:nth-child(2){border-right:0}.quick-grid button{border-bottom:1px solid var(--divider)}}@media(max-width:600px){.quick-grid{grid-template-columns:1fr}.quick-grid button{border-right:0}.today-list article{grid-template-columns:48px 3px minmax(0,1fr)}.today-list article>:last-child{grid-column:3}.page-actions .el-button{flex:1}.risk-grid article{grid-template-columns:36px 1fr}.risk-grid article>.status-tag{grid-column:2}.risk-grid article>button{grid-column:2}}
</style>
