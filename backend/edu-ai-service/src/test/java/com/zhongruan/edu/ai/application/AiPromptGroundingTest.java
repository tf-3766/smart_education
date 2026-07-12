package com.zhongruan.edu.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.generation.AiTextGenerator;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
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

        new AiApplicationService(contextService, generator)
                .courseQa("Bearer token", 1002L, "TEACHER", 21001L, 23001L, "什么是依赖注入？", "trace")
                .collectList()
                .block();

        assertThat(systemPrompt.get()).contains("依赖注入通过容器提供对象所需依赖，从而降低耦合。");
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
