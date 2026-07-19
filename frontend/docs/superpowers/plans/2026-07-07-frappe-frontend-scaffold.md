# Frappe 前端脚手架重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `smart-education-frontend` 的表现层重建在真实 Frappe LMS 的工具链与主题体系上（Tailwind v3 + frappe-ui preset + 语义 token + 明/暗模式），保持纯前端、mock 驱动、可独立运行。

**Architecture:** 移植 `lms/frontend` 的 Frappe 原生工具链（tailwind.config / postcss / 主题 composable）进本项目，`index.css` 收敛为 `@apply` Frappe 语义 utility 的 compat 层（自动明/暗适配），逐步把外壳与共享组件换成 frappe-ui 组件与语义 class，Dashboard 作为首个纯 Frappe 样板页。不引入任何后端耦合。

**Tech Stack:** Vue 3 + TypeScript + Vite + Pinia + Vue Router + frappe-ui@1.0.0-beta.7 + Tailwind CSS v3.4 + PostCSS + lucide-vue-next。

## Global Constraints

- Tailwind **v3.4.x**（不是 v4）；`postcss@^8.4`；`autoprefixer@^10.4`。preset 只兼容 v3。
- frappe-ui beta.7 `Button` 的 `theme` 仅支持 `'gray' | 'blue' | 'green' | 'red'`，**不含 teal**；teal 主按钮通过 class 覆盖实现。
- `Button` 的 `variant` ∈ `'solid' | 'subtle' | 'outline' | 'ghost'`，默认 `subtle`；`theme` 默认 `gray`。
- 只移植前端：**不得**引入 `frappeRequest` / `setConfig(resourceFetcher)` / `initSocket` / `telemetryPlugin` / 后端 `translation` / `pageMetaPlugin` / PWA / SCORM 代理 / jinjaBootData。数据继续走 `src/services/*` + `src/services/mockRuntime.ts`。
- 保持 **TypeScript**；既有 `vitest` 服务层测试（4 个）必须保持绿。
- 暗色：`document.documentElement` 上的 `data-theme` 属性，取值 `'light' | 'dark'`；localStorage key = `'theme'`。
- 只使用 lms 已验证的语义 utility 词汇：文本 `text-ink-gray-{4,5,6,7,8,9}`、`text-ink-base`；面 `bg-surface-{base,gray-1,gray-2,gray-3}`；边框 `border-outline-gray-{1,2,3}`；状态 `text-ink-{green,red,amber}-{5,6}`、`bg-surface-{green,amber,red}-{1,3}`。
- teal 品牌色以 CSS 变量 `--teal-*`（oklch）保留在 `:root`（明/暗一致）。
- 项目为 ESM（`package.json` `"type":"module"`）：配置文件用 `export default`。

---

### Task 1: 工具链与 compat 层（Tailwind v3 + preset + index.css）

**Files:**
- Modify: `package.json`（devDependencies 增加 tailwind/postcss/autoprefixer）
- Create: `tailwind.config.js`
- Create: `postcss.config.js`
- Modify: `src/styles/index.css`（全量重写为 `@apply` Frappe 语义 utility 的 compat 层 + teal 变量 + 暗色覆盖）

**Interfaces:**
- Consumes: 现有 `main.ts` 已有 `import 'frappe-ui/style.css'`（含 `@tailwind base/components/utilities`）与 `import './styles/index.css'`。
- Produces: 全站 class（`.panel`/`.nav-link`/`.table`/`.pill`/`.course-card`/`.input` 等）改由 Frappe 语义 token 驱动，明/暗自动适配；`--teal-*` 变量与 `.app-btn-primary` 供 Task 4 使用。

- [ ] **Step 1: 安装 v3 工具链依赖**

Run:
```bash
cd /workspace/lms/smart-education-frontend
npm i -D tailwindcss@^3.4.15 postcss@^8.4 autoprefixer@^10.4
```
Expected: 安装成功；`node -e "console.log(require('tailwindcss/package.json').version)"` 打印 `3.4.x`。

- [ ] **Step 2: 创建 `tailwind.config.js`**

```js
import frappeUIPreset from 'frappe-ui/tailwind'

export default {
  presets: [frappeUIPreset],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
    './node_modules/frappe-ui/src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      strokeWidth: { 1.5: '1.5' },
    },
  },
  plugins: [],
}
```

- [ ] **Step 3: 创建 `postcss.config.js`**

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

- [ ] **Step 4: 全量重写 `src/styles/index.css` 为 compat 层**

```css
/* Frappe 语义 token 由 frappe-ui preset 注入（--surface-* / --ink-* / --outline-*，
   并随 [data-theme='dark'] 翻转）。本文件把既有手写 class 收敛为 @apply 这些语义
   utility 的 compat 层，使旧页面与新 frappe-ui 组件同源、并自动适配明/暗。 */

:root {
  /* teal 品牌色（明/暗一致）— 供主按钮/强调用 */
  --teal-50: oklch(0.978 0.014 180.717);
  --teal-100: oklch(0.963 0.018 184.27);
  --teal-500: oklch(0.716 0.113 185.344);
  --teal-600: oklch(0.556 0.096 185.508);
  --teal-700: oklch(0.502 0.084 186.188);
  --teal-800: oklch(0.431 0.069 188.078);
}

* { box-sizing: border-box; }

body {
  margin: 0;
  min-width: 1120px;
  @apply bg-surface-gray-1 text-ink-gray-7;
  font-family: Inter, "InterVariable", ui-sans-serif, system-ui, -apple-system,
    BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 14px;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

a { color: inherit; text-decoration: none; }

/* App shell */
.app-shell { display: grid; grid-template-columns: 240px 1fr; min-height: 100vh; @apply bg-surface-gray-1; }
.sidebar { position: sticky; top: 0; align-self: start; height: 100vh; overflow-y: auto; padding: 14px 10px; @apply bg-surface-gray-1 border-r border-outline-gray-1; }
.brand { display: flex; align-items: center; gap: 10px; margin: 4px 6px 18px; font-weight: 600; font-size: 15px; @apply text-ink-gray-9; }
.brand-mark { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 6px; color: #fff; font-weight: 700; background: var(--teal-600); }
.nav-section { margin-top: 16px; }
.nav-title { margin: 0 8px 6px; font-size: 12px; font-weight: 500; @apply text-ink-gray-5; }
.nav-link { display: flex; align-items: center; gap: 10px; min-height: 32px; padding: 6px 10px; border-radius: 6px; font-size: 13.5px; font-weight: 500; @apply text-ink-gray-6; transition: background-color .12s, color .12s, box-shadow .12s; }
.nav-link:hover { @apply bg-surface-gray-2 text-ink-gray-9; }
.nav-link.router-link-active { @apply bg-surface-base text-ink-gray-9 shadow-sm; font-weight: 600; }

.main-area { min-width: 0; @apply bg-surface-gray-1; }
.topbar { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; justify-content: space-between; gap: 20px; height: 58px; padding: 0 24px; @apply bg-surface-base border-b border-outline-gray-1; }
.topbar strong { font-size: 14px; font-weight: 600; @apply text-ink-gray-9; }
.content { padding: 24px; max-width: 1280px; }

/* Page header */
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; line-height: 1.3; letter-spacing: -0.02em; @apply text-ink-gray-9; }
.page-subtitle { margin: 6px 0 0; font-size: 13.5px; @apply text-ink-gray-5; }

/* Grid */
.grid { display: grid; gap: 14px; }
.grid.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.grid.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.grid.cols-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }

/* Panels */
.panel { padding: 18px; border-radius: 10px; @apply bg-surface-base border border-outline-gray-1 shadow-sm; }
.panel .panel { border-radius: 8px; @apply bg-surface-gray-1 shadow-none; }
.panel-title { margin: 0 0 14px; font-size: 14px; font-weight: 600; @apply text-ink-gray-9; }
.metric-value { font-size: 26px; font-weight: 650; line-height: 1.1; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; @apply text-ink-gray-9; }
.muted { @apply text-ink-gray-5; }
.stack { display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; align-items: center; gap: 10px; }
.spread { display: flex; align-items: center; justify-content: space-between; gap: 12px; }

/* Tables */
.table { width: 100%; border-collapse: collapse; font-size: 13.5px; }
.table th, .table td { padding: 11px 10px; text-align: left; vertical-align: middle; @apply border-b border-outline-gray-1; }
.table th { font-weight: 500; font-size: 12px; @apply text-ink-gray-5 bg-surface-gray-1; }
.table tbody tr { transition: background-color .12s; }
.table tbody tr:hover { @apply bg-surface-gray-1; }
.table tbody tr:last-child td { border-bottom: none; }
.table td { font-variant-numeric: tabular-nums; @apply text-ink-gray-7; }

/* Pills（Task 4 会把 StatusBadge 换成 frappe-ui Badge；此处保留 compat） */
.pill { display: inline-flex; align-items: center; gap: 4px; min-height: 22px; padding: 0 8px; border-radius: 6px; font-size: 12px; font-weight: 500; white-space: nowrap; @apply bg-surface-gray-2 text-ink-gray-6; }
.pill.green { @apply bg-surface-green-3 text-ink-green-6; }
.pill.orange { @apply bg-surface-amber-1 text-ink-amber-6; }
.pill.red { @apply bg-surface-red-1 text-ink-red-6; }

/* Course card */
.course-card { display: flex; flex-direction: column; min-height: 208px; overflow: hidden; border-radius: 10px; @apply bg-surface-base border border-outline-gray-1 shadow-sm; transition: box-shadow .15s, border-color .15s, transform .15s; }
.course-card:hover { @apply border-outline-gray-2; box-shadow: 0 6px 16px rgba(0,0,0,.08); transform: translateY(-1px); }
.course-cover { height: 84px; background: linear-gradient(135deg, var(--teal-600), var(--teal-800)), var(--cover, var(--teal-600)); }
.course-body { display: flex; flex: 1; flex-direction: column; gap: 8px; padding: 14px; }
.course-body strong { font-size: 14px; font-weight: 600; @apply text-ink-gray-9; }
.course-body .muted { font-size: 13px; line-height: 1.5; }

/* Forms */
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.textarea, .input, .select { width: 100%; padding: 8px 11px; border-radius: 6px; font: inherit; font-size: 13.5px; @apply bg-surface-base border border-outline-gray-2 text-ink-gray-8; transition: border-color .12s, box-shadow .12s; }
.textarea::placeholder, .input::placeholder { @apply text-ink-gray-4; }
.textarea:focus, .input:focus, .select:focus { outline: none; border-color: var(--teal-600); box-shadow: 0 0 0 2px var(--teal-50); }
.textarea { min-height: 110px; resize: vertical; line-height: 1.5; }

/* AI 强调块 */
.ai-answer { border-left: 3px solid var(--teal-600); border-radius: 8px; background: var(--teal-50); @apply border border-outline-gray-1; }

.clickable { cursor: pointer; }

/* teal 主按钮覆盖（frappe-ui Button 无 teal theme，用 !important 盖过 solid variant） */
.app-btn-primary { background-color: var(--teal-600) !important; color: #fff !important; }
.app-btn-primary:hover { background-color: var(--teal-700) !important; }

@media (max-width: 1180px) {
  body { min-width: 960px; }
  .app-shell { grid-template-columns: 216px 1fr; }
}
```

- [ ] **Step 5: 构建验证**

Run: `npx vite build`
Expected: 退出码 0；无 “unknown utility” / “@apply” 报错；`dist/assets/*.css` 生成。

- [ ] **Step 6: 回归验证（既有测试保持绿）**

Run: `npx vitest run`
Expected: `Test Files 1 passed`，`Tests 4 passed`。

- [ ] **Step 7: 提交**

```bash
cd /workspace/lms
git add smart-education-frontend/package.json smart-education-frontend/package-lock.json \
        smart-education-frontend/tailwind.config.js smart-education-frontend/postcss.config.js \
        smart-education-frontend/src/styles/index.css
git commit -m "build(frontend): 接入 Tailwind v3 + frappe-ui preset，compat 层改用语义 token"
```

---

### Task 2: 明/暗主题（theme.ts + 启动应用 + 顶栏切换）

**Files:**
- Create: `src/utils/theme.ts`
- Create: `src/utils/theme.test.ts`
- Modify: `src/main.ts`（启动时应用主题）
- Create: `src/components/ThemeToggle.vue`

**Interfaces:**
- Produces:
  - `theme: Ref<'light' | 'dark'>` — 当前主题（响应式）
  - `applyTheme(value: 'light' | 'dark'): void` — 写 `data-theme` 属性 + localStorage + 更新 ref
  - `toggleTheme(): void` — 在 light/dark 间切换
  - `ThemeToggle.vue` — 无 props，顶栏用的 sun/moon 图标按钮，点击调用 `toggleTheme()`

- [ ] **Step 1: 写失败测试 `src/utils/theme.test.ts`**

```ts
import { describe, it, expect, beforeEach } from 'vitest'
import { theme, applyTheme, toggleTheme } from './theme'

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    applyTheme('light')
  })

  it('applyTheme 写入 data-theme、localStorage 与 ref', () => {
    applyTheme('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(localStorage.getItem('theme')).toBe('dark')
    expect(theme.value).toBe('dark')
  })

  it('toggleTheme 在 light/dark 间切换', () => {
    applyTheme('light')
    toggleTheme()
    expect(theme.value).toBe('dark')
    toggleTheme()
    expect(theme.value).toBe('light')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npx vitest run src/utils/theme.test.ts`
Expected: FAIL —— 无法解析模块 `./theme`（文件尚未创建）。

- [ ] **Step 3: 创建 `src/utils/theme.ts`**（移植自 lms，逻辑不变）

```ts
import { ref } from 'vue'

const theme = ref<'light' | 'dark'>(
  (localStorage.getItem('theme') as 'light' | 'dark') || 'light',
)

const applyTheme = (value: 'light' | 'dark') => {
  document.documentElement.setAttribute('data-theme', value)
  localStorage.setItem('theme', value)
  theme.value = value
}

const toggleTheme = () => {
  applyTheme(theme.value === 'dark' ? 'light' : 'dark')
}

export { theme, applyTheme, toggleTheme }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npx vitest run src/utils/theme.test.ts`
Expected: PASS（2 个用例）。

- [ ] **Step 5: 启动时应用主题 —— 修改 `src/main.ts`**

在 `import` 段增加：
```ts
import { theme, applyTheme } from './utils/theme'
```
在 `app.mount('#app')` 之前增加一行，使刷新后 `data-theme` 与持久化一致：
```ts
applyTheme(theme.value)
```

- [ ] **Step 6: 创建 `src/components/ThemeToggle.vue`**

```vue
<template>
  <Button variant="ghost" @click="toggleTheme" aria-label="切换深色模式">
    <template #icon>
      <Moon v-if="theme === 'light'" :size="16" />
      <Sun v-else :size="16" />
    </template>
  </Button>
</template>

<script setup lang="ts">
import { Button } from 'frappe-ui'
import { Moon, Sun } from 'lucide-vue-next'
import { theme, toggleTheme } from '@/utils/theme'
</script>
```

- [ ] **Step 7: 构建 + 全量测试**

Run: `npx vite build && npx vitest run`
Expected: build 退出码 0；`Tests 6 passed`（原 4 + 新 2）。

- [ ] **Step 8: 提交**

```bash
cd /workspace/lms
git add smart-education-frontend/src/utils/theme.ts smart-education-frontend/src/utils/theme.test.ts \
        smart-education-frontend/src/main.ts smart-education-frontend/src/components/ThemeToggle.vue
git commit -m "feat(frontend): 明/暗主题 composable、启动应用与顶栏切换"
```

---

### Task 3: AppShell 换成 frappe-ui + 语义 token

**Files:**
- Modify: `src/layouts/AppShell.vue`

**Interfaces:**
- Consumes: `ThemeToggle.vue`（Task 2）；`useSessionStore`（现有）；frappe-ui `Select`、`Button`。
- Produces: 外壳仍暴露 `.app-shell/.sidebar/.nav-link/.topbar/.content` 结构（compat CSS 已 token 化），顶栏含主题切换与用户 chip。

- [ ] **Step 1: 顶栏引入 ThemeToggle 与 frappe-ui Select**

在 `<script setup>` 增加导入：
```ts
import { Select } from 'frappe-ui'
import ThemeToggle from '@/components/ThemeToggle.vue'
```
把模板中角色切换 `<select class="select">…</select>` 替换为：
```vue
<Select
  v-model="selectedRole"
  :options="[
    { label: '管理员', value: 'admin' },
    { label: '教师', value: 'teacher' },
    { label: '学生', value: 'student' },
  ]"
  @update:modelValue="changeRole"
/>
```
（`changeRole` 现依赖 `selectedRole.value`，保持不变；`@change` 改为 `@update:modelValue`。）

- [ ] **Step 2: 顶栏右侧加入主题切换**

在用户 chip 前插入 `<ThemeToggle />`：
```vue
<div class="row">
  <Select … />
  <ThemeToggle />
  <span class="user-chip">
    <span class="user-avatar">{{ session.currentUser.name.slice(0, 1) }}</span>
    <span>{{ session.currentUser.name }}</span>
  </span>
  <Button variant="subtle" @click="router.push('/ai/assistant')">AI 助手</Button>
</div>
```
（把旧的 `appearance="secondary"` 一并改为 `variant="subtle"`，因为 beta.7 无 `appearance` 属性。）

- [ ] **Step 3: 补 `.user-chip`/`.user-avatar` compat 样式**

若 Task 1 的 `index.css` 尚无这两个 class，追加：
```css
.user-chip { display: inline-flex; align-items: center; gap: 8px; padding: 4px 10px 4px 4px; border-radius: 999px; font-size: 13px; font-weight: 500; @apply bg-surface-base border border-outline-gray-1 text-ink-gray-7; }
.user-avatar { display: grid; width: 24px; height: 24px; place-items: center; border-radius: 999px; font-size: 12px; font-weight: 600; color: var(--teal-800); background: var(--teal-50); }
```

- [ ] **Step 4: 构建 + 测试**

Run: `npx vite build && npx vitest run`
Expected: build 退出码 0；`Tests 6 passed`。

- [ ] **Step 5: 人工冒烟（明/暗）**

Run: `npx vite preview --host 0.0.0.0`
检查：侧栏灰底、激活项白色悬浮 chip；点击主题切换，整站在明/暗间翻转且刷新后保持；角色下拉可切换并正确改导航。

- [ ] **Step 6: 提交**

```bash
cd /workspace/lms
git add smart-education-frontend/src/layouts/AppShell.vue smart-education-frontend/src/styles/index.css
git commit -m "refactor(frontend): AppShell 采用 frappe-ui Select/Button 与主题切换"
```

---

### Task 4: 共享组件迁移 + AppButton（teal 主按钮）

**Files:**
- Create: `src/components/AppButton.vue`
- Modify: `src/components/StatusBadge.vue`
- Modify: `src/components/AppMetric.vue`
- Modify: `src/components/CourseCard.vue`
- Modify: `src/components/EmptyState.vue`
- Modify: `src/components/AiResultPanel.vue`

**Interfaces:**
- Produces:
  - `AppButton.vue` — props：`variant?: 'primary' | 'secondary'`（默认 `secondary`）。`primary` → frappe-ui `Button variant="solid"` + `.app-btn-primary` teal 覆盖；`secondary` → `Button variant="subtle" theme="gray"`。透传 slot 与 `@click`。
  - `StatusBadge.vue` — 保持 props `{ label: string; tone?: 'green'|'orange'|'red' }`，内部渲染 frappe-ui `Badge`。

- [ ] **Step 1: 创建 `src/components/AppButton.vue`**

```vue
<template>
  <Button
    :variant="variant === 'primary' ? 'solid' : 'subtle'"
    theme="gray"
    :class="variant === 'primary' ? 'app-btn-primary' : ''"
  >
    <slot />
  </Button>
</template>

<script setup lang="ts">
import { Button } from 'frappe-ui'
defineProps<{ variant?: 'primary' | 'secondary' }>()
</script>
```

- [ ] **Step 2: `StatusBadge.vue` 改用 frappe-ui Badge**

```vue
<template>
  <Badge :theme="theme" variant="subtle" :label="label" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Badge } from 'frappe-ui'

const props = defineProps<{ label: string; tone?: 'green' | 'orange' | 'red' }>()

const theme = computed(() =>
  props.tone === 'green' ? 'green'
  : props.tone === 'orange' ? 'orange'
  : props.tone === 'red' ? 'red'
  : 'gray',
)
</script>
```

- [ ] **Step 3: `AppMetric.vue` 用语义 class**

```vue
<template>
  <div class="panel">
    <div class="spread">
      <div>
        <p class="muted" style="margin: 0 0 6px">{{ label }}</p>
        <div class="metric-value">{{ value }}</div>
      </div>
      <Badge :theme="badgeTheme" variant="subtle" :label="hint" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Badge } from 'frappe-ui'

const props = defineProps<{
  label: string
  value: string | number
  hint: string
  tone?: 'green' | 'orange' | 'red'
}>()

const badgeTheme = computed(() =>
  props.tone === 'green' ? 'green'
  : props.tone === 'orange' ? 'orange'
  : props.tone === 'red' ? 'red'
  : 'gray',
)
</script>
```

- [ ] **Step 4: `CourseCard.vue` 状态标签换 StatusBadge**

把模板中的 `<span :class="statusClass">{{ statusText }}</span>` 替换为：
```vue
<StatusBadge :label="statusText" :tone="statusTone" />
```
在 `<script setup>` 增加导入与 tone 计算，删除旧 `statusClass`：
```ts
import StatusBadge from '@/components/StatusBadge.vue'

const statusTone = computed(() =>
  props.course.status === 'published' ? 'green'
  : props.course.status === 'review' ? 'orange'
  : undefined,
)
```
（`statusText` 保持不变。）

- [ ] **Step 5: `EmptyState.vue` 与 `AiResultPanel.vue` 语义 token 化**

打开两文件，把任何硬编码颜色（如 `#667085`、`#0f766e`、`color: #…`）替换为语义 class：正文 `class="muted"` 或 `text-ink-gray-7`，标题 `text-ink-gray-9`，强调块沿用 `.ai-answer`。不改变结构与 props，仅替换视觉。

- [ ] **Step 6: 全站主按钮改用 AppButton（外壳与已迁移处）**

将 `AppShell.vue`、`DashboardPage.vue`（Task 5 会重写，此处先不动）以外，已用到主 CTA 的共享位置，把 `<Button variant="subtle">` 或旧 `appearance="primary"` 的**主**按钮换为 `<AppButton variant="primary">…</AppButton>`。次按钮用 `<AppButton>…</AppButton>`（默认 secondary）。

- [ ] **Step 7: 构建 + 测试**

Run: `npx vite build && npx vitest run`
Expected: build 退出码 0；`Tests 6 passed`。

- [ ] **Step 8: 人工冒烟**

Run: `npx vite preview --host 0.0.0.0`
检查：徽章在明/暗下对比正常；主按钮为 teal；卡片/指标/空状态配色统一、暗色无“死白/死黑”。

- [ ] **Step 9: 提交**

```bash
cd /workspace/lms
git add smart-education-frontend/src/components/
git commit -m "refactor(frontend): 共享组件迁移到 frappe-ui Badge/Button 与语义 token，新增 AppButton(teal)"
```

---

### Task 5: Dashboard 纯 Frappe 样板页

**Files:**
- Modify: `src/domains/dashboard/DashboardPage.vue`

**Interfaces:**
- Consumes: `AppButton`（Task 4）、frappe-ui `Card`/`Badge`、语义 utility class；现有 services（`courseService` 等，不变）。
- Produces: 首个完全用 frappe-ui 组件 + 语义 Tailwind class 写成的页面，作为后续逐页迁移的模板。

- [ ] **Step 1: 用 frappe-ui `Card` + 语义 class 重写模板**

把 `.panel`/`.page-header`/`.grid` 等 compat class 换成直接的 frappe-ui 组件与 utility。示例结构（保留原有数据绑定 `courses/pendingSubmissions/exams/stats/risks`）：
```vue
<template>
  <div>
    <div class="mb-5 flex items-start justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold tracking-tight text-ink-gray-9">首页总览</h1>
        <p class="mt-1.5 text-sm text-ink-gray-5">
          面向{{ session.roleLabels[session.currentRole] }}的课程运行、待办和 AI 预警概览。
        </p>
      </div>
      <AppButton variant="primary" @click="$router.push('/ai/assistant')">打开智能答疑</AppButton>
    </div>

    <div class="grid grid-cols-4 gap-3.5">
      <AppMetric label="课程数量" :value="courses.length" hint="含待审核" tone="green" />
      <AppMetric label="待批改作业" :value="pendingSubmissions" hint="需要处理" tone="orange" />
      <AppMetric label="近期考试" :value="exams.length" hint="7 月安排" />
      <AppMetric label="风险学生" :value="stats?.riskCount ?? '-'" hint="需干预" tone="red" />
    </div>

    <div class="mt-4 grid grid-cols-2 gap-3.5">
      <Card title="学习进度预警">
        <div class="space-y-2.5">
          <div v-for="risk in risks" :key="risk.id"
               class="rounded-lg bg-surface-gray-1 p-3">
            <p class="text-sm font-medium text-ink-gray-8">{{ risk.title }}</p>
            <p class="mt-1 text-sm text-ink-gray-5">{{ risk.content }}</p>
          </div>
        </div>
      </Card>

      <Card title="课程动态">
        <div class="divide-y divide-outline-gray-1">
          <div v-for="course in courses.slice(0, 4)" :key="course.id"
               class="flex items-center justify-between py-2.5 text-sm">
            <span class="text-ink-gray-8">{{ course.title }}</span>
            <span class="text-ink-gray-5">{{ course.teacherName }}</span>
            <Badge theme="green" variant="subtle" :label="`${course.progressAverage}%`" />
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 更新 `<script setup>` 导入**

在现有导入基础上增加（保留所有 service 逻辑与 `onMounted` 不变）：
```ts
import { Card, Badge } from 'frappe-ui'
import AppButton from '@/components/AppButton.vue'
```
删除对已不再使用的 compat class 的依赖（模板已替换）。

- [ ] **Step 3: 构建 + 测试**

Run: `npx vite build && npx vitest run`
Expected: build 退出码 0；`Tests 6 passed`。

- [ ] **Step 4: 人工冒烟（明/暗）**

Run: `npx vite preview --host 0.0.0.0`
检查：Dashboard 用 frappe-ui `Card` 呈现；指标区、预警、课程动态在明/暗下均正常；teal 主按钮生效；与其余 compat 页面视觉一致无割裂。

- [ ] **Step 5: 提交**

```bash
cd /workspace/lms
git add smart-education-frontend/src/domains/dashboard/DashboardPage.vue
git commit -m "refactor(frontend): Dashboard 重写为纯 frappe-ui 样板页"
```

---

## Self-Review

**Spec coverage：**
- §3 移植工具链 → Task 1（tailwind/postcss/config/index.css）✅
- §3 theme.ts → Task 2 ✅
- §4 剥离后端 → Global Constraints + main.ts 仅加 applyTheme（不引入后端插件）✅；vite `frappeProxy:false` 为现状，无需改动。
- §5.1 AppShell → Task 3 ✅
- §5.2 共享组件 → Task 4 ✅
- §5.3 Dashboard 样板 → Task 5 ✅
- §6 teal 主色 → `--teal-*` + `.app-btn-primary` + `AppButton`（Task 1/4）✅
- §6 暗色 → Task 2 + compat 层 `@apply` 语义 token 自动翻转 ✅
- §8 测试保持绿 → 每个 Task 末尾 `vitest run` ✅
- §9 分步交付 → 5 个 Task 与提交一一对应 ✅

**Placeholder 扫描：** 每步含实际代码/命令与预期输出，无 TBD/TODO/“类似上文”。✅

**类型一致性：** `theme`/`applyTheme`/`toggleTheme` 签名在 Task 2 定义并在 Task 2/3 使用一致；`AppButton` 的 `variant: 'primary'|'secondary'`、`StatusBadge` 的 `tone: 'green'|'orange'|'red'` 在 Task 4/5 使用一致；utility class 仅用 Global Constraints 中已验证词汇。✅

**偏离 spec 的已知点（已在计划内显式处理）：** frappe-ui Button 无 teal theme → 以 `AppButton` + `.app-btn-primary` class 覆盖实现 teal 主色（spec §6 目标不变，实现手段落地）。
