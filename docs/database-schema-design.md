# 在线教育辅助教学系统数据库设计脚本说明

> 版本：2026-07-09  
> 适用服务：`edu-biz-service`  
> 生产迁移：`backend/edu-biz-service/src/main/resources/db/migration/V20260709110000__create_learning_collaboration_tables.sql`  
> 本地演示数据：`backend/edu-biz-service/src/main/resources/db/localmigration/R__local_learning_collaboration_demo.sql`

## 1. 设计原则

- 正式业务事实只由 `edu-biz-service` 写入 MySQL，`edu-ai-service` 不连接 Biz 数据库。
- 历史迁移 `V1` 到 `V3` 不修改，本次新增时间戳迁移。
- 本项目不创建数据库物理外键，靠应用事务、唯一约束、索引和审计校验保证一致性。
- 所有核心业务表保留 `created_at`、`created_by`、`updated_at`、`updated_by`、`deleted`、`version`。
- 本地演示 seed 使用固定 ID，方便前端、Postman 和自动化测试复现。

## 2. 已有基础表

| 领域 | 表 |
|---|---|
| 用户权限 | `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission` |
| 课程学习 | `edu_course`、`edu_course_teacher`、`edu_course_enrollment`、`edu_course_chapter`、`edu_course_lesson`、`edu_course_material`、`edu_lesson_learning_record` |
| 课程审核 | `edu_course_review` |

## 3. 本次新增表

| 领域 | 表 | 用途 |
|---|---|---|
| 作业 | `edu_assignment` | 教师发布课程作业，绑定课程和可选课时 |
| 作业 | `edu_assignment_attachment` | 作业说明附件元数据 |
| 提交/批改 | `edu_assignment_submission` | 学生提交、教师评分、AI 评语草稿关联 |
| 成绩 | `edu_grade_record` | 统一保存作业、考试或人工成绩记录 |
| 论坛 | `edu_forum_topic` | 课程内讨论主题 |
| 论坛 | `edu_forum_reply` | 课程内讨论回复 |
| 预警 | `edu_learning_warning` | 学习风险记录和处理状态 |
| 预警 | `edu_warning_evidence` | 预警证据和指标快照 |
| 考试 | `edu_exam` | 考试安排 |
| 题库 | `edu_question_bank`、`edu_question`、`edu_question_option` | 题库、题目和选项 |
| 试卷 | `edu_exam_paper`、`edu_exam_paper_question` | 试卷草稿/发布稿和题目编排 |
| 答题 | `edu_exam_attempt`、`edu_exam_answer` | 学生答题记录和答案 |
| AI 采用审计 | `edu_ai_generation_record` | Biz 侧保存 AI 草稿、建议、采用状态和人工确认记录 |

## 4. 演示数据

本地 seed 会基于已有演示账号和课程插入：

| 数据 | 固定 ID 示例 |
|---|---|
| 作业与附件 | `edu_assignment.id = 31001`、`edu_assignment_attachment.id = 31501` |
| 学生提交与成绩 | `edu_assignment_submission.id = 32001`、`edu_grade_record.id = 33001` |
| 课程论坛 | `edu_forum_topic.id = 34001`、`edu_forum_reply.id = 35001` |
| 学习预警 | `edu_learning_warning.id = 36001`、`edu_warning_evidence.id = 36101` |
| 题库与考试 | `edu_question_bank.id = 37001`、`edu_exam.id = 38001`、`edu_exam_attempt.id = 38301` |
| AI 记录 | `edu_ai_generation_record.id = 39001/39002` |

## 5. 主要状态码

| 领域 | 状态示例 |
|---|---|
| 作业 | `DRAFT`、`PUBLISHED`、`CLOSED` |
| 提交 | `DRAFT`、`SUBMITTED`、`GRADED`、`RETURNED` |
| 成绩 | `DRAFT`、`PUBLISHED`、`REVOKED` |
| 论坛 | `VISIBLE`、`HIDDEN` |
| 预警 | `OPEN`、`HANDLED`、`IGNORED`；级别 `LOW`、`MEDIUM`、`HIGH` |
| 考试/试卷 | `DRAFT`、`PUBLISHED`、`CLOSED` |
| 题目 | `ACTIVE`、`DISABLED` |
| 答题 | `NOT_STARTED`、`IN_PROGRESS`、`SUBMITTED`、`GRADED` |
| AI 记录 | `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED` |

## 6. 与代码的对应关系

- 新增 Entity/Mapper 位于 `backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/**/infrastructure/persistence/`。
- 跨服务 DTO 不放在 Biz Entity 中，而放在 `backend/edu-feign-api/src/main/java/com/zhongruan/edu/feign/**`。
- 前端接口以 `docs/api-contract-platform.md` 和 `docs/openapi/platform-api.openapi.yaml` 为准。
