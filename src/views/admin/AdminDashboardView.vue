<script setup lang="ts">
import { ref } from 'vue'
import { Bell, Checked, Collection, DocumentChecked, MagicStick, Plus, Refresh, TrendCharts, User, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppShell from '../../components/layout/AppShell.vue'
import MetricCard from '../../components/common/MetricCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'
import LineTrendChart from '../../components/charts/LineTrendChart.vue'

const period = ref('近 30 天')
const metrics = [
  { label: '系统用户', value: '12,486', detail: '本月新增 326', tone: 'blue' as const, icon: User },
  { label: '运行课程', value: '368', detail: '待审核 12 门', tone: 'teal' as const, icon: Collection },
  { label: '今日活跃', value: '3,842', detail: '活跃率 30.8%', tone: 'orange' as const, icon: TrendCharts },
  { label: '教学任务', value: '8,926', detail: '作业与考试总量', tone: 'purple' as const, icon: DocumentChecked },
]

const pending = [
  { type: '课程审核', title: '人工智能导论', owner: '计算机学院 · 刘老师', time: '12 分钟前', status: '待审核', tone: 'warning' as const },
  { type: '教师认证', title: '赵海峰 教师资格申请', owner: '数学与统计学院', time: '36 分钟前', status: '待核验', tone: 'primary' as const },
  { type: '课程变更', title: '大学物理（下）教学计划调整', owner: '理学院 · 王老师', time: '1 小时前', status: '待审核', tone: 'warning' as const },
  { type: '内容举报', title: '课程论坛帖子 #2048', owner: '数据结构课程论坛', time: '2 小时前', status: '需处理', tone: 'danger' as const },
]

const alerts = [
  { title: '对象存储使用率达到 82%', meta: '建议检查大文件与过期课程资料', level: 'warning' },
  { title: '2 个课程视频转码失败', meta: '最近一次失败发生于 09:32', level: 'danger' },
  { title: '昨日备份任务已完成', meta: '备份大小 18.6 GB，校验通过', level: 'success' },
]
</script>

<template>
  <AppShell role="admin">
    <div class="page-container admin-dashboard">
      <div class="page-title-row">
        <div><h1>系统数据看板</h1><p>全校在线教学运行概览 · 数据更新于今天 10:24</p></div>
        <div class="page-actions"><el-select v-model="period" style="width:130px"><el-option label="近 7 天" value="近 7 天"/><el-option label="近 30 天" value="近 30 天"/><el-option label="本学期" value="本学期"/></el-select><el-button :icon="Refresh" @click="ElMessage.success('数据已刷新')">刷新数据</el-button></div>
      </div>

      <section class="metrics-grid"><MetricCard v-for="item in metrics" :key="item.label" :item="item" /></section>

      <div class="dashboard-charts">
        <section class="panel user-growth">
          <div class="panel-heading"><div><h2>用户活跃趋势</h2><p>学生与教师每日活跃人数</p></div><div class="chart-legend"><span><i class="blue"/>学生</span><span><i class="teal"/>教师</span></div></div>
          <div class="chart-area"><LineTrendChart :values="[2300,2550,2480,2820,3100,2980,3520,3260,3610,3842]" :second-values="[210,230,225,246,268,260,281,277,298,312]" :labels="['6/25','6/27','6/29','7/1','7/3','7/5']" /></div>
        </section>
        <section class="panel role-distribution">
          <div class="panel-heading"><h2>用户角色分布</h2><a>查看用户</a></div>
          <div class="distribution-body"><div class="role-donut"><span><b>12,486</b><small>总用户</small></span></div><div class="role-legend"><div><span><i class="student"/>学生</span><b>10,842 <small>86.8%</small></b></div><div><span><i class="teacher"/>教师</span><b>1,516 <small>12.1%</small></b></div><div><span><i class="admin"/>管理员</span><b>128 <small>1.1%</small></b></div></div></div>
        </section>
      </div>

      <div class="dashboard-middle">
        <section class="panel course-status">
          <div class="panel-heading"><h2>课程状态分布</h2><a>课程管理</a></div>
          <div class="bar-list"><div><span><i class="running"/>进行中</span><b>286</b><em><i style="width:78%"/></em><small>77.7%</small></div><div><span><i class="draft"/>未发布</span><b>42</b><em><i style="width:32%"/></em><small>11.4%</small></div><div><span><i class="pending"/>待审核</span><b>12</b><em><i style="width:18%"/></em><small>3.3%</small></div><div><span><i class="ended"/>已结课</span><b>28</b><em><i style="width:25%"/></em><small>7.6%</small></div></div>
        </section>
        <section class="panel teaching-volume">
          <div class="panel-heading"><h2>本月教学活动</h2><span>对比上月</span></div>
          <div class="volume-grid"><div><span class="volume-icon blue"><el-icon><DocumentChecked /></el-icon></span><p><b>5,286</b><small>作业提交</small><em>↑ 12.6%</em></p></div><div><span class="volume-icon purple"><el-icon><Checked /></el-icon></span><p><b>18,624</b><small>考试记录</small><em>↑ 8.2%</em></p></div><div><span class="volume-icon teal"><el-icon><Collection /></el-icon></span><p><b>46,218h</b><small>学习时长</small><em>↑ 15.4%</em></p></div><div><span class="volume-icon orange"><el-icon><MagicStick /></el-icon></span><p><b>8,931</b><small>AI 辅助次数</small><em>↑ 24.1%</em></p></div></div>
        </section>
        <section class="panel activity-heat">
          <div class="panel-heading"><h2>一周学习活跃度</h2><span>按小时</span></div>
          <div class="heatmap"><div class="heat-labels"><span>周一</span><span>周二</span><span>周三</span><span>周四</span><span>周五</span><span>周六</span><span>周日</span></div><div class="heat-cells"><template v-for="day in 7" :key="day"><i v-for="hour in 8" :key="hour" :class="`level-${(day*hour)%4}`"/></template></div><div class="heat-axis"><span>08:00</span><span>12:00</span><span>16:00</span><span>20:00</span></div></div>
        </section>
      </div>

      <div class="admin-bottom-grid">
        <section class="panel pending-panel">
          <div class="panel-heading"><div><h2>待审核与待处理</h2><p>共 18 项需要管理员处理</p></div><a>查看全部</a></div>
          <div class="pending-table"><div v-for="item in pending" :key="item.title" class="pending-row"><span class="type-icon"><el-icon><DocumentChecked /></el-icon></span><div><strong>{{ item.title }}</strong><small>{{ item.type }} · {{ item.owner }} · {{ item.time }}</small></div><StatusTag :label="item.status" :tone="item.tone"/><button>处理</button></div></div>
        </section>
        <section class="panel alerts-panel">
          <div class="panel-heading"><h2>系统提醒</h2><a>运行监控</a></div>
          <div class="alert-list"><article v-for="item in alerts" :key="item.title" :class="item.level"><span><el-icon v-if="item.level!=='success'"><Warning /></el-icon><el-icon v-else><Checked /></el-icon></span><div><strong>{{ item.title }}</strong><p>{{ item.meta }}</p></div></article></div>
        </section>
      </div>

      <section class="panel admin-quick">
        <div class="panel-heading"><h2>快捷管理</h2><span>常用后台操作</span></div>
        <div class="admin-quick-grid"><button><span><el-icon><Plus /></el-icon></span><b>添加用户</b><small>创建学生或教师账号</small></button><button><span><el-icon><Collection /></el-icon></span><b>课程审核</b><small>12 门课程待审核</small></button><button><span><el-icon><Bell /></el-icon></span><b>发布公告</b><small>面向全校或指定角色</small></button><button><span><el-icon><TrendCharts /></el-icon></span><b>导出统计</b><small>教学数据汇总报表</small></button></div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.panel-heading>div p{margin:3px 0 0;color:var(--muted);font-size:8px}.dashboard-charts{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(300px,.8fr);gap:16px;margin-bottom:16px}.chart-legend{display:flex;gap:13px;color:var(--muted);font-size:9px}.chart-legend span{display:flex;align-items:center;gap:5px}.chart-legend i{width:7px;height:7px;border-radius:50%}.chart-legend i.blue{background:var(--primary)}.chart-legend i.teal{background:var(--teal)}.chart-area{padding:12px 20px 4px}.distribution-body{display:flex;align-items:center;justify-content:center;gap:28px;padding:22px 18px}.role-donut{position:relative;width:128px;height:128px;display:grid;place-items:center;border-radius:50%;background:conic-gradient(var(--primary) 0 86.8%,var(--teal) 86.8% 98.9%,var(--ai) 98.9%)}.role-donut:before{content:"";position:absolute;width:88px;height:88px;border-radius:50%;background:#fff}.role-donut span{z-index:1;text-align:center}.role-donut b,.role-donut small{display:block}.role-donut b{font-size:16px}.role-donut small{margin-top:3px;color:var(--muted);font-size:8px}.role-legend{min-width:145px;display:grid;gap:12px}.role-legend>div{display:flex;justify-content:space-between;gap:15px;font-size:9px}.role-legend span{display:flex;align-items:center;gap:6px;color:var(--muted)}.role-legend i{width:8px;height:8px;border-radius:2px}.role-legend i.student{background:var(--primary)}.role-legend i.teacher{background:var(--teal)}.role-legend i.admin{background:var(--ai)}.role-legend b{font-size:9px}.role-legend small{color:var(--muted);font-weight:400}.dashboard-middle{display:grid;grid-template-columns:1fr 1fr 1.1fr;gap:16px;margin-bottom:16px}.bar-list{display:grid;gap:13px;padding:18px}.bar-list>div{display:grid;grid-template-columns:70px 32px 1fr 34px;align-items:center;gap:8px;font-size:9px}.bar-list span{display:flex;align-items:center;gap:5px;color:var(--muted)}.bar-list span>i{width:7px;height:7px;border-radius:2px}.bar-list i.running{background:var(--primary)}.bar-list i.draft{background:#94a3b8}.bar-list i.pending{background:#f59e0b}.bar-list i.ended{background:var(--teal)}.bar-list em{height:6px;border-radius:99px;background:#edf1f6;overflow:hidden}.bar-list em i{display:block;height:100%;border-radius:99px;background:var(--primary)}.bar-list small{color:var(--muted)}.volume-grid{display:grid;grid-template-columns:1fr 1fr;padding:9px 14px}.volume-grid>div{display:flex;gap:9px;padding:10px;border-right:1px solid var(--divider);border-bottom:1px solid var(--divider)}.volume-grid>div:nth-child(2n){border-right:0}.volume-grid>div:nth-last-child(-n+2){border-bottom:0}.volume-icon{width:32px;height:32px;display:grid;place-items:center;flex:none;border-radius:8px}.volume-icon.blue{color:var(--primary);background:var(--primary-soft)}.volume-icon.purple{color:var(--ai);background:var(--ai-soft)}.volume-icon.teal{color:var(--teal);background:var(--teal-soft)}.volume-icon.orange{color:#d97706;background:var(--orange-soft)}.volume-grid p{margin:0}.volume-grid b,.volume-grid small,.volume-grid em{display:block}.volume-grid b{font-size:13px}.volume-grid small{margin:2px 0;color:var(--muted);font-size:8px}.volume-grid em{color:var(--teal);font-size:7px;font-style:normal}.heatmap{display:grid;grid-template-columns:34px 1fr;gap:6px;padding:15px}.heat-labels{display:grid;grid-template-rows:repeat(7,1fr);gap:4px;color:var(--muted);font-size:7px}.heat-labels span{display:flex;align-items:center}.heat-cells{display:grid;grid-template-columns:repeat(8,1fr);grid-template-rows:repeat(7,13px);grid-auto-flow:column;gap:4px}.heat-cells i{border-radius:2px;background:#eaf0fb}.heat-cells i.level-1{background:#c8d7f5}.heat-cells i.level-2{background:#7ba1e5}.heat-cells i.level-3{background:var(--primary)}.heat-axis{grid-column:2;display:flex;justify-content:space-between;color:var(--weak);font-size:7px}.admin-bottom-grid{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(300px,.8fr);gap:16px;margin-bottom:16px}.pending-row{display:grid;grid-template-columns:34px minmax(0,1fr) auto auto;align-items:center;gap:10px;min-height:62px;padding:9px 15px;border-bottom:1px solid var(--divider)}.pending-row:last-child{border-bottom:0}.type-icon{width:32px;height:32px;display:grid;place-items:center;border-radius:8px;background:var(--primary-soft);color:var(--primary)}.pending-row strong,.pending-row small{display:block}.pending-row strong{font-size:10px}.pending-row small{margin-top:4px;color:var(--muted);font-size:8px}.pending-row>button{border:0;background:transparent;color:var(--primary);font-size:9px;cursor:pointer}.alert-list{padding:6px 14px}.alert-list article{display:flex;gap:10px;padding:11px 0;border-bottom:1px solid var(--divider)}.alert-list article:last-child{border-bottom:0}.alert-list article>span{width:28px;height:28px;display:grid;place-items:center;flex:none;border-radius:7px}.alert-list article.warning>span{color:#d97706;background:var(--orange-soft)}.alert-list article.danger>span{color:var(--danger);background:var(--danger-soft)}.alert-list article.success>span{color:var(--teal);background:var(--teal-soft)}.alert-list strong{font-size:9px}.alert-list p{margin:4px 0 0;color:var(--muted);font-size:8px;line-height:1.5}.admin-quick{margin-bottom:10px}.admin-quick .panel-heading>span{color:var(--muted);font-size:8px}.admin-quick-grid{display:grid;grid-template-columns:repeat(4,1fr);padding:12px}.admin-quick-grid button{display:grid;grid-template-columns:38px 1fr;grid-template-rows:auto auto;column-gap:10px;padding:10px;border:0;border-right:1px solid var(--divider);background:#fff;text-align:left;cursor:pointer}.admin-quick-grid button:last-child{border-right:0}.admin-quick-grid button:hover{background:#f8fafc}.admin-quick-grid button>span{grid-row:1/3;width:38px;height:38px;display:grid;place-items:center;border-radius:9px;background:var(--primary-soft);color:var(--primary);font-size:18px}.admin-quick-grid b{font-size:10px}.admin-quick-grid small{align-self:end;color:var(--muted);font-size:8px}
@media(max-width:1150px){.dashboard-middle{grid-template-columns:1fr 1fr}.activity-heat{grid-column:1/-1}.admin-bottom-grid{grid-template-columns:1fr}.admin-quick-grid{grid-template-columns:1fr 1fr}.admin-quick-grid button:nth-child(2){border-right:0}}@media(max-width:800px){.dashboard-charts{grid-template-columns:1fr}.dashboard-middle{grid-template-columns:1fr}.activity-heat{grid-column:auto}.distribution-body{justify-content:flex-start}.admin-quick-grid{grid-template-columns:1fr}.admin-quick-grid button{border-right:0;border-bottom:1px solid var(--divider)}.page-actions{display:grid;grid-template-columns:1fr 1fr}.page-actions .el-select{width:100%!important}}@media(max-width:520px){.distribution-body{align-items:flex-start;flex-direction:column}.pending-row{grid-template-columns:32px 1fr auto}.pending-row>button{grid-column:2}.pending-row>.status-tag{grid-column:3;grid-row:1}.volume-grid{grid-template-columns:1fr}.volume-grid>div{border-right:0!important;border-bottom:1px solid var(--divider)!important}}
</style>
