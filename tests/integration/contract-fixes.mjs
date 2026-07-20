// 契约修复回归（demo 模式）：验证 demo 层与真实后端形状一致。
// RED→GREEN：先看失败，再改 services/api 使其通过。
import { fileURLToPath } from 'node:url'

const FRONTEND = fileURLToPath(new URL('../../frontend', import.meta.url))
process.env.VITE_API_MODE = 'demo' // 覆盖 .env.local，强制 demo 模式

const storage = new Map()
globalThis.localStorage = {
  getItem: (k) => (storage.has(k) ? storage.get(k) : null),
  setItem: (k, v) => storage.set(k, String(v)),
  removeItem: (k) => storage.delete(k),
  clear: () => storage.clear(),
}
globalThis.window = {
  localStorage: globalThis.localStorage,
  dispatchEvent: () => true,
  addEventListener: () => {},
  setTimeout: (...a) => setTimeout(...a),
  clearTimeout: (...a) => clearTimeout(...a),
}

const { createServer } = await import(`${FRONTEND}/node_modules/vite/dist/node/index.js`)
const server = await createServer({ root: FRONTEND, appType: 'custom', logLevel: 'error', server: { middlewareMode: true, hmr: false } })
const { warningsApi, forumApi, studentLearningApi, resetDemoData } = await server.ssrLoadModule('/src/services/api/index.ts')
resetDemoData()

let failed = 0
function expect(name, cond, detail = '') {
  console.log(`[${cond ? 'PASS' : 'FAIL'}] ${name}${detail ? ` — ${detail}` : ''}`)
  if (!cond) failed++
}

// 1. 预警 VO 的枚举字段应为 CodeLabel（与后端一致），OPEN 标签为「待处理」
const warnings = await warningsApi.teacherList('21001', {})
const w = warnings.records.find((x) => x.warningId === '43001')
expect('预警 warningStatus 是 CodeLabel', w?.warningStatus?.code === 'OPEN', JSON.stringify(w?.warningStatus))
expect('预警 OPEN 标签为待处理', w?.warningStatus?.label === '待处理', JSON.stringify(w?.warningStatus))
expect('预警 warningType 是 CodeLabel', w?.warningType?.code === 'PROGRESS_LAG' && w?.warningType?.label === '学习进度落后', JSON.stringify(w?.warningType))
expect('预警 warningLevel 是 CodeLabel', w?.warningLevel?.code === 'MEDIUM', JSON.stringify(w?.warningLevel))
expect('证据 metricValue 是字符串', typeof w?.evidences?.[0]?.metricValue === 'string', JSON.stringify(w?.evidences?.[0]?.metricValue))

// 2. 论坛 status 应为 CodeLabel（与后端一致）
const topics = await forumApi.studentTopics('21001', {})
const t = topics.records[0]
expect('主题列表 status 是 CodeLabel', t?.status?.code === 'VISIBLE' && t?.status?.label === '可见', JSON.stringify(t?.status))
const topicDetail = await forumApi.topicDetail('41001')
expect('主题详情 status 是 CodeLabel', topicDetail?.status?.code === 'VISIBLE', JSON.stringify(topicDetail?.status))
const replies = await forumApi.listReplies('41001', {})
expect('回复 status 是 CodeLabel', replies.records[0]?.status?.code === 'VISIBLE', JSON.stringify(replies.records[0]?.status))

// 3. 教师论坛详情/回复方法（后端已有 /teacher/forum/topics/{id} 等，契约层需暴露）
expect('teacherTopicDetail 方法存在', typeof forumApi.teacherTopicDetail === 'function')
expect('teacherListReplies 方法存在', typeof forumApi.teacherListReplies === 'function')
expect('teacherCreateReply 方法存在', typeof forumApi.teacherCreateReply === 'function')
if (typeof forumApi.teacherTopicDetail === 'function') {
  const hidden = await forumApi.teacherTopicDetail('41003') // 隐藏帖，属教师2的课程21002
  expect('教师可看隐藏主题详情', hidden?.topicId === '41003' && hidden?.status?.code === 'HIDDEN', JSON.stringify(hidden?.status))
  const reply = await forumApi.teacherCreateReply('41001', { content: '教师回复测试' })
  expect('教师可回帖', reply?.topicId === '41001' && !!reply?.replyId)
  const trs = await forumApi.teacherListReplies('41001', {})
  expect('教师回复列表可用', Array.isArray(trs?.records) && trs.records.some((r) => r.replyId === reply?.replyId))
}

// 4. demo 选课与真实后端对齐：重复选课幂等返回既有 enrollment，而非抛错
const first = await studentLearningApi.enroll('21004')
let secondResult, secondError
try { secondResult = await studentLearningApi.enroll('21004') } catch (e) { secondError = e }
expect('重复选课不抛错(幂等)', !secondError, secondError ? `${secondError.code} ${secondError.message}` : '')
expect('幂等返回同一 enrollment', secondResult?.enrollmentId === first.enrollmentId,
  `first=${first.enrollmentId} second=${secondResult?.enrollmentId}`)

console.log(failed ? `\n${failed} FAILED` : '\nALL PASS')
await server.close()
process.exit(failed ? 1 : 0)
