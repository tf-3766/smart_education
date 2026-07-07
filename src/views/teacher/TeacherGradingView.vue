<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Check, CircleCheck, CopyDocument, Document, Download, MagicStick, Refresh, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppHeader from '../../components/layout/AppHeader.vue'
import StatusTag from '../../components/common/StatusTag.vue'

const router = useRouter()
const selected = ref(0)
const score = ref(86)
const feedback = ref('整体思路正确，递归终止条件清晰。建议补充空树测试，并注意后序遍历中左右子树的调用顺序。')
const aiDraft = ref('该同学能够正确实现二叉树的前序、中序与后序遍历，代码结构清晰，递归边界处理准确。建议进一步补充空树与单结点树的测试用例，并在实验报告中说明三种遍历方式的时间复杂度。')
const generating = ref(false)
const published = ref(false)

const students = [
  { name: '李明', id: '2023010208', time: '今天 14:22', state: '批改中', score: null },
  { name: '王晓雨', id: '2023010216', time: '今天 13:48', state: '待批改', score: null },
  { name: '陈一鸣', id: '2023010221', time: '今天 12:35', state: '待批改', score: null },
  { name: '赵可欣', id: '2023010233', time: '今天 11:19', state: '已批改', score: 92 },
  { name: '周子航', id: '2023010240', time: '昨天 22:41', state: '已批改', score: 88 },
]

const currentStudent = computed(() => students[selected.value])

const generateFeedback = async () => {
  generating.value = true
  await new Promise((resolve) => setTimeout(resolve, 1000))
  aiDraft.value = '完成度较高：三种遍历算法均能正确运行，递归出口与访问顺序处理准确。代码命名清楚、可读性良好。建议增加异常输入测试，并在报告中对递归栈空间复杂度作进一步说明。'
  generating.value = false
}

const applyDraft = () => {
  feedback.value = aiDraft.value
  ElMessage.success('AI 评语已填入教师评语，可继续编辑')
}

const save = () => ElMessage.success('批改草稿已保存')
const publishNext = () => {
  published.value = true
  ElMessage.success('成绩已发布，已切换到下一位学生')
  if (selected.value < students.length - 1) selected.value += 1
}

const copyDraft = async () => {
  await navigator.clipboard?.writeText(aiDraft.value)
  ElMessage.success('评语已复制')
}
</script>

<template>
  <div class="grading-page">
    <AppHeader role="teacher" compact>
      <template #center>
        <div class="grading-context"><button @click="router.push('/teacher/dashboard')"><el-icon><ArrowLeft /></el-icon> 返回作业列表</button><span>数据结构</span><b>/</b><strong>二叉树遍历编程题</strong></div>
      </template>
    </AppHeader>

    <div class="grading-toolbar">
      <div><h1>作业批改</h1><StatusTag label="46 / 52 已提交" tone="primary" /><span>已批改 14 份</span></div>
      <div><el-button @click="save">保存草稿</el-button><el-button type="primary" :icon="Check" @click="publishNext">发布成绩并批改下一份</el-button></div>
    </div>

    <main class="grading-workbench">
      <aside class="student-queue">
        <div class="queue-head"><div><strong>学生列表</strong><small>按提交时间排序</small></div><span>46</span></div>
        <div class="queue-filter"><button class="active">全部</button><button>待批改 32</button><button>已批改 14</button></div>
        <div class="student-list"><button v-for="(item,index) in students" :key="item.id" :class="{ active:selected===index }" @click="selected=index"><span class="student-avatar">{{ item.name[0] }}</span><span><strong>{{ item.name }}</strong><small>{{ item.id }} · {{ item.time }}</small></span><span v-if="item.score" class="student-score">{{ item.score }}</span><i v-else :class="item.state==='批改中'?'processing':''"/></button></div>
        <div class="queue-progress"><span><b>批改进度</b><em>27%</em></span><div class="progress-track"><i /></div></div>
      </aside>

      <section class="submission-view">
        <div class="student-summary">
          <div class="student-large-avatar">{{ currentStudent.name[0] }}</div>
          <div><h2>{{ currentStudent.name }} <small>{{ currentStudent.id }}</small></h2><p>提交于 {{ currentStudent.time }} · 用时 1 小时 26 分</p></div>
          <StatusTag label="按时提交" tone="success" />
        </div>

        <article class="submission-card">
          <div class="submission-card-head"><div><el-icon><Document /></el-icon><div><strong>作业要求</strong><small>完成二叉树三种遍历算法并分析复杂度</small></div></div><button>展开题目</button></div>
          <div class="file-tabs"><button class="active">main.cpp</button><button>实验报告.pdf</button><button>运行结果.txt</button><span/><el-button size="small" :icon="Download">下载全部</el-button></div>
          <div class="code-preview">
            <div class="line-nos">1<br>2<br>3<br>4<br>5<br>6<br>7<br>8<br>9<br>10<br>11<br>12<br>13<br>14<br>15<br>16<br>17<br>18</div>
            <pre><span class="kw">#include</span> &lt;iostream&gt;
<span class="kw">using namespace</span> std;

<span class="kw">struct</span> TreeNode {
  <span class="type">char</span> data;
  TreeNode *left, *right;
};

<span class="type">void</span> preOrder(TreeNode* root) {
  <span class="kw">if</span> (root == <span class="kw">nullptr</span>) <span class="kw">return</span>;
  cout &lt;&lt; root-&gt;data &lt;&lt; <span class="str">" "</span>;
  preOrder(root-&gt;left);
  preOrder(root-&gt;right);
}

<span class="comment">// 中序、后序遍历实现略</span></pre>
            <button class="inline-comment" title="添加行内评语">+</button>
          </div>
          <div class="run-result"><span><el-icon><CircleCheck /></el-icon>编译通过 · 3 / 3 个测试用例通过</span><button>查看运行详情</button></div>
        </article>
      </section>

      <aside class="grading-panel">
        <section class="score-section">
          <div class="section-head"><h3>评分</h3><strong><input v-model.number="score" type="number" min="0" max="100"> / 100</strong></div>
          <div class="rubric-list">
            <label><span><b>功能正确性</b><small>算法结果与测试用例</small></span><input value="48"><em>/ 50</em></label>
            <label><span><b>代码规范</b><small>命名、结构与可读性</small></span><input value="18"><em>/ 20</em></label>
            <label><span><b>复杂度分析</b><small>报告说明是否准确</small></span><input value="12"><em>/ 15</em></label>
            <label><span><b>测试完整性</b><small>边界与异常用例</small></span><input value="8"><em>/ 15</em></label>
          </div>
        </section>

        <section class="feedback-section">
          <div class="section-head"><h3>教师评语</h3><span>{{ feedback.length }} / 500</span></div>
          <textarea v-model="feedback" rows="5" maxlength="500" />
        </section>

        <section class="ai-feedback">
          <div class="ai-feedback-head"><div><span><el-icon><MagicStick /></el-icon></span><div><strong>AI 智能评语</strong><small>基于作业内容与评分生成</small></div></div><StatusTag label="教师确认后使用" tone="ai" /></div>
          <div v-if="generating" class="ai-loading"><i/><i/><i/><p>正在分析代码与评分维度…</p></div>
          <template v-else>
            <p>{{ aiDraft }}</p>
            <div class="ai-actions"><el-button size="small" :icon="CopyDocument" @click="copyDraft">复制</el-button><el-button size="small" :icon="Refresh" @click="generateFeedback">重新生成</el-button><el-button size="small" type="primary" @click="applyDraft">采用评语</el-button></div>
          </template>
        </section>

        <div class="panel-actions"><el-button :icon="MagicStick" class="ai-button" :loading="generating" @click="generateFeedback">生成新评语</el-button><el-button type="primary" :icon="Upload" @click="publishNext">确认并发布</el-button></div>
      </aside>
    </main>
  </div>
</template>

<style scoped>
.grading-page{min-height:100vh;background:#f4f7fb;padding-top:64px}.grading-context{display:flex;align-items:center;gap:10px;color:var(--muted);font-size:12px}.grading-context button{display:flex;align-items:center;gap:5px;border:0;background:transparent;color:var(--primary);cursor:pointer}.grading-context b{color:#c8d0dc}.grading-context strong{color:var(--text-regular)}.grading-toolbar{height:68px;display:flex;align-items:center;justify-content:space-between;padding:0 24px;border-bottom:1px solid var(--border);background:#fff}.grading-toolbar>div{display:flex;align-items:center;gap:12px}.grading-toolbar h1{margin:0;font-size:18px}.grading-toolbar>div:first-child>span:last-child{color:var(--muted);font-size:11px}.grading-workbench{height:calc(100vh - 132px);display:grid;grid-template-columns:230px minmax(480px,1fr) 390px}.student-queue,.grading-panel{background:#fff;overflow:auto}.student-queue{border-right:1px solid var(--border);display:flex;flex-direction:column}.queue-head{display:flex;align-items:center;justify-content:space-between;padding:16px;border-bottom:1px solid var(--divider)}.queue-head strong,.queue-head small{display:block}.queue-head strong{font-size:13px}.queue-head small{margin-top:3px;color:var(--muted);font-size:9px}.queue-head>span{padding:3px 7px;border-radius:99px;background:var(--primary-soft);color:var(--primary);font-size:10px}.queue-filter{display:grid;grid-template-columns:repeat(3,1fr);padding:9px;border-bottom:1px solid var(--divider)}.queue-filter button{padding:6px 2px;border:0;border-radius:6px;background:transparent;color:var(--muted);font-size:9px;cursor:pointer}.queue-filter button.active{background:var(--primary-soft);color:var(--primary);font-weight:600}.student-list{display:grid}.student-list>button{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:8px;min-height:62px;padding:10px 12px;border:0;border-left:3px solid transparent;border-bottom:1px solid var(--divider);background:#fff;text-align:left;cursor:pointer}.student-list>button.active{border-left-color:var(--primary);background:#f4f7ff}.student-avatar{width:34px;height:34px;display:grid;place-items:center;border-radius:50%;background:#e6edfa;color:var(--primary);font-size:11px;font-weight:700}.student-list strong,.student-list small{display:block}.student-list strong{font-size:11px}.student-list small{margin-top:4px;color:var(--muted);font-size:8px}.student-list i{width:7px;height:7px;border-radius:50%;background:#cbd5e1}.student-list i.processing{background:var(--primary);box-shadow:0 0 0 3px #dbe6ff}.student-score{color:var(--teal);font-size:12px;font-weight:700}.queue-progress{margin-top:auto;padding:14px}.queue-progress span{display:flex;justify-content:space-between;margin-bottom:7px;font-size:9px}.queue-progress em{color:var(--primary);font-style:normal}.queue-progress i{display:block;width:27%;height:100%;background:var(--primary);border-radius:99px}.submission-view{min-width:0;overflow:auto;padding:18px 22px 35px}.student-summary{display:flex;align-items:center;gap:11px;margin-bottom:14px}.student-large-avatar{width:42px;height:42px;display:grid;place-items:center;border-radius:50%;background:#dce7fb;color:var(--primary);font-weight:700}.student-summary h2{margin:0;font-size:15px}.student-summary h2 small{margin-left:6px;color:var(--muted);font-size:9px;font-weight:400}.student-summary p{margin:4px 0 0;color:var(--muted);font-size:9px}.student-summary>.status-tag{margin-left:auto}.submission-card{border:1px solid var(--border);border-radius:10px;background:#fff;overflow:hidden}.submission-card-head{display:flex;align-items:center;justify-content:space-between;padding:13px 15px;border-bottom:1px solid var(--divider)}.submission-card-head>div{display:flex;align-items:center;gap:9px}.submission-card-head .el-icon{color:var(--primary);font-size:20px}.submission-card-head strong,.submission-card-head small{display:block}.submission-card-head strong{font-size:12px}.submission-card-head small{margin-top:3px;color:var(--muted);font-size:9px}.submission-card-head button{border:0;background:transparent;color:var(--primary);font-size:9px;cursor:pointer}.file-tabs{display:flex;align-items:center;gap:2px;padding:0 12px;border-bottom:1px solid var(--border);background:#fafbfd}.file-tabs>button:not(.el-button){height:38px;padding:0 13px;border:0;border-bottom:2px solid transparent;background:transparent;color:var(--muted);font-size:10px;cursor:pointer}.file-tabs>button.active{border-bottom-color:var(--primary);color:var(--primary)}.file-tabs>span{flex:1}.code-preview{position:relative;display:grid;grid-template-columns:42px 1fr;min-height:450px;background:#111b2e;color:#dce6f7;overflow:auto}.line-nos{padding:16px 10px;border-right:1px solid #243149;color:#64748b;text-align:right;font:11px/1.75 Consolas,monospace;user-select:none}.code-preview pre{margin:0;padding:16px;font:11px/1.75 Consolas,monospace}.kw{color:#c0a9ff}.type{color:#72d6c5}.str{color:#f2c678}.comment{color:#6e829f}.inline-comment{position:absolute;right:10px;top:183px;width:22px;height:22px;border:1px solid #475a7a;border-radius:5px;background:#263551;color:#a9b9d3;cursor:pointer}.run-result{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-top:1px solid #233149;background:#17243a;color:#72d6a8;font-size:9px}.run-result span{display:flex;align-items:center;gap:6px}.run-result button{border:0;background:transparent;color:#8eb8ff;font-size:9px;cursor:pointer}.grading-panel{border-left:1px solid var(--border);padding-bottom:18px}.grading-panel section{padding:16px;border-bottom:1px solid var(--divider)}.section-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.section-head h3{margin:0;font-size:13px}.section-head>strong{display:flex;align-items:baseline;color:var(--muted);font-size:10px}.section-head>strong input{width:54px;border:0;border-bottom:1px solid var(--primary);color:var(--primary);font-size:22px;font-weight:700;text-align:right;outline:0}.section-head>span{color:var(--weak);font-size:9px}.rubric-list{display:grid;gap:7px}.rubric-list label{display:grid;grid-template-columns:1fr 40px auto;align-items:center;gap:6px;padding:8px 9px;border:1px solid var(--border);border-radius:7px}.rubric-list b,.rubric-list small{display:block}.rubric-list b{font-size:10px}.rubric-list small{margin-top:3px;color:var(--muted);font-size:8px}.rubric-list input{width:40px;padding:4px;border:1px solid var(--border);border-radius:5px;text-align:center;font-size:11px}.rubric-list em{color:var(--muted);font-size:9px;font-style:normal}.feedback-section textarea{width:100%;resize:vertical;border:1px solid var(--border);border-radius:8px;padding:10px;outline:0;color:var(--text-regular);font-size:11px;line-height:1.6}.feedback-section textarea:focus{border-color:var(--primary)}.ai-feedback{margin:14px;border:1px solid #d7d1f8!important;border-radius:10px;background:#fbfaff}.ai-feedback-head{display:flex;align-items:center;justify-content:space-between}.ai-feedback-head>div{display:flex;align-items:center;gap:8px}.ai-feedback-head>div>span{width:30px;height:30px;display:grid;place-items:center;border-radius:7px;background:var(--ai);color:#fff}.ai-feedback-head strong,.ai-feedback-head small{display:block}.ai-feedback-head strong{font-size:11px}.ai-feedback-head small{margin-top:2px;color:var(--muted);font-size:8px}.ai-feedback>p{margin:12px 0;color:#3e4a60;font-size:10px;line-height:1.7}.ai-actions{display:flex;gap:5px}.ai-actions .el-button:last-child{margin-left:auto}.ai-loading{min-height:100px;display:flex;align-items:center;justify-content:center;gap:5px;color:var(--ai)}.ai-loading i{width:6px;height:6px;border-radius:50%;background:var(--ai);animation:blink .8s infinite alternate}.ai-loading i:nth-child(2){animation-delay:.2s}.ai-loading i:nth-child(3){animation-delay:.4s}.ai-loading p{margin-left:6px;font-size:9px}@keyframes blink{to{opacity:.2}}.panel-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:0 14px}.panel-actions .el-button{margin:0}
@media(max-width:1180px){.grading-workbench{grid-template-columns:200px minmax(480px,1fr)}.grading-panel{position:fixed;z-index:5;right:0;top:132px;bottom:0;width:390px;box-shadow:-10px 0 25px rgba(23,32,51,.15)}}@media(max-width:767px){.grading-page{padding-top:56px}.grading-toolbar{height:auto;min-height:92px;align-items:flex-start;flex-direction:column;padding:12px}.grading-toolbar>div:last-child{width:100%}.grading-toolbar .el-button{flex:1}.grading-workbench{height:auto;display:block}.student-queue{display:none}.submission-view{padding:14px}.grading-panel{position:static;width:auto;border-left:0;box-shadow:none}.code-preview{min-height:360px}.grading-context span,.grading-context b{display:none}}
</style>
