# AI 内容级 RAG（Phase 1）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让课程内 AI 问答基于上传资料的正文（而非文件名）回答，且无需手动点「同步知识库」。

**Architecture:** 两处改动，全部落在 `edu-ai-service` 内，不新增跨服务方向。① 新增 `CourseKnowledgeTools`（`@Tool searchCourseKnowledge`）注入 `courseQa` 的 LLM 工具列表，让模型可按需检索资料正文；② `courseQa` 已每次拉取带正文的课程上下文，在此对知识库做「内容哈希去重的惰性自动索引」，去掉手动同步依赖。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring AI（ChatClient `.tools(...)`、Qdrant VectorStore）、JUnit 5 + Mockito。

## Global Constraints

- 不新增 biz→AI 出站方向；自动索引只用 `courseQa` 已获取的 `AiCourseContextResponse`。
- 复用现有 `@Tool` 注入方式：`generator.stream(system, user, conversationId, tool1, tool2, ...)`。
- 工具与索引均须尊重现有权限：`courseQa` 前置的 `contextService.courseContext(...)` 已做归属校验，工具只在该课程范围内检索。
- 向量库可能未配置（`configured()==false`）时不得抛错，须优雅降级（沿用 `Retrieval.unavailable()`）。
- DashScope `text-embedding-v3` 单请求最多 10 条输入（已有 `EMBEDDING_BATCH_SIZE=10`，勿破坏）。

---

### Task 1: `searchCourseKnowledge` 工具

**Files:**
- Create: `backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/tools/CourseKnowledgeTools.java`
- Modify: `backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/application/AiApplicationService.java`（`courseQa` 的 `generator.stream(...)` 工具列表，当前 `AiApplicationService.java:103-108`）
- Test: `backend/edu-ai-service/src/test/java/com/zhongruan/edu/ai/tools/CourseKnowledgeToolsTest.java`

**Interfaces:**
- Consumes: `CourseKnowledgeBaseService.retrieve(Long courseId, Long lessonId, String question)` → `Retrieval(boolean vectorStoreAvailable, String context, List<AiCitationVO> citations)`，其中 `Retrieval.matched()` 表示命中正文。
- Produces: `CourseKnowledgeTools(CourseKnowledgeBaseService knowledgeBase, Long courseId, Long lessonId)`，含 `@Tool` 方法 `String searchCourseKnowledge(String query)`。

- [ ] **Step 1: 写失败测试**

```java
package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService.Retrieval;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseKnowledgeToolsTest {

    @Test
    void returnsRetrievedBodyWhenMatched() {
        CourseKnowledgeBaseService kb = mock(CourseKnowledgeBaseService.class);
        when(kb.retrieve(eq(7L), eq(3L), eq("向量检索原理")))
                .thenReturn(new Retrieval(true, "【第2章】向量检索基于近似最近邻...", List.of()));

        CourseKnowledgeTools tools = new CourseKnowledgeTools(kb, 7L, 3L);

        String result = tools.searchCourseKnowledge("向量检索原理");

        assertThat(result).contains("向量检索基于近似最近邻");
    }

    @Test
    void tellsModelWhenNoMatchInsteadOfThrowing() {
        CourseKnowledgeBaseService kb = mock(CourseKnowledgeBaseService.class);
        when(kb.retrieve(eq(7L), eq(null), eq("不存在的知识")))
                .thenReturn(new Retrieval(true, "", List.of()));

        CourseKnowledgeTools tools = new CourseKnowledgeTools(kb, 7L, null);

        String result = tools.searchCourseKnowledge("不存在的知识");

        assertThat(result).contains("未检索到");
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd backend && ./mvnw -q -pl edu-ai-service test -Dtest=CourseKnowledgeToolsTest`
Expected: 编译失败（`CourseKnowledgeTools` 不存在）。

- [ ] **Step 3: 实现工具类**

```java
package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService.Retrieval;
import org.springframework.ai.tool.annotation.Tool;

/** 课程资料正文检索工具：让 LLM 按需从已授权课程的知识库中检索上传资料的正文片段。 */
public class CourseKnowledgeTools {
    private final CourseKnowledgeBaseService knowledgeBase;
    private final Long courseId;
    private final Long lessonId;

    public CourseKnowledgeTools(CourseKnowledgeBaseService knowledgeBase, Long courseId, Long lessonId) {
        this.knowledgeBase = knowledgeBase;
        this.courseId = courseId;
        this.lessonId = lessonId;
    }

    @Tool(name = "searchCourseKnowledge",
            description = "从当前已授权课程上传的资料正文中检索与问题相关的片段；回答涉及资料具体内容（而非仅文件名）的问题时必须调用，参数为检索关键词或问题")
    public String searchCourseKnowledge(String query) {
        Retrieval retrieval = knowledgeBase.retrieve(courseId, lessonId, query == null ? "" : query.trim());
        if (!retrieval.vectorStoreAvailable()) {
            return "课程知识库尚未配置向量检索，无法按资料正文回答。";
        }
        if (!retrieval.matched()) {
            return "未检索到与该问题相关的资料正文片段，请结合课程目录或提示用户上传相关资料。";
        }
        return retrieval.context();
    }
}
```

- [ ] **Step 4: 把工具接入 `courseQa`**

在 `AiApplicationService.courseQa` 的 `generator.stream(...)` 工具可变参里追加新工具（当前 `AiApplicationService.java:103-108`）：

```java
                        generator.stream(
                                        prepared.systemPrompt(),
                                        prepared.userPrompt(),
                                        conversationId,
                                        platformUtilityTools,
                                        new CourseContextTools(prepared.context()),
                                        new CourseKnowledgeTools(knowledgeBase, courseId, lessonId))
```

（`knowledgeBase` 字段已存在于 `AiApplicationService`；`courseId`/`lessonId` 是 `courseQa` 的方法参数，直接可用。新增 import：`com.zhongruan.edu.ai.tools.CourseKnowledgeTools`。）

- [ ] **Step 5: 跑测试确认通过**

Run: `cd backend && ./mvnw -q -pl edu-ai-service test -Dtest=CourseKnowledgeToolsTest`
Expected: PASS（2 个用例）。

- [ ] **Step 6: 提交**

```bash
git add backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/tools/CourseKnowledgeTools.java \
        backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/application/AiApplicationService.java \
        backend/edu-ai-service/src/test/java/com/zhongruan/edu/ai/tools/CourseKnowledgeToolsTest.java
git commit -m "feat(ai): 新增 searchCourseKnowledge 工具，课程问答可按资料正文检索"
```

---

### Task 2: 内容哈希去重的惰性自动索引

**Files:**
- Modify: `backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/knowledge/CourseKnowledgeBaseService.java`（新增 `syncIfStale`，`SyncState` 增加 `contentHash`）
- Modify: `backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/application/AiApplicationService.java`（`courseQa` 拉到 context 后调用 `syncIfStale`）
- Test: `backend/edu-ai-service/src/test/java/com/zhongruan/edu/ai/knowledge/CourseKnowledgeAutoSyncTest.java`

**Interfaces:**
- Consumes: `AiCourseContextResponse`（含 `courseId()`、`lessons()`、`materials()`，material 有 `extractedText()`/`status()`）。
- Produces: `CourseKnowledgeBaseService.syncIfStale(AiCourseContextResponse context)` → `boolean`（true=本次触发了重建索引，false=内容未变已跳过）。

- [ ] **Step 1: 写失败测试**

```java
package com.zhongruan.edu.ai.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

class CourseKnowledgeAutoSyncTest {

    @SuppressWarnings("unchecked")
    private CourseKnowledgeBaseService serviceWith(VectorStore store) {
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return new CourseKnowledgeBaseService(provider);
    }

    private AiCourseContextResponse context(String materialBody) {
        AiMaterialRef material = new AiMaterialRef(
                10L, null, 1L, "第2章.pdf", "PDF", "key", "url",
                "COURSE", "PUBLISHED", materialBody, "EXTRACTED", null);
        AiLessonRef lesson = new AiLessonRef(1L, null, "第2章", "PUBLISHED", "TEXT", "章节说明", 45);
        return new AiCourseContextResponse(
                7L, "CS101", "课程", "PUBLISHED", "APPROVED", 99L, true, true,
                List.of(lesson), List.of(material));
    }

    @Test
    void firstCallIndexesAndSecondIdenticalCallSkips() {
        VectorStore store = mock(VectorStore.class);
        CourseKnowledgeBaseService service = serviceWith(store);

        boolean first = service.syncIfStale(context("向量检索基于近似最近邻搜索"));
        boolean second = service.syncIfStale(context("向量检索基于近似最近邻搜索"));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void changedContentReindexes() {
        VectorStore store = mock(VectorStore.class);
        CourseKnowledgeBaseService service = serviceWith(store);

        service.syncIfStale(context("旧正文"));
        boolean afterChange = service.syncIfStale(context("新正文，内容已更新"));

        assertThat(afterChange).isTrue();
    }
}
```

（注意：请先按当前仓库里 `AiLessonRef` / `AiMaterialRef` / `AiCourseContextResponse` 的真实构造参数顺序核对，若字段不同则据实调整测试里的构造调用；以 `edu-feign-api` 源码为准。）

- [ ] **Step 2: 跑测试确认失败**

Run: `cd backend && ./mvnw -q -pl edu-ai-service test -Dtest=CourseKnowledgeAutoSyncTest`
Expected: 编译失败（`syncIfStale` 不存在）。

- [ ] **Step 3: 实现 `syncIfStale` 并给 `SyncState` 加 `contentHash`**

在 `CourseKnowledgeBaseService` 中：把 `SyncState` 改为携带哈希，并新增方法。

```java
    // 替换原 record 定义
    private record SyncState(int indexedChunks, java.time.OffsetDateTime lastSyncedAt, String contentHash) {}
```

原 `sync(...)` 里构造 `SyncState` 处改为传入哈希（复用下方 `contentHash(context)`）：

```java
        syncStates.put(context.courseId(),
                new SyncState(documents.size(), OffsetDateTime.now(ZoneOffset.UTC), contentHash(context)));
```

`status(...)` 内对 `SyncState` 的读取不受影响（仍取 `indexedChunks()` / `lastSyncedAt()`）。新增：

```java
    /** 仅当课程资料/课时正文自上次索引后发生变化时才重建索引，避免每次问答都重复嵌入。返回是否本次重建。 */
    public boolean syncIfStale(AiCourseContextResponse context) {
        if (vectorStoreProvider.getIfAvailable() == null) {
            return false;
        }
        String hash = contentHash(context);
        SyncState existing = syncStates.get(context.courseId());
        if (existing != null && hash.equals(existing.contentHash())) {
            return false;
        }
        sync(context);
        return true;
    }

    private String contentHash(AiCourseContextResponse context) {
        StringBuilder builder = new StringBuilder();
        for (AiLessonRef lesson : context.lessons()) {
            builder.append('L').append(lesson.lessonId()).append('=')
                    .append(lesson.content() == null ? "" : lesson.content()).append('\n');
        }
        for (AiMaterialRef material : context.materials()) {
            builder.append('M').append(material.materialId()).append('=').append(material.status()).append(':')
                    .append(material.extractedText() == null ? "" : material.extractedText()).append('\n');
        }
        return UUID.nameUUIDFromBytes(builder.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }
```

（`AiLessonRef`/`AiMaterialRef`/`AiCourseContextResponse` 已在本类 import；`UUID`、`StandardCharsets` 也已 import。若 `AiLessonRef.content()` 的方法名与仓库不同，按真实签名调整 `contentHash`。）

- [ ] **Step 4: 在 `courseQa` 里触发惰性索引**

在 `AiApplicationService.courseQa` 的 `Mono.fromCallable` 内，取到 `context` 之后、`knowledgeBase.retrieve(...)` 之前插入一行：

```java
                    AiCourseContextResponse context = contextService.courseContext(
                            authorization, userId, role, courseId, lessonId, AiContextPurpose.COURSE_QA, traceId);
                    knowledgeBase.syncIfStale(context); // 内容变化时自动重建索引，无需手动同步
                    CourseKnowledgeBaseService.Retrieval retrieval = knowledgeBase.retrieve(courseId, lessonId, question.trim());
```

- [ ] **Step 5: 跑测试确认通过**

Run: `cd backend && ./mvnw -q -pl edu-ai-service test -Dtest=CourseKnowledgeAutoSyncTest`
Expected: PASS（2 个用例）。

- [ ] **Step 6: 跑该模块全量测试，确认无回归**

Run: `cd backend && ./mvnw -q -pl edu-ai-service test`
Expected: 全绿（含既有 `CourseKnowledgeBaseServiceTest` 等）。

- [ ] **Step 7: 提交**

```bash
git add backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/knowledge/CourseKnowledgeBaseService.java \
        backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/application/AiApplicationService.java \
        backend/edu-ai-service/src/test/java/com/zhongruan/edu/ai/knowledge/CourseKnowledgeAutoSyncTest.java
git commit -m "feat(ai): 课程问答惰性自动索引，资料变化即重建，去掉手动同步依赖"
```

---

## 后续 Phase（本计划不含，待 Phase 1 执行验收后各自成计划）

- **Phase 2**：自动流写框架骨架 + AI→biz 内部写 API（`BizAiAuthoringFeignClient`）+ `source` 字段 schema + `generateQuestionBank` 端到端。
- **Phase 3**：`assembleExamPaper` / `generateAssignment` / `draftLessonOutlineOrAnnouncement` 三条写工具。
- **Phase 4**：前端 AI 草稿角标（复用 `StatusBadge` amber）与确认发布 UI（复用 `AiResultPanel` 风格）。

## Self-Review 记录

- Spec 覆盖：本计划对应 spec「组件 1 — 内容级 RAG」全部要求（检索当工具 + 上传即索引，后者以惰性自动索引实现，避免新增 biz→AI 方向）。组件 2–5 归入后续 Phase。
- 占位符扫描：无 TBD/TODO；测试与实现均含完整代码。
- 类型一致性：`syncIfStale(AiCourseContextResponse)→boolean`、`CourseKnowledgeTools(CourseKnowledgeBaseService,Long,Long)`、`Retrieval.matched()/vectorStoreAvailable()/context()` 在任务间一致。
- 已知风险：feign record 构造参数顺序需以 `edu-feign-api` 源码为准（测试步骤已注明核对）。
