<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Check, CirclePlus, Delete, MagicStick, Refresh, Setting, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppShell from '../../components/layout/AppShell.vue'
import StatusTag from '../../components/common/StatusTag.vue'

const router = useRouter()
const generating = ref(false)
const generated = ref(true)
const activeDifficulty = ref('中等')
const totalScore = ref(100)
const questions = ref([
  { id: 1, type: '单选题', title: '在一棵完全二叉树中，第 6 层最多包含多少个结点？', knowledge: '二叉树性质', difficulty: '基础', score: 4, selected: true },
  { id: 2, type: '单选题', title: '下列关于图的深度优先遍历说法正确的是？', knowledge: '图的遍历', difficulty: '中等', score: 4, selected: true },
  { id: 3, type: '简答题', title: '比较顺序查找与二分查找的适用条件和时间复杂度。', knowledge: '查找算法', difficulty: '中等', score: 10, selected: true },
  { id: 4, type: '算法题', title: '设计一个算法判断给定二叉树是否为平衡二叉树，并分析复杂度。', knowledge: '树与二叉树', difficulty: '较难', score: 18, selected: true },
])

const selectedScore = computed(() => questions.value.filter((q) => q.selected).reduce((sum, q) => sum + q.score, 0))

const generate = async () => {
  generating.value = true
  generated.value = false
  await new Promise((resolve) => setTimeout(resolve, 1200))
  generated.value = true
  generating.value = false
  ElMessage.success('已根据课程目标生成组卷建议')
}

const replaceQuestion = (id: number) => {
  const item = questions.value.find((q) => q.id === id)
  if (item) item.title = 'AI 已替换：分析递归算法在不同树高下的空间复杂度。'
  ElMessage.success('已替换为同知识点、同难度试题')
}
</script>

<template>
  <AppShell role="teacher">
    <div class="page-container paper-page">
      <div class="paper-back"><button @click="router.push('/teacher/dashboard')"><el-icon><ArrowLeft /></el-icon>返回教师工作台</button><span>考试管理 / 智能组卷</span></div>
      <div class="page-title-row"><div><h1>智能组卷建议</h1><p>设定考试范围与难度，AI 从课程题库中推荐试题，最终试卷由教师确认。</p></div><div class="page-actions"><el-button :icon="View">预览试卷</el-button><el-button type="primary" :icon="Check" @click="ElMessage.success('试卷草稿已保存')">保存试卷草稿</el-button></div></div>

      <div class="paper-layout">
        <aside class="panel paper-config">
          <div class="panel-heading"><h2>组卷条件</h2><el-icon><Setting /></el-icon></div>
          <div class="config-body">
            <label><span>所属课程</span><el-select model-value="数据结构"><el-option label="数据结构" value="数据结构" /></el-select></label>
            <label><span>考试名称</span><el-input model-value="数据结构期末考试" /></label>
            <div class="config-row"><label><span>试卷总分</span><el-input v-model="totalScore"><template #append>分</template></el-input></label><label><span>考试时长</span><el-input model-value="120"><template #append>分钟</template></el-input></label></div>
            <label><span>知识范围</span><div class="knowledge-tags"><StatusTag label="线性表 20%" tone="primary" /><StatusTag label="树 35%" tone="primary" /><StatusTag label="图 25%" tone="primary" /><StatusTag label="查找与排序 20%" tone="primary" /><button>+ 调整</button></div></label>
            <label><span>整体难度</span><div class="difficulty-select"><button v-for="item in ['基础','中等','较难']" :key="item" :class="{active:activeDifficulty===item}" @click="activeDifficulty=item">{{ item }}</button></div></label>
            <label><span>题型结构</span><div class="type-list"><div><b>单选题</b><span>10 题 × 4 分</span></div><div><b>判断题</b><span>10 题 × 2 分</span></div><div><b>简答题</b><span>2 题 × 10 分</span></div><div><b>算法题</b><span>1 题 × 20 分</span></div></div></label>
            <el-checkbox checked>避开近两年已使用试题</el-checkbox><el-checkbox checked>优先覆盖课程重点</el-checkbox>
            <el-button class="generate-button" type="primary" :icon="MagicStick" :loading="generating" @click="generate">{{ generating ? '正在分析题库…' : '生成智能组卷建议' }}</el-button>
            <p class="config-note">AI 仅提供建议，所有试题均需教师确认后进入试卷。</p>
          </div>
        </aside>

        <section class="paper-results">
          <div v-if="generating" class="panel generating-card"><span><el-icon><MagicStick /></el-icon></span><h2>正在生成组卷建议</h2><p>正在匹配 428 道课程试题，校验知识点覆盖、难度梯度与重复率…</p><div class="scan-line" /></div>
          <template v-else-if="generated">
            <div class="panel ai-overview"><div class="ai-overview-main"><span><el-icon><MagicStick /></el-icon></span><div><small>AI 组卷分析</small><h2>建议方案覆盖均衡，可作为试卷初稿</h2><p>已覆盖 8 个核心知识点，难度系数预计 0.68，与课程目标匹配度 92%。</p></div></div><div class="overview-stats"><div><b>23</b><span>推荐试题</span></div><div><b>92%</b><span>目标匹配</span></div><div><b>0.68</b><span>难度系数</span></div></div></div>

            <div class="paper-stats-grid"><article class="panel"><div class="mini-chart donut"><span>100<small>总分</small></span></div><div><strong>题型分布</strong><p><i class="blue"/>客观题 60 分</p><p><i class="purple"/>主观题 40 分</p></div></article><article class="panel"><strong>难度分布</strong><div class="difficulty-bars"><span><b>基础</b><i><em style="width:35%"/></i><small>35%</small></span><span><b>中等</b><i><em style="width:45%"/></i><small>45%</small></span><span><b>较难</b><i><em style="width:20%"/></i><small>20%</small></span></div></article><article class="panel"><strong>知识点覆盖</strong><div class="coverage"><b>8 / 9</b><span>核心知识点</span><small>“散列表冲突处理”覆盖偏低</small></div></article></div>

            <section class="panel question-panel">
              <div class="panel-heading"><div><h2>推荐试题</h2><p>已选 {{ questions.filter(q=>q.selected).length }} 题 · 当前 {{ selectedScore }} 分</p></div><div><el-button size="small" :icon="CirclePlus">从题库添加</el-button><el-button size="small" :icon="Refresh" @click="generate">重新生成方案</el-button></div></div>
              <div class="question-list"><article v-for="(item,index) in questions" :key="item.id" :class="{excluded:!item.selected}"><el-checkbox v-model="item.selected" /><span class="question-no">{{ index+1 }}</span><div class="question-main"><div><StatusTag :label="item.type" tone="info" /><StatusTag :label="item.difficulty" :tone="item.difficulty==='较难'?'warning':'neutral'" /><span>{{ item.knowledge }}</span></div><h3>{{ item.title }}</h3><small>来源：课程题库 · 最近使用：未使用</small></div><strong>{{ item.score }} 分</strong><div class="question-actions"><button @click="replaceQuestion(item.id)"><el-icon><Refresh /></el-icon>替换</button><button @click="item.selected=false"><el-icon><Delete /></el-icon>排除</button></div></article></div>
            </section>
          </template>
        </section>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.paper-back{display:flex;align-items:center;gap:12px;margin-bottom:16px;color:var(--muted);font-size:11px}.paper-back button{display:flex;align-items:center;gap:5px;border:0;background:transparent;color:var(--primary);cursor:pointer}.paper-layout{display:grid;grid-template-columns:330px minmax(0,1fr);gap:18px;align-items:start}.paper-config{position:sticky;top:84px}.paper-config .panel-heading>.el-icon{color:var(--muted)}.config-body{display:grid;gap:16px;padding:17px}.config-body label>span{display:block;margin-bottom:7px;color:var(--text-regular);font-size:10px;font-weight:600}.config-body .el-select{width:100%}.config-row{display:grid;grid-template-columns:1fr 1fr;gap:9px}.knowledge-tags{display:flex;flex-wrap:wrap;gap:6px}.knowledge-tags button{border:1px dashed #a9b5c7;border-radius:6px;background:#fff;color:var(--primary);font-size:9px;cursor:pointer}.difficulty-select{display:grid;grid-template-columns:repeat(3,1fr);padding:3px;border-radius:8px;background:#f1f4f8}.difficulty-select button{height:30px;border:0;border-radius:6px;background:transparent;color:var(--muted);font-size:10px;cursor:pointer}.difficulty-select button.active{background:#fff;color:var(--primary);box-shadow:0 1px 4px rgba(23,32,51,.1);font-weight:600}.type-list{display:grid;gap:1px;border:1px solid var(--border);border-radius:8px;overflow:hidden}.type-list div{display:flex;justify-content:space-between;padding:9px;background:#fafbfd;font-size:10px}.type-list span{color:var(--muted)}.generate-button{width:100%;height:40px;margin-top:3px;background:var(--ai);border-color:var(--ai)}.generate-button:hover{background:var(--ai-dark);border-color:var(--ai-dark)}.config-note{margin:0;color:var(--weak);font-size:8px;text-align:center}.paper-results{display:grid;gap:14px;min-width:0}.generating-card{min-height:460px;display:grid;place-content:center;justify-items:center;text-align:center}.generating-card>span{width:58px;height:58px;display:grid;place-items:center;border-radius:15px;background:var(--ai-soft);color:var(--ai);font-size:28px}.generating-card h2{margin:18px 0 4px}.generating-card p{color:var(--muted);font-size:11px}.scan-line{width:260px;height:4px;margin-top:18px;border-radius:99px;background:linear-gradient(90deg,var(--ai-soft),var(--ai),var(--ai-soft));background-size:200% 100%;animation:scan 1s linear infinite}@keyframes scan{to{background-position:-200% 0}}.ai-overview{display:flex;align-items:center;justify-content:space-between;padding:18px;border-color:#d8d1fa;background:#fbfaff}.ai-overview-main{display:flex;align-items:center;gap:13px}.ai-overview-main>span{width:44px;height:44px;display:grid;place-items:center;border-radius:11px;background:var(--ai);color:#fff;font-size:22px}.ai-overview small{color:var(--ai);font-size:9px;font-weight:700}.ai-overview h2{margin:3px 0;font-size:15px}.ai-overview p{margin:0;color:var(--muted);font-size:9px}.overview-stats{display:flex;gap:25px}.overview-stats div{text-align:center}.overview-stats b,.overview-stats span{display:block}.overview-stats b{color:var(--ai);font-size:18px}.overview-stats span{margin-top:3px;color:var(--muted);font-size:8px}.paper-stats-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.paper-stats-grid>article{min-height:126px;padding:15px;display:flex;align-items:center;gap:14px}.paper-stats-grid article>strong{align-self:flex-start;font-size:11px}.mini-chart.donut{width:75px;height:75px;display:grid;place-items:center;flex:none;border-radius:50%;background:conic-gradient(var(--primary) 0 60%,var(--ai) 60%)}.mini-chart.donut:before{content:"";position:absolute;width:55px;height:55px;border-radius:50%;background:#fff}.mini-chart.donut span{z-index:1;text-align:center;font-size:14px;font-weight:700}.mini-chart.donut small{display:block;color:var(--muted);font-size:7px}.paper-stats-grid p{margin:8px 0 0;color:var(--muted);font-size:8px}.paper-stats-grid p i{display:inline-block;width:7px;height:7px;margin-right:5px;border-radius:2px}.paper-stats-grid p i.blue{background:var(--primary)}.paper-stats-grid p i.purple{background:var(--ai)}.difficulty-bars{width:100%;display:grid;gap:10px}.difficulty-bars span{display:grid;grid-template-columns:32px 1fr 26px;align-items:center;gap:7px;font-size:8px}.difficulty-bars i{height:5px;border-radius:99px;background:#edf1f6;overflow:hidden}.difficulty-bars em{display:block;height:100%;border-radius:99px;background:var(--primary)}.difficulty-bars small{color:var(--muted)}.coverage{width:100%;text-align:center}.coverage b,.coverage span,.coverage small{display:block}.coverage b{color:var(--teal);font-size:24px}.coverage span{margin:4px;color:var(--muted);font-size:8px}.coverage small{margin-top:8px;color:var(--orange);font-size:8px}.question-panel .panel-heading{height:auto;min-height:56px}.question-panel .panel-heading p{margin:3px 0 0;color:var(--muted);font-size:8px}.question-list{display:grid}.question-list article{display:grid;grid-template-columns:auto 25px minmax(0,1fr) auto;align-items:start;gap:9px;padding:14px 16px;border-bottom:1px solid var(--divider)}.question-list article:last-child{border-bottom:0}.question-list article.excluded{opacity:.45;background:#fafbfd}.question-no{width:23px;height:23px;display:grid;place-items:center;border-radius:6px;background:var(--primary-soft);color:var(--primary);font-size:9px;font-weight:700}.question-main>div{display:flex;align-items:center;gap:5px}.question-main>div>span:last-child{color:var(--muted);font-size:8px}.question-main h3{margin:7px 0 5px;font-size:11px;font-weight:600}.question-main small{color:var(--weak);font-size:8px}.question-list article>strong{font-size:11px;white-space:nowrap}.question-actions{grid-column:3/-1;display:flex;gap:8px}.question-actions button{display:flex;align-items:center;gap:4px;border:0;background:transparent;color:var(--muted);font-size:8px;cursor:pointer}.question-actions button:hover{color:var(--primary)}
@media(max-width:1050px){.paper-layout{grid-template-columns:1fr}.paper-config{position:static}.config-body{grid-template-columns:repeat(2,1fr)}.config-body>.generate-button,.config-body>.config-note,.config-body>.el-checkbox{grid-column:1/-1}.paper-stats-grid{grid-template-columns:1fr 1fr}.paper-stats-grid article:last-child{grid-column:1/-1}}@media(max-width:700px){.config-body{grid-template-columns:1fr}.config-body>*{grid-column:1!important}.paper-stats-grid{grid-template-columns:1fr}.paper-stats-grid article:last-child{grid-column:auto}.ai-overview{align-items:flex-start;flex-direction:column;gap:16px}.overview-stats{width:100%;justify-content:space-around}.question-panel .panel-heading{align-items:flex-start;flex-direction:column;padding:12px}.question-list article{grid-template-columns:auto 22px minmax(0,1fr)}.question-list article>strong{grid-column:3}.question-actions{grid-column:3}.page-actions{display:grid;grid-template-columns:1fr 1fr}}
</style>
