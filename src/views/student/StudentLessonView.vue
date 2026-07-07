<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, ChatDotRound, Check, CircleCheck, Collection, Document,
  Download, MagicStick, Paperclip, Position, Reading, VideoPlay
} from '@element-plus/icons-vue'
import AppHeader from '../../components/layout/AppHeader.vue'

const router = useRouter()
const activeChapter = ref(4)
const summaryOpen = ref(false)
const aiOpen = ref(true)
const isGenerating = ref(false)
const question = ref('')
const completed = ref(false)
const answer = ref('二叉树是一种每个结点最多有两个子结点的树结构。前序遍历遵循“根结点 → 左子树 → 右子树”的顺序。图中的遍历结果为 A、B、D、E、C、F。')
const chatBody = ref<HTMLElement>()

const chapters = [
  { no: 1, title: '树的基本概念', meta: '18 分钟', done: true },
  { no: 2, title: '树的存储结构', meta: '22 分钟', done: true },
  { no: 3, title: '二叉树的性质', meta: '25 分钟', done: true },
  { no: 4, title: '二叉树的遍历', meta: '32 分钟', done: false },
  { no: 5, title: '线索二叉树', meta: '20 分钟', done: false },
  { no: 6, title: '树与森林', meta: '28 分钟', done: false },
]

const ask = async (preset?: string) => {
  const value = preset ?? question.value.trim()
  if (!value || isGenerating.value) return
  question.value = ''
  isGenerating.value = true
  await new Promise((resolve) => setTimeout(resolve, 900))
  answer.value = value.includes('复杂度')
    ? '三种深度优先遍历都会访问每个结点一次，因此时间复杂度为 O(n)。递归实现的空间复杂度由树高决定，平均为 O(log n)，最坏为 O(n)。'
    : '可以把遍历理解成“何时访问根结点”：前序先访问根，中序在左右子树之间访问根，后序最后访问根。结合当前课程图例，按箭头顺序逐层判断会更直观。'
  isGenerating.value = false
  await nextTick()
  chatBody.value?.scrollTo({ top: chatBody.value.scrollHeight, behavior: 'smooth' })
}
</script>

<template>
  <div class="lesson-page">
    <AppHeader role="student" compact>
      <template #center>
        <div class="lesson-breadcrumb">
          <button type="button" @click="router.push('/student/dashboard')"><el-icon><ArrowLeft /></el-icon> 返回课程</button>
          <span>数据结构</span><b>/</b><strong>第 4 章 · 二叉树的遍历</strong>
        </div>
      </template>
    </AppHeader>

    <main class="lesson-workbench" :class="{ 'ai-collapsed': !aiOpen }">
      <aside class="chapter-rail">
        <div class="course-mini">
          <span class="course-code">DS</span>
          <div><strong>数据结构</strong><small>计算机科学 · 2026 春</small></div>
        </div>
        <div class="course-progress"><span><b>课程进度</b><em>58%</em></span><div class="progress-track"><i /></div></div>
        <p class="rail-label">第 4 章 · 树与二叉树</p>
        <nav class="chapter-list">
          <button v-for="item in chapters" :key="item.no" type="button" :class="{ active: activeChapter === item.no }" @click="activeChapter = item.no">
            <span class="chapter-state"><el-icon v-if="item.done"><CircleCheck /></el-icon><b v-else>{{ item.no }}</b></span>
            <span><strong>{{ item.title }}</strong><small>{{ item.meta }}</small></span>
          </button>
        </nav>
        <div class="rail-links">
          <button><el-icon><Document /></el-icon>课程资料 <span>8</span></button>
          <button><el-icon><Collection /></el-icon>章节作业 <span class="warn">1</span></button>
          <button><el-icon><Reading /></el-icon>章节测验</button>
        </div>
      </aside>

      <section class="lesson-content">
        <div class="lesson-titlebar">
          <div><span>4.4</span><h1>二叉树的遍历</h1><p>掌握前序、中序和后序遍历的基本思想与实现方式</p></div>
          <div class="lesson-tools">
            <el-button :icon="MagicStick" class="ai-button" @click="summaryOpen = true">知识点摘要</el-button>
            <el-button :type="completed ? 'success' : 'primary'" :icon="completed ? Check : undefined" @click="completed = !completed">{{ completed ? '已完成' : '标记完成' }}</el-button>
          </div>
        </div>

        <article class="content-card video-card">
          <div class="video-placeholder">
            <button class="play-button" aria-label="播放课程视频"><el-icon><VideoPlay /></el-icon></button>
            <div class="tree-visual" aria-label="二叉树示意图">
              <svg viewBox="0 0 600 250" role="img">
                <g stroke="#8ba8df" stroke-width="3"><line x1="300" y1="45" x2="175" y2="112"/><line x1="300" y1="45" x2="425" y2="112"/><line x1="175" y1="112" x2="110" y2="190"/><line x1="175" y1="112" x2="240" y2="190"/><line x1="425" y1="112" x2="490" y2="190"/></g>
                <g fill="#fff" stroke="#3d6fce" stroke-width="3"><circle cx="300" cy="45" r="26"/><circle cx="175" cy="112" r="26"/><circle cx="425" cy="112" r="26"/><circle cx="110" cy="190" r="26"/><circle cx="240" cy="190" r="26"/><circle cx="490" cy="190" r="26"/></g>
                <g fill="#234071" font-size="18" font-weight="700" text-anchor="middle" dominant-baseline="central"><text x="300" y="45">A</text><text x="175" y="112">B</text><text x="425" y="112">C</text><text x="110" y="190">D</text><text x="240" y="190">E</text><text x="490" y="190">F</text></g>
              </svg>
            </div>
            <span class="video-time">12:48 / 18:20</span>
          </div>
        </article>

        <article class="content-card article-card">
          <div class="article-heading"><span class="index-badge">01</span><div><h2>前序遍历</h2><p>Preorder Traversal</p></div></div>
          <p>若二叉树为空，则遍历结束；否则依次访问根结点、前序遍历左子树、前序遍历右子树。</p>
          <div class="formula-row"><span>访问顺序</span><b>根结点</b><i>→</i><b>左子树</b><i>→</i><b>右子树</b></div>
          <div class="code-box"><span>void</span> PreOrder(TreeNode* root) {<br>&nbsp;&nbsp;if (root == nullptr) return;<br>&nbsp;&nbsp;visit(root); <em>// 访问根结点</em><br>&nbsp;&nbsp;PreOrder(root-&gt;left);<br>&nbsp;&nbsp;PreOrder(root-&gt;right);<br>}</div>
        </article>

        <article class="content-card resource-strip">
          <div><span class="file-icon"><el-icon><Paperclip /></el-icon></span><div><strong>本节学习资料</strong><p>二叉树遍历讲义.pdf · 1.8 MB</p></div></div>
          <el-button :icon="Download">下载资料</el-button>
        </article>
      </section>

      <aside v-if="aiOpen" class="ai-panel">
        <div class="ai-panel-head"><div><span><el-icon><MagicStick /></el-icon></span><div><strong>AI 智能答疑</strong><small>基于当前课程资料回答</small></div></div><button aria-label="关闭答疑" @click="aiOpen = false">×</button></div>
        <div ref="chatBody" class="chat-body">
          <div class="ai-welcome"><span><el-icon><ChatDotRound /></el-icon></span><h3>学习遇到问题了吗？</h3><p>我已读取本节课程内容，可以帮你解释知识点、梳理思路。</p></div>
          <div class="quick-questions"><button @click="ask('三种遍历方式有什么区别？')">三种遍历方式有什么区别？</button><button @click="ask('遍历算法的时间复杂度是多少？')">遍历算法的时间复杂度是多少？</button></div>
          <div v-if="answer" class="answer-card"><div class="answer-role"><span>AI</span><strong>课程助教</strong></div><p>{{ answer }}</p><div class="citation"><b>引用资料</b><button>课程讲义 · 第 4.4 节</button><button>视频 08:16 - 10:42</button></div><div class="answer-actions"><button>复制</button><button>有帮助</button><button @click="ask('请换一种方式解释')">重新生成</button></div></div>
          <div v-if="isGenerating" class="generating"><i/><i/><i/><span>正在结合课程资料生成回答…</span></div>
        </div>
        <div class="chat-composer"><textarea v-model="question" rows="2" placeholder="输入你关于本节内容的问题…" @keydown.ctrl.enter.prevent="ask()"/><div><small>AI 回答仅供学习参考</small><button :disabled="!question.trim() || isGenerating" @click="ask()"><el-icon><Position /></el-icon></button></div></div>
      </aside>
      <button v-else class="ai-fab" @click="aiOpen = true"><el-icon><MagicStick /></el-icon><span>AI 答疑</span></button>
    </main>

    <el-drawer v-model="summaryOpen" title="本节知识点摘要" size="420px">
      <div class="summary-drawer"><span class="ai-summary-label"><el-icon><MagicStick /></el-icon>AI 根据课程内容生成</span><h3>二叉树遍历的核心</h3><ul><li>遍历是按某种次序访问树中每个结点一次。</li><li>前、中、后的命名，取决于根结点的访问时机。</li><li>三种递归实现结构相似，时间复杂度均为 O(n)。</li></ul><el-alert title="建议结合图示手动写出访问序列，再对照代码理解递归过程。" type="info" :closable="false" show-icon /></div>
    </el-drawer>
  </div>
</template>

<style scoped>
.lesson-page{min-height:100vh;background:#f4f7fb}.lesson-breadcrumb{min-width:0;display:flex;align-items:center;gap:10px;font-size:13px;color:var(--muted)}.lesson-breadcrumb button{display:flex;align-items:center;gap:6px;border:0;background:transparent;color:var(--primary);cursor:pointer}.lesson-breadcrumb b{color:#c3ccd9}.lesson-breadcrumb strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--text-regular)}
.lesson-workbench{min-height:100vh;padding-top:64px;display:grid;grid-template-columns:260px minmax(520px,1fr) 360px}.lesson-workbench.ai-collapsed{grid-template-columns:260px minmax(520px,1fr)}.chapter-rail,.ai-panel{position:sticky;top:64px;height:calc(100vh - 64px);background:#fff}.chapter-rail{border-right:1px solid var(--border);overflow:auto}.course-mini{display:flex;align-items:center;gap:11px;padding:18px;border-bottom:1px solid var(--divider)}.course-code{width:40px;height:40px;display:grid;place-items:center;border-radius:9px;background:var(--primary);color:#fff;font-weight:700}.course-mini div,.course-mini strong,.course-mini small{display:block}.course-mini strong{font-size:14px}.course-mini small{margin-top:4px;color:var(--muted);font-size:11px}.course-progress{padding:14px 18px}.course-progress span{display:flex;justify-content:space-between;margin-bottom:8px;font-size:11px}.course-progress b{font-weight:600}.course-progress em{color:var(--primary);font-style:normal}.course-progress i{display:block;width:58%;height:100%;background:var(--primary);border-radius:99px}.rail-label{margin:8px 18px;color:var(--muted);font-size:11px;font-weight:700;text-transform:uppercase}.chapter-list{display:grid;padding:0 8px}.chapter-list button{display:flex;align-items:center;gap:10px;min-height:54px;padding:7px 10px;border:0;border-left:3px solid transparent;border-radius:7px;background:transparent;text-align:left;cursor:pointer}.chapter-list button.active{border-left-color:var(--primary);background:var(--primary-soft);color:var(--primary)}.chapter-state{width:24px;height:24px;display:grid;place-items:center;flex:0 0 auto;color:var(--teal)}.chapter-state b{width:22px;height:22px;display:grid;place-items:center;border:1px solid #d5dce8;border-radius:50%;color:var(--muted);font-size:10px}.chapter-list button>span:last-child,.chapter-list strong,.chapter-list small{display:block}.chapter-list strong{font-size:12px;font-weight:600}.chapter-list small{margin-top:4px;color:var(--muted);font-size:10px}.rail-links{margin-top:12px;padding:12px 10px;border-top:1px solid var(--divider);display:grid}.rail-links button{display:flex;align-items:center;gap:9px;height:38px;border:0;background:transparent;color:var(--text-regular);font-size:12px;cursor:pointer}.rail-links button span{margin-left:auto;padding:1px 6px;border-radius:99px;background:#edf2f8;color:var(--muted);font-size:10px}.rail-links button .warn{background:var(--orange-soft);color:var(--orange)}
.lesson-content{min-width:0;padding:26px 30px 60px}.lesson-titlebar{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:18px}.lesson-titlebar>div:first-child>span{color:var(--primary);font-size:12px;font-weight:700}.lesson-titlebar h1{margin:4px 0 5px;font-size:25px}.lesson-titlebar p{margin:0;color:var(--muted);font-size:12px}.lesson-tools{display:flex;gap:8px}.content-card{max-width:920px;margin:0 auto 16px;border:1px solid var(--border);border-radius:12px;background:#fff}.video-placeholder{position:relative;min-height:330px;overflow:hidden;border-radius:11px;background:#10213f}.tree-visual{position:absolute;inset:25px 60px 36px;display:grid;place-items:center}.tree-visual svg{max-width:650px;filter:drop-shadow(0 12px 20px rgba(0,0,0,.22))}.play-button{position:absolute;z-index:2;left:50%;top:50%;width:58px;height:58px;display:grid;place-items:center;transform:translate(-50%,-50%);border:2px solid rgba(255,255,255,.8);border-radius:50%;background:rgba(37,87,214,.9);color:#fff;font-size:26px;cursor:pointer}.video-time{position:absolute;right:14px;bottom:12px;padding:4px 8px;border-radius:5px;background:rgba(0,0,0,.55);color:#fff;font-size:10px}.article-card{padding:25px}.article-heading{display:flex;align-items:center;gap:12px}.article-heading h2{margin:0;font-size:19px}.article-heading p{margin:2px 0 0;color:var(--muted);font-size:11px}.index-badge{width:36px;height:36px;display:grid;place-items:center;border-radius:8px;background:var(--primary-soft);color:var(--primary);font-weight:700}.article-card>p{color:#425168;font-size:14px;line-height:1.8}.formula-row{display:flex;align-items:center;gap:9px;padding:14px;border:1px solid #d9e4fb;border-radius:8px;background:#f6f9ff;font-size:12px}.formula-row span{margin-right:6px;color:var(--muted)}.formula-row b{padding:4px 9px;border-radius:5px;background:#fff;color:var(--primary)}.formula-row i{color:var(--weak);font-style:normal}.code-box{margin-top:16px;padding:18px;border-radius:8px;background:#111c31;color:#dbe6fa;font:12px/1.75 Consolas,monospace}.code-box span{color:#b3a6ff}.code-box em{color:#6fc7a7}.resource-strip{display:flex;align-items:center;justify-content:space-between;padding:16px 18px}.resource-strip>div{display:flex;align-items:center;gap:11px}.resource-strip strong{display:block;font-size:13px}.resource-strip p{margin:4px 0 0;color:var(--muted);font-size:11px}.file-icon{width:36px;height:36px;display:grid;place-items:center;border-radius:8px;background:#fff1f2;color:#e34b5f}
.ai-panel{z-index:3;border-left:1px solid var(--border);display:flex;flex-direction:column}.ai-panel-head{height:70px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;border-bottom:1px solid var(--divider)}.ai-panel-head>div{display:flex;align-items:center;gap:10px}.ai-panel-head>div>span{width:34px;height:34px;display:grid;place-items:center;border-radius:9px;background:var(--ai);color:#fff}.ai-panel-head strong,.ai-panel-head small{display:block}.ai-panel-head strong{font-size:14px}.ai-panel-head small{margin-top:3px;color:var(--muted);font-size:10px}.ai-panel-head>button{border:0;background:transparent;color:var(--muted);font-size:22px;cursor:pointer}.chat-body{flex:1;overflow:auto;padding:18px 16px}.ai-welcome{text-align:center;padding:6px 10px 14px}.ai-welcome>span{width:42px;height:42px;display:grid;place-items:center;margin:auto;border-radius:12px;background:var(--ai-soft);color:var(--ai);font-size:22px}.ai-welcome h3{margin:10px 0 5px;font-size:14px}.ai-welcome p{margin:0;color:var(--muted);font-size:11px;line-height:1.6}.quick-questions{display:grid;gap:7px}.quick-questions button{padding:9px 10px;border:1px solid var(--border);border-radius:8px;background:#fff;color:#475569;text-align:left;font-size:11px;cursor:pointer}.quick-questions button:hover{border-color:#bdb4f1;color:var(--ai)}.answer-card{margin-top:14px;padding:13px;border:1px solid #dcd7fa;border-radius:10px;background:#faf9ff}.answer-role{display:flex;align-items:center;gap:7px}.answer-role span{width:24px;height:24px;display:grid;place-items:center;border-radius:6px;background:var(--ai);color:#fff;font-size:9px}.answer-role strong{font-size:11px}.answer-card>p{color:#35435a;font-size:12px;line-height:1.75}.citation{padding:9px;border-left:3px solid var(--ai);background:#fff}.citation b{display:block;margin-bottom:6px;color:var(--muted);font-size:9px}.citation button{display:block;margin-top:4px;border:0;background:transparent;color:var(--primary);font-size:10px;cursor:pointer}.answer-actions{display:flex;gap:12px;margin-top:10px}.answer-actions button{border:0;background:transparent;color:var(--muted);font-size:10px;cursor:pointer}.generating{display:flex;align-items:center;gap:5px;margin-top:12px;color:var(--muted);font-size:10px}.generating i{width:5px;height:5px;border-radius:50%;background:var(--ai);animation:pulse 1s infinite alternate}.generating i:nth-child(2){animation-delay:.2s}.generating i:nth-child(3){animation-delay:.4s}@keyframes pulse{to{opacity:.25;transform:translateY(-3px)}}.chat-composer{padding:12px;border-top:1px solid var(--divider)}.chat-composer textarea{width:100%;resize:none;border:1px solid var(--border);border-radius:9px;padding:10px;outline:none;font-size:12px}.chat-composer textarea:focus{border-color:var(--ai)}.chat-composer>div{display:flex;align-items:center;justify-content:space-between;margin-top:6px}.chat-composer small{color:var(--weak);font-size:9px}.chat-composer button{width:30px;height:30px;display:grid;place-items:center;border:0;border-radius:7px;background:var(--ai);color:#fff;cursor:pointer}.chat-composer button:disabled{opacity:.4}.ai-fab{position:fixed;right:22px;bottom:22px;display:flex;align-items:center;gap:8px;padding:12px 16px;border:0;border-radius:99px;background:var(--ai);color:#fff;box-shadow:0 8px 24px rgba(109,92,231,.3);cursor:pointer}.summary-drawer{color:var(--text-regular)}.ai-summary-label{display:inline-flex;align-items:center;gap:6px;padding:5px 8px;border-radius:6px;background:var(--ai-soft);color:var(--ai);font-size:11px}.summary-drawer h3{margin:20px 0 10px}.summary-drawer ul{padding-left:20px;line-height:1.9;font-size:13px}
@media(max-width:1199px){.lesson-workbench,.lesson-workbench.ai-collapsed{grid-template-columns:220px minmax(480px,1fr)}.ai-panel{position:fixed;right:0;bottom:0;top:64px;width:360px;box-shadow:-8px 0 24px rgba(23,32,51,.12)}.chapter-rail{width:220px}.lesson-content{padding:22px}.lesson-tools{flex-direction:column}}
@media(max-width:767px){.lesson-breadcrumb span,.lesson-breadcrumb b{display:none}.lesson-workbench,.lesson-workbench.ai-collapsed{display:block;padding-top:56px}.chapter-rail{display:none}.lesson-content{padding:18px 14px}.lesson-titlebar{align-items:flex-start;flex-direction:column}.lesson-tools{width:100%;flex-direction:row}.lesson-tools .el-button{flex:1}.video-placeholder{min-height:230px}.tree-visual{inset:20px}.article-card{padding:18px}.formula-row{flex-wrap:wrap}.ai-panel{top:56px;width:100%}.resource-strip{align-items:flex-start;gap:12px}.app-header--compact :deep(.profile-button){display:none}}
</style>
