package com.zhongruan.edu.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.generation.AiTextGenerator;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.tools.PlatformUtilityTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AiPromptGroundingTest {
    @Test
    void courseQuestionPromptContainsAuthorizedLessonBody() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.courseContext(anyString(), any(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(new AiCourseContextResponse(
                        21001L,
                        "COURSE-001",
                        "Spring 课程",
                        "PUBLISHED",
                        "APPROVED",
                        1002L,
                        true,
                        false,
                        List.of(new AiLessonRef(
                                23001L,
                                22001L,
                                "依赖注入",
                                "PUBLISHED",
                                "RICH_TEXT",
                                "依赖注入通过容器提供对象所需依赖，从而降低耦合。",
                                30)),
                        List.of()));
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AiTextGenerator generator = new CapturingGenerator(systemPrompt);
        CourseKnowledgeBaseService knowledgeBase = mock(CourseKnowledgeBaseService.class);
        when(knowledgeBase.retrieve(any(), any(), anyString()))
                .thenReturn(new CourseKnowledgeBaseService.Retrieval(false, "", List.of()));

        new AiApplicationService(contextService, generator, knowledgeBase, new PlatformUtilityTools(new ObjectMapper()),
                        mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .courseQa("Bearer token", 1002L, "TEACHER", 21001L, 23001L, "什么是依赖注入？", "lesson-1", "trace")
                .collectList()
                .block();

        assertThat(systemPrompt.get()).contains("依赖注入通过容器提供对象所需依赖，从而降低耦合。");
    }

    @Test
    void globalAssistantPromptIncludesTheAuthorizedPurposeScopedFacts() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new AiAssistantContextResponse(
                        1001L,
                        "student",
                        "STUDENT",
                        OffsetDateTime.of(2026, 7, 19, 12, 0, 0, 0, ZoneOffset.UTC),
                        List.of("学期 2027 春季：开放 2027-01-10T00:00，截止 2027-02-20T23:59"),
                        List.of("软件工程（课程代码 SE-101，课程ID 21001，学期 2027 春季，状态 PUBLISHED）"),
                        List.of("软件工程：级别 MEDIUM，状态 OPEN，学习进度低于课程节奏；建议：本周完成补学"),
                        List.of("软件工程：第一章作业，状态 PUBLISHED，截止 2027-02-01T23:59"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AiTextGenerator generator = new CapturingGenerator(systemPrompt);
        CourseKnowledgeBaseService knowledgeBase = mock(CourseKnowledgeBaseService.class);

        new AiApplicationService(contextService, generator, knowledgeBase, new PlatformUtilityTools(new ObjectMapper()),
                        mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .assistantChat(
                        "Bearer token", 1001L, "STUDENT", null, null, "/student/dashboard", "学生首页",
                        "我有哪些学习预警？", "global-1", "trace")
                .collectList()
                .block();

        assertThat(systemPrompt.get())
                .contains("已授权数据计数")
                .contains("当前 JWT 身份和角色从真实数据库实时读取")
                .contains("软件工程（课程代码 SE-101")
                .contains("学习进度低于课程节奏", "本周完成补学")
                .contains("作业：\n- 未请求此事实域");
        assertThat(systemPrompt.get())
                .doesNotContain("第一章作业", "截止 2027-02-01T23:59", "平台用户手机号");
    }

    @Test
    void broadQuantityWordDoesNotExpandBeyondTheRequestedFactDomain() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new AiAssistantContextResponse(
                        1001L, "student", "STUDENT", OffsetDateTime.now(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of()));
        AiTextGenerator generator = new CapturingGenerator(new AtomicReference<>());
        new AiApplicationService(contextService, generator, mock(CourseKnowledgeBaseService.class),
                        new PlatformUtilityTools(new ObjectMapper()), mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .assistantChat("Bearer token", 1001L, "STUDENT", null, null, "/student/exams", "考试",
                        "查看全部考试", "global-2", "trace")
                .collectList().block();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> domains = ArgumentCaptor.forClass(Set.class);
        verify(contextService).assistantContext(
                eq("Bearer token"), eq(1001L), eq("STUDENT"), eq("trace"), domains.capture());
        assertThat(domains.getValue()).containsExactly("EXAMS");
    }

    @Test
    void pendingTeacherApprovalIntentLoadsTheUsersDomain() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new AiAssistantContextResponse(
                        1003L, "admin", "SUPER_ADMIN", OffsetDateTime.now(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of()));
        AiTextGenerator generator = new CapturingGenerator(new AtomicReference<>());

        new AiApplicationService(contextService, generator, mock(CourseKnowledgeBaseService.class),
                        new PlatformUtilityTools(new ObjectMapper()), mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .assistantChat("Bearer token", 1003L, "SUPER_ADMIN", null, null, "/admin/users", "用户管理",
                        "给所有待审核教师都审批通过", "admin-pending-1", "trace")
                .collectList().block();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> domains = ArgumentCaptor.forClass(Set.class);
        verify(contextService).assistantContext(
                eq("Bearer token"), eq(1003L), eq("SUPER_ADMIN"), eq("trace"), domains.capture());
        assertThat(domains.getValue()).contains("USERS");
    }

    @Test
    void globalOverviewQuestionOnCoursePageDoesNotFallBackToCourseQa() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new AiAssistantContextResponse(
                        1021L, "teacher_math", "TEACHER", OffsetDateTime.now(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of()));
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AiTextGenerator generator = new CapturingGenerator(systemPrompt);

        new AiApplicationService(contextService, generator, mock(CourseKnowledgeBaseService.class),
                        new PlatformUtilityTools(new ObjectMapper()), mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .assistantChat("Bearer token", 1021L, "TEACHER", 22003L, null,
                        "/teacher/courses/22003/content", "课程内容",
                        "请汇总我的完整教学概况、待批改提交和学生预警，只查询不修改数据",
                        "teacher-global-1", "trace")
                .collectList().block();

        verify(contextService).assistantContext(eq("Bearer token"), eq(1021L), eq("TEACHER"), eq("trace"), any());
        assertThat(systemPrompt.get()).contains("全局教学智能助手");
    }

    @Test
    void readOnlyGlobalSummaryFallsBackToAuthorizedFactsWhenTheModelFails() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new AiAssistantContextResponse(
                        1021L, "teacher_math", "TEACHER", OffsetDateTime.now(), List.of(),
                        List.of("高等数学基础（课程ID 22003，状态 PUBLISHED）"),
                        List.of("赵明远：高风险，学习进度落后"), List.of(), List.of(), List.of(), List.of(), List.of()));

        var events = new AiApplicationService(contextService, new FailingGenerator(),
                        mock(CourseKnowledgeBaseService.class), new PlatformUtilityTools(new ObjectMapper()),
                        mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .assistantChat("Bearer token", 1021L, "TEACHER", null, null, "/teacher", "教学工作台",
                        "请汇总我的完整教学概况和学生预警，只查询不修改数据", "teacher-fallback-1", "trace")
                .collectList().block();

        assertThat(events).noneMatch(event -> "error".equals(event.type()));
        assertThat(events).anyMatch(event -> "delta".equals(event.type())
                && String.valueOf(event.data()).contains("高等数学基础")
                && String.valueOf(event.data()).contains("赵明远"));
    }

    @Test
    void adminOperationsBriefForbidsFalseMetricMismatchAndFakeConfirmationPlans() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString()))
                .thenReturn(new AiAssistantContextResponse(
                        1003L, "admin", "SUPER_ADMIN", OffsetDateTime.now(), List.of(),
                        List.of("Java 程序设计（课程ID 22001，状态 PUBLISHED）"), List.of(), List.of(), List.of(),
                        List.of("开放学习预警：7", "指标口径说明：上述数量直接实时统计业务表；管理员上下文中明细未提供不代表底层表为空，不得据此判断指标失真。"),
                        List.of(), List.of()));
        AtomicReference<String> systemPrompt = new AtomicReference<>();

        new AiApplicationService(contextService, new CapturingGenerator(systemPrompt),
                        mock(CourseKnowledgeBaseService.class), new PlatformUtilityTools(new ObjectMapper()),
                        mock(BizAiAuthoringFeignClient.class),
                        mock(com.zhongruan.edu.feign.ai.BizAiActionFeignClient.class))
                .adminOperationsBrief("Bearer token", 1003L, "SUPER_ADMIN", "仅列出有证据的异常", "trace")
                .block();

        assertThat(systemPrompt.get())
                .contains("不得把未提供明细解释为底层表为空或指标失真")
                .contains("只有工具真实返回 actionId")
                .contains("不得声称处于 WAITING_CONFIRMATION")
                .contains("预警重算", "课程归档", "批量通知", "公告受众修改")
                .contains("ARCHIVED 不是合法课程状态")
                .contains("同一课程代码跨教师或学期多次开课属于正常业务")
                .contains("DRAFT 未进入审核、OFFLINE 计入课程总数均不是异常")
                .contains("只有事实被明确标记为经规则确认的异常信号")
                .contains("不得自行统计或补全数量");
    }

    private record CapturingGenerator(AtomicReference<String> systemPrompt) implements AiTextGenerator {
        @Override
        public Flux<String> stream(String systemPrompt, String userPrompt) {
            this.systemPrompt.set(systemPrompt);
            return Flux.just("回答");
        }

        @Override
        public String generate(String systemPrompt, String userPrompt) {
            this.systemPrompt.set(systemPrompt);
            return "草稿";
        }

        @Override
        public String provider() {
            return "test";
        }

        @Override
        public String model() {
            return "test-model";
        }

        @Override
        public boolean configured() {
            return true;
        }
    }

    private static final class FailingGenerator implements AiTextGenerator {
        @Override public Flux<String> stream(String systemPrompt, String userPrompt) {
            return Flux.error(new IllegalStateException("temporary model failure"));
        }
        @Override public String generate(String systemPrompt, String userPrompt) {
            throw new IllegalStateException("temporary model failure");
        }
        @Override public String provider() { return "test"; }
        @Override public String model() { return "test-model"; }
        @Override public boolean configured() { return true; }
    }
}
