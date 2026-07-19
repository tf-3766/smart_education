package com.zhongruan.edu.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.generation.AiTextGenerator;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.tools.PlatformUtilityTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

        new AiApplicationService(contextService, generator, knowledgeBase, new PlatformUtilityTools(new ObjectMapper()))
                .courseQa("Bearer token", 1002L, "TEACHER", 21001L, 23001L, "什么是依赖注入？", "lesson-1", "trace")
                .collectList()
                .block();

        assertThat(systemPrompt.get()).contains("依赖注入通过容器提供对象所需依赖，从而降低耦合。");
    }

    @Test
    void globalAssistantPromptContainsRoleScopedDatabaseFacts() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        when(contextService.assistantContext(anyString(), any(), anyString(), anyString()))
                .thenReturn(new AiAssistantContextResponse(
                        1001L,
                        "student",
                        "STUDENT",
                        OffsetDateTime.of(2026, 7, 19, 12, 0, 0, 0, ZoneOffset.UTC),
                        List.of("学期 2027 春季：开放 2027-01-10T00:00，截止 2027-02-20T23:59"),
                        List.of("软件工程（课程代码 SE-101，课程ID 21001，学期 2027 春季，状态 PUBLISHED）"),
                        List.of("软件工程：级别 MEDIUM，状态 OPEN，学习进度低于课程节奏；建议：本周完成补学"),
                        List.of(),
                        List.of(),
                        List.of()));
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AiTextGenerator generator = new CapturingGenerator(systemPrompt);
        CourseKnowledgeBaseService knowledgeBase = mock(CourseKnowledgeBaseService.class);

        new AiApplicationService(contextService, generator, knowledgeBase, new PlatformUtilityTools(new ObjectMapper()))
                .assistantChat(
                        "Bearer token", 1001L, "STUDENT", null, null, "/student/dashboard", "学生首页",
                        "我有哪些学习预警？", "global-1", "trace")
                .collectList()
                .block();

        assertThat(systemPrompt.get())
                .contains("2027 春季")
                .contains("学习进度低于课程节奏")
                .contains("当前 JWT 身份和角色从真实数据库实时读取");
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
}
