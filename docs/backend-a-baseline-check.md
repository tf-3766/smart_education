# 后端 A 第一阶段基线检查

> 分支策略：本文在本地 `backend-2` 编写，等待后端 B 完成后再统一合并到 `dev`。
> 适用范围：后端 A 的 Biz 主链，重点是作业、提交、成绩、论坛和学习预警。
> 本文只记录检查结论和后续契约要求，不实现 Controller、Service、Mapper、Entity 或 Bootstrap SQL。

## 1. 读取与缺失文档

已读取并作为本轮依据：

| 文件 | 结论 |
|---|---|
| `docs/team-division.md` | 明确后端 A 负责 `auth`、`course`、`assignment`、`grade`、`forum`、`warning`、`shared` |
| `docs/module-ownership.md` | 明确 `dev` 是唯一后端集成分支，`backend-2` 只作为本地/临时分支 |
| `docs/api-reference.md` | 明确 `/api/v1`、统一响应、分页、错误码、幂等和 SSE 规则 |
| `docs/database-conventions.md` | 明确 Bootstrap SQL、审计字段、逻辑删除、乐观锁和状态流转规则 |
| `docs/migration-register.md` | 记录 Bootstrap SQL 的变更规则和当前基线 |
| `docs/api-reference.md` | 汇总已实现和未实现接口，后续实现必须同步更新状态 |
| `backend/README.md` | 明确五个 Maven 模块及当前服务职责 |

已统一纳入以下现有文档：

```text
docs/api-reference.md
docs/team-division.md
docs/database-conventions.md
docs/module-ownership.md
```

## 2. 当前分支和合并策略

当前工作分支应保持为：

```text
backend-2
```

本阶段不创建 `docs/a-assignment-contract` 分支，不切换到 `dev`，不合并 `dev`。这些文档先作为后端 A 的本地契约草案，待后端 B 的 AI/Gateway/考试能力完成后再一起整理合入 `dev`。

## 3. 已有能力

| 范围 | 当前状态 | 后端 A 可复用点 |
|---|---|---|
| 认证与角色 | `auth` 已有登录、当前用户、角色与权限基础实现 | Controller 使用角色和权限注解，业务用例读取当前用户 |
| 课程与学习 | `course` 已有课程、章节、课时、资料、选课和学习进度实现 | 复用课程负责人、协作者、有效选课和学生可见性判断 |
| 审计字段 | `BaseAuditEntity` 已统一 `id/createdAt/createdBy/updatedAt/updatedBy/deleted/version` | 新业务 Entity 已继承，后续只实现用例不重造审计 |
| 迁移 | `V20260709110000__create_learning_collaboration_tables.sql` 已创建 A/B 基础表 | 本阶段不再为八张基础表创建 SQL |
| 演示数据 | `R__local_learning_collaboration_demo.sql` 已有作业、成绩、论坛、预警示例 | 可作为后续接口集成测试和前端联调 seed 参考 |

## 4. 已有表与骨架

后端 A 可直接围绕以下已有表和 Entity/Mapper 骨架设计用例：

| 模块 | 已有表 | 当前缺口 |
|---|---|---|
| 作业 | `edu_assignment`、`edu_assignment_attachment` | 缺 DTO/VO、状态枚举、创建/更新/发布用例、权限测试 |
| 提交 | `edu_assignment_submission` | 缺草稿/正式提交规则、重复提交/逾期规则、教师提交列表 |
| 成绩 | `edu_grade_record` | 缺批改、成绩发布、学生成绩查询、教师统计 |
| 论坛 | `edu_forum_topic`、`edu_forum_reply` | 缺成员发帖回复、教师/管理员治理、可见性规则 |
| 预警 | `edu_learning_warning`、`edu_warning_evidence` | 缺规则生成、学生/教师查询、处理状态和 AI 解释边界 |

## 5. 权限复用计划

后端 A 后续实现必须复用或扩展课程权限语义：

| 业务动作 | 权限来源 |
|---|---|
| 教师创建、更新、发布作业 | 课程负责人或课程协作者 |
| 教师查看提交、批改、发布成绩 | 课程负责人或课程协作者 |
| 学生查看作业、保存草稿、正式提交 | 对课程有有效选课且课程对学生可见 |
| 学生查看成绩 | 只看本人且成绩为 `PUBLISHED` |
| 课程论坛发帖和回复 | 课程成员：已选学生或课程教师 |
| 教师治理论坛内容 | 对应课程负责人或课程协作者 |
| 管理员治理论坛内容 | 管理员只能治理内容，不替教师评分 |
| 学生查看预警 | 只看本人预警 |
| 教师查看和处理预警 | 只处理自己负责或协作课程下学生预警 |

## 6. 状态与规则基线

| 领域 | 基线规则 |
|---|---|
| 作业 | `DRAFT -> PUBLISHED -> CLOSED`；截止时间和开放时间由服务端判断 |
| 提交 | `DRAFT -> SUBMITTED -> GRADED/RETURNED`；`GRADED` 不等于成绩已发布 |
| 成绩 | `DRAFT -> PUBLISHED`；学生接口不能返回未发布成绩 |
| 论坛 | `VISIBLE -> HIDDEN`；删除使用逻辑删除或治理状态，不物理删除历史 |
| 预警 | `OPEN -> HANDLED/IGNORED` 作为当前表可承载的 MVP 状态；更细状态需增量迁移 |
| AI | AI 只返回评语草稿、解释和建议；正式保存、发布、处理必须由 Biz 完成 |

## 7. 数据库差距检查

当前不创建新 SQL。先记录需要在契约评审时确认的潜在差距：

| 差距 | 当前处理 |
|---|---|
| 学生提交多附件 | 当前 `edu_assignment_submission` 只有单组 `file_key/file_url`；MVP 可先支持单附件，若前端要求多附件再登记 `edu_submission_attachment` |
| 评分历史和撤回 | 当前 `edu_grade_record` 是当前正式记录；MVP 不做成绩版本表 |
| 论坛删除原因 | 当前 topic/reply 只有 `status`；MVP 可先隐藏/逻辑删除，复杂治理延后 |
| 预警处理备注 | 当前 `edu_learning_warning` 有处理人和时间，无独立 action 表；MVP 可先用状态追踪，处理备注如必须持久化再增量迁移 |
| 幂等键存储 | 当前无通用幂等表；MVP 可先以业务唯一性约束控制重复正式提交，后续公共幂等由 shared 统一设计 |

## 8. 第一阶段完成标准

- `docs/api-reference.md` 已写清楚接口、状态、权限和错误码。
- 不修改 `edu-common`、`edu-gateway`、`edu-ai-service`、`edu-biz-service/exam/**`。
- 不创建或修改 Bootstrap SQL。
- 不新增 Controller、Service、Mapper、Entity。
- 后续编码前先从这些契约拆分 issue，再用测试驱动作业/成绩最小闭环。
