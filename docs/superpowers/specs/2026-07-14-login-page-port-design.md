# 登录页布局移植设计（求职版 → smart-education-frontend）

日期：2026-07-14
状态：已批准，待实现

## 背景

`jinshanHomework/final_homework/user-frontend/src/views/Login.vue`（"职途招聘"求职版）
的登录页采用两栏布局：顶栏品牌 + 左侧登录卡片 + 右侧平台介绍面板，视觉更完整。
目标项目 `smart-education-frontend`（"知行教学云"）当前登录页是单栏居中卡片。
需求：把源页布局移植到目标登录页。

## 目标与非目标

**目标**
- 把源页两栏布局（顶栏品牌 / 左卡片 / 右介绍面板）搬到目标登录页。
- 用目标现有技术栈重写，**不引入 Element Plus**。
- 保留目标现有登录逻辑与双模式（demo 角色快捷登录 / real 账号密码）。
- 品牌与文案换成教育版。

**非目标**
- 不改后端契约、不改 session store 逻辑、不加自助注册。
- 不引入任何新依赖。

## 约束与现状事实

- 目标登录页：`src/domains/auth/LoginPage.vue`。`<script setup>` 依赖
  `useSessionStore` / `demoAccounts` / `roleHome` / `roleLabels` / `Role`，
  通过 `session.isDemoMode`（= `apiMode === 'demo'`，环境驱动，用户不可切换）
  决定渲染角色按钮还是账号密码表单。
- 现有登录样式在 `src/styles/index.css` 第 304–314 行（`.login-*` 块）。
- 目标 CSS 变量：`--brand #1468e8` / `--ink` / `--ink-2` / `--muted` / `--muted-2`
  / `--line` / `--line-soft` / `--panel` / `--canvas` / `--shadow-pop`。
  （源页用的是 `--primary` 等变量，移植时需映射到目标变量。）
- 目标**无 `/register` 路由**，无自助注册 → 删除源页的"注册"角标与"立即注册"链接。
- 复用已有 `AppButton` 组件与 `lucide-vue-next` 图标依赖。

## 设计

### 改动文件（2 个）

1. **`src/domains/auth/LoginPage.vue`** — 重写 `<template>`；`<script setup>` 基本不动
   （保留 `submit()` / `enter(role)` / `session` / `router` / 各 store 引用）。
2. **`src/styles/index.css`** — 替换现有 `.login-*` 块，改为移植后的两栏样式
   （类名沿用/扩展 `.login-*` 命名，映射到目标 CSS 变量），并带 `@media (max-width: 760px)` 响应式。

### 布局结构

```
┌ 顶栏：[知] 知行教学云 | 登录 ─────────────────────┐
│                                                     │
│  ┌ 登录卡片 ────────┐        右侧介绍面板           │
│  │ 账号登录（下划线） │   Learning Console            │
│  │ ── real 模式 ──   │   让教学更清晰、更高效          │
│  │ 账号 __________   │   ✓ 集中管理课程·作业·考试      │
│  │ 密码 __________   │   ✓ 快速检索教学资源            │
│  │ [ 登录 → ]        │   ✓ 随时跟进学情与进度          │
│  │ ── demo 模式 ──   │                               │
│  │ [学生][教师][管理] │                               │
│  │ 底部 hint         │                               │
│  └──────────────────┘                               │
└─────────────────────────────────────────────────────┘
```

- **顶栏**：`知` 徽章（复用 `.brand-mark` 风格）+ "知行教学云" + 竖线 + "登录"。
- **左卡片**：标题"账号登录"带下划线强调条。
  - real 模式：账号 / 密码输入框 + "登录 →" `AppButton` + 错误提示（`.login-error`）。
  - demo 模式：角色快捷登录按钮（沿用现有 `.login-role` 观感）。
  - 底部 hint：沿用现有 demo/联调账号提示。
- **右介绍面板**：kicker + 大标题（关键词高亮）+ 3 条 ✓ 要点，教育文案。
- **响应式**：<760px 时两栏收成单栏，介绍面板置于卡片下方或隐藏。

## 测试

- 复用/更新 `src/tests/loginForm.test.ts`：
  - real 模式：渲染账号、密码输入框与登录按钮；填写并提交调用 `loginWithCredentials`。
  - demo 模式：渲染角色快捷登录按钮。
  - 顶栏品牌文案"知行教学云"存在；右侧介绍面板存在。
- 全量 `npx vitest run` 通过；`vue-tsc` 应用区 0 错误。

## 风险

- `LoginPage.vue` 已在既有 WIP 工作区中（`M` 状态）；本次在其基础上继续改，
  提交时只 `git add` 登录相关文件（`LoginPage.vue`、`styles/index.css`、`loginForm.test.ts`）。
- 纯前端视觉改动，不触碰后端联调链路。
