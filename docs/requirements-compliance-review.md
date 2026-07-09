# 需求符合性审查报告

> 审查日期：2026-07-09  
> 需求来源：`第1组-在线教育辅助教学系统需求分析说明书.docx`  
> 审查对象：当前代码、数据库脚本、接口文档、项目设计文档  
> 结论口径：区分“已实现”“已有表/契约/骨架”“未实现”，避免把设计范围写成完成状态。

## 1. 总体结论

当前项目方向与需求说明书一致：角色覆盖学生、教师、管理员；后端结构覆盖 `edu-gateway`、`edu-common`、`edu-feign-api`、`edu-biz-service`、`edu-ai-service`；数据库已经补齐课程、作业、成绩、论坛、预警、考试、题库和 AI 采用审计的基础表；接口文档已经给出前后端并行开发所需的路径、角色和边界。

当前代码完成度不能写成“完整教学平台已实现”。真实状态是：

- 认证、角色、课程、章节、课时、资料、选课、学习进度和课程审核已有可运行实现。
- 作业、提交、成绩、论坛、预警、考试、题库和 AI 采用审计已有数据库、Entity、Mapper 和接口契约，但缺少公开 Controller、Application Service、DTO/VO、权限用例和集成测试闭环。
- AI 服务已有工程骨架、鉴权过滤、Feign 契约接入和 Biz 课程上下文内部接口，但公开 AI API、Fake Adapter、SSE、RAG、摘要、评语草稿、风险解释和组卷建议尚未实现。
- 前端已有学生、教师、管理员核心页面原型和静态交互，但尚未接入真实 API、鉴权状态、错误处理和端到端联调。

## 2. 需求覆盖矩阵

| 需求域 | 需求说明书要求 | 当前证据 | 符合性 | 后续处理 |
|---|---|---|---|---|
| 三类角色 | 学生、教师、管理员分别拥有课程学习、教学管理、系统治理能力 | `sys_user/sys_role/sys_permission`，前端三类路由和页面 | 部分符合 | 补管理员用户治理、课程分类、公告治理 API |
| 权限与资源范围 | 后端必须资源级校验，不能只靠菜单隐藏 | 课程模块已做 owner/协作者/选课校验，AI context 由 Biz 授权 | 部分符合 | 新增作业、考试、论坛、预警时沿用资源校验 |
| 课程学习 | 课程、章节、课时、资料、选课、进度 | `course/**` Controller/Service/测试，`V2/V3` migration | 符合 MVP | 继续作为 A/B 两条后端任务的上游稳定契约 |
| 作业提交与批改 | 教师发布作业，学生提交，教师评分，学生查成绩 | `edu_assignment*`、`edu_grade_record`、Entity/Mapper、API 草案 | 部分符合 | 后端 A 实现 Controller/Service/DTO/测试 |
| 成绩统计 | 教师查看课程成绩统计，学生查看本人已发布成绩 | 成绩表和接口草案已有 | 部分符合 | 后端 A 实现统计查询和发布状态规则 |
| 论坛讨论 | 学生发帖、回复，教师/管理员治理 | `edu_forum_topic`、`edu_forum_reply`、接口草案 | 部分符合 | 后端 A 实现最小闭环，公告延后或单独排期 |
| 学习预警 | 基于进度、缺交、低分生成正式预警和证据 | `edu_learning_warning`、`edu_warning_evidence`、接口草案 | 部分符合 | 后端 A 实现规则生成、学生查看、教师处理 |
| 考试与题库 | 考试安排、题库、试卷草稿，智能组卷建议 | `edu_exam*`、`edu_question*`、`edu_exam_paper*` 表和骨架 | 部分符合 | 后端 B 实现考试/题库 API；完整在线考试引擎可延后 |
| AI 答疑与草稿 | RAG 答疑、章节摘要、评语草稿、风险解释、组卷建议 | `edu-ai-service` 骨架、Feign 上下文、AI API 草案 | 部分符合 | 后端 B 先做 Fake Adapter + SSE + 引用结构 |
| AI 边界 | AI 只能返回回答、草稿、解释、引用和建议，正式事实由 Biz 保存 | ADR、架构文档、Feign contract、`edu_ai_generation_record` | 设计符合，代码部分符合 | 新 AI API 不得连接 Biz MySQL 或写正式业务表 |
| 前端体验 | 学生、教师、管理员页面，AI 嵌入具体业务场景 | Vue 3 页面、路由、Element Plus UI | 部分符合 | 前端 C 接入 API、登录态、错误态和联调数据 |
| 非功能 | JDK 21、Spring Boot 3.x、Nacos、Gateway、OpenFeign、MyBatis-Plus、MySQL、Redis、RabbitMQ、Docker/CI | Maven 模块、依赖、`deploy/docker-compose.yml`、docs | 部分符合 | 补 `.gitlab-ci.yml`、启动验收脚本、SSE/限流验证 |

## 3. 当前不一致与修正结果

| 问题 | 修正 |
|---|---|
| 设计文档残留旧 AI 路径 `/api/v1/ai/course-qa/streams` | 统一为 `/api/v1/ai/courses/{courseId}/qa/stream` |
| 设计文档残留旧选课路径 `/student/courses/{id}/enrollments` | 统一为 `/api/v1/student/courses/{courseId}/enroll` |
| 设计文档中作业批改路径有 `/grading` 和 `/grade` 两种 | 统一为 `/api/v1/teacher/submissions/{submissionId}/grade` |
| `mvp-scope.md` 仍写“不要创建业务表/Mapper” | 改为当前状态：表、Entity、Mapper 已完成，下一步实现业务闭环 |
| 后端协作文档只描述两名后端，未纳入前端成员 | 新增三人开发计划，明确前端 C 与后端 A/B 的接口协作边界 |
| 旧课程模块交付文档与平台级契约重复 | 清理旧交付说明，保留平台 API 和 OpenAPI 作为当前契约 |

## 4. 后续必须补齐的实现缺口

1. 后端 A：作业、提交、成绩、论坛、预警的公开接口、服务层、权限校验、状态流转和集成测试。
2. 后端 B：AI Fake Adapter、SSE、评语/摘要/风险解释/组卷建议接口，考试与题库接口，Gateway SSE/限流联调。
3. 前端 C：登录态、API client、路由守卫、三端页面数据接入、错误态/加载态、与后端演示 seed 联调。
4. 共同：OpenAPI 与实现保持一致；新增字段、状态、错误码先更新契约再编码。

