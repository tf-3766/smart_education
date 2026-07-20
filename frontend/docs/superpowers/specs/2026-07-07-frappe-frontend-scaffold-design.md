# 设计文档：以 Frappe LMS 前端脚手架重构 smart-education-frontend

- 日期：2026-07-07
- 范围：**只移植前端**（纯前端、mock 驱动、可独立运行，不引入任何后端耦合）
- 目标项目：`lms/smart-education-frontend`（TypeScript + Vite + Vue 3 + frappe-ui）
- 参考基座：`lms/frontend`（真实 Frappe Learning 前端）

## 1. 背景与目标

`smart-education-frontend` 目前是一套 mock 驱动的 MVP，表现层由手写 CSS（`src/styles/index.css`）承载，仅零散使用了 frappe-ui 的 `Button`。目标是**彻底采用 Frappe UI 体系**：以真实 Frappe LMS 前端（`lms/frontend`）的工具链与主题为脚手架基座，在其上重建表现层，并支持明/暗模式；同时**保持纯前端、可独立运行**，不引入 Frappe 后端（`frappeRequest`/socket/telemetry/翻译/分发层等）。

品牌主色保留 **teal**（教育品牌），通过 Tailwind preset 映射为 primary。

### 成功标准

1. 项目构建运行在 **Tailwind v3.4 + `frappe-ui/tailwind` preset** 上。
2. 应用**可独立运行**（`vite dev` / `vite preview` 无需任何后端；继续走 `mockRuntime`）。
3. 应用外壳（AppShell：侧栏 / 顶栏 / 导航）与共享组件走 **frappe-ui 组件 + 语义 token**（`bg-surface-*`、`text-ink-*`、`Badge`、`Button`、`Select` 等）。
4. **明/暗模式可切换并持久化**（`data-theme` + localStorage）。
5. teal 主色在主按钮 / 链接 / 选中态生效。
6. 所有既有页面仍能渲染（迁移期通过 compat 层）；Dashboard 为首个纯 Frappe 样板页。
7. 既有 `vitest` 服务层测试（4 个）保持绿。

### 非目标（明确排除）

- 不物理复制 `lms/frontend` 那 340 个文件的整棵树。
- 不引入 Frappe 后端数据层（`frappeRequest`/`createResource`/socket/telemetry/后端翻译/`pageMeta`）。
- 不引入 PWA、SCORM 代理、jinjaBootData、分发层。
- 本期不逐页重写全部 20+ 领域页面（除 Dashboard 样板外，其余先由 compat 层承载，后续迭代迁移）。

## 2. 执行策略：移植脚手架（非物理复制整棵树）

`smart-education-frontend` 已具备正确骨架（TypeScript、Vite、frappe-ui、Pinia、Router、领域代码 + mock）。因此"以 lms 为新脚手架"的执行方式是：**把 lms 的 Frappe 原生工具链与主题文件移植进本项目，在其上重建表现层**，保留 TypeScript 与既有测试。最终态与"物理复制 lms/frontend 再删domain"一致，但更干净、无需清理后端耦合与 JS→TS 回退。

## 3. 从 lms/frontend 移植进来（近乎照搬）

| 文件 | 处理 |
|---|---|
| `tailwind.config.js` | 新增：`presets:[require('frappe-ui/tailwind')]`；`content` globs 覆盖 `./index.html`、`./src/**/*.{vue,js,ts}`、`./node_modules/frappe-ui/src/**/*`；`theme.extend` 增加 teal→primary 映射与 `strokeWidth 1.5` |
| `postcss.config.js` | 新增：`{ plugins: { tailwindcss:{}, autoprefixer:{} } }` |
| `src/styles/index.css` | 改为：`@import 'frappe-ui/style.css';` +（Tailwind 由 preset 注入 base/components/utilities）+ `[data-theme='dark']` 覆盖 + 收敛后的 compat 层（现有 class，值改为 `@apply` / Frappe token） |
| `src/utils/theme.ts` | 移植：明/暗切换 composable（`data-theme` + `localStorage.theme`），直接复用 |
| devDeps | 新增 `tailwindcss@^3.4`、`postcss@^8.4`、`autoprefixer@^10.4`（对齐 lms）；停用 transitively 装入的 Tailwind v4 |

## 4. 剥离 / 不带进来

- `main.ts`：不引入 `frappeRequest`/`setConfig(resourceFetcher)`/`initSocket`/`telemetryPlugin`/后端 `translation`/`pageMetaPlugin`。保留 `FrappeUI` + Pinia + Router。
- `vite.config.ts`：`frappeProxy: false`（现状即如此）；不加 PWA / SCORM 代理 / jinjaBootData。
- 不引入 lms 的业务页面 / doctype / resource 数据层。数据继续走 `src/services/*` + `src/services/mockRuntime.ts`。

## 5. 重建表现层

### 5.1 AppShell（`src/layouts/AppShell.vue`）
- 侧栏、顶栏、导航采用 Frappe 模式：`bg-surface-gray-1`（侧栏与页面同灰底）、卡片/顶栏白面、`text-ink-*` 文本；激活导航项为白色悬浮 chip（`bg-surface-white` + 轻 elevation），非彩色填充；分组标题 `text-ink-gray-5` medium、不大写。
- 顶栏新增 **明/暗切换按钮**（`theme.ts` 驱动，sun/moon 图标）与用户菜单。
- 角色切换 `<select>` → frappe-ui `Select`。

### 5.2 共享组件
- `StatusBadge.vue` → frappe-ui `Badge`（保留 tone 映射）。
- `AppMetric.vue` / `CourseCard.vue` / `EmptyState.vue` / `AiResultPanel.vue` → 语义 token class（`bg-surface-white`、`border-outline-gray-1`、`text-ink-*`）。
- 按钮统一 frappe-ui `Button`（primary=teal solid）。

### 5.3 页面
- 迁移期：20+ 领域页面继续用 token 化的 compat CSS 渲染，保证可用与观感一致。
- **Dashboard（`src/domains/dashboard/DashboardPage.vue`）作为首个纯 Frappe 样板页**，全部改用 frappe-ui 组件 + 语义 class，作为后续逐页迁移的模板。

## 6. 主题与暗色

- **teal 主色**：在 `tailwind.config.js` 中把 preset 的 primary/accent 语义映射到 Frappe teal 色阶；frappe-ui 组件本身 token 驱动，暗色自动适配。
- **暗色模式**：`src/utils/theme.ts` 设置 `document.documentElement[data-theme]`，持久化到 `localStorage.theme`；`main.ts` 启动时读取并应用；compat 层为残留原始值补 `[data-theme='dark']` 覆盖。

## 7. 组件与边界

- **theme 模块**（`utils/theme.ts`）：单一职责=读写主题并同步 DOM 属性 + localStorage；接口 `theme` ref 与 `toggleTheme()`；依赖仅 DOM/localStorage。
- **AppShell**：布局与导航壳；依赖 theme 模块、router、session store；不含业务数据逻辑。
- **共享组件**：纯展示，props 输入、无副作用。
- **数据层（services + mockRuntime）**：不变，保持前端 mock，无后端依赖。

## 8. 测试与回归

- 既有 `vitest` 服务层 4 个测试保持绿（本次为表现层改造，逻辑不变，预期无需改测试）。
- 每步验证：`vite build` 通过 + `vitest run` 通过 + `vite preview` 明/暗各人工冒烟一次。

## 9. 分步交付（小步提交，每步独立可构建/可提交）

1. **工具链**：加 tailwind v3 + postcss + `tailwind.config.js` + `postcss.config.js` + 改 `index.css`（引 frappe-ui/style.css + preset）；构建通过。
2. **主题 + 暗色**：移植 `theme.ts`，`main.ts` 应用主题，顶栏加切换；明/暗可切换持久化。
3. **AppShell**：侧栏/顶栏/导航改 frappe-ui + 语义 token。
4. **共享组件**：Badge/AppMetric/CourseCard/EmptyState/AiResultPanel/Button 迁移。
5. **Dashboard 样板页**：纯 frappe-ui 重写，作为后续迁移模板。

## 10. 风险与缓解

- **Tailwind v4 与 v3 preset 冲突**：显式安装 v3.4 并以 `tailwind.config.js` + `postcss.config.js` 驱动；确认构建使用 v3。
- **compat 层与新 token 观感漂移**：compat class 的颜色一律改引 Frappe token / `@apply`，使新旧组件同源同色。
- **暗色下 compat 残留硬编码色**：为其补 `[data-theme='dark']` 覆盖块。
- **frappe-ui beta.7 组件差异**（如 FormLabel/Switch）：参考 lms `index.css` 中已有的对应修正照搬。
