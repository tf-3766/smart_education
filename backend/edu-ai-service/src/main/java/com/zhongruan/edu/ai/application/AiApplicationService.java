package com.zhongruan.edu.ai.application;

import com.zhongruan.edu.ai.api.vo.AiCitationVO;
import com.zhongruan.edu.ai.api.vo.AiDraftVO;
import com.zhongruan.edu.ai.api.vo.AiServiceStatusVO;
import com.zhongruan.edu.ai.api.vo.AiStreamEvent;
import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.generation.AiTextGenerator;
import com.zhongruan.edu.feign.ai.AiContextPurpose;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AiApplicationService {
    private final AuthorizedAiContextService contextService;
    private final AiTextGenerator generator;
    private final Clock clock = Clock.systemUTC();

    public AiApplicationService(AuthorizedAiContextService contextService, AiTextGenerator generator) {
        this.contextService = contextService;
        this.generator = generator;
    }

    public Flux<AiStreamEvent> courseQa(
            String authorization,
            Long userId,
            String role,
            Long courseId,
            Long lessonId,
            String question,
            String traceId) {
        String requestId = UUID.randomUUID().toString();
        return Mono.fromCallable(() -> {
                    AiCourseContextResponse context = contextService.courseContext(
                            authorization,
                            userId,
                            role,
                            courseId,
                            lessonId,
                            AiContextPurpose.COURSE_QA,
                            traceId);
                    return new PreparedRequest(
                            systemPrompt(context, "回答课程问题，只能使用提供的课程上下文；上下文不足时明确说明。"),
                            "问题：" + question.trim(),
                            citations(context, lessonId));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(prepared -> Flux.concat(
                        Flux.just(event("meta", requestId, new Meta(
                                courseId, generator.provider(), generator.model(), generator.configured()))),
                        generator.stream(prepared.systemPrompt(), prepared.userPrompt())
                                .filter(chunk -> chunk != null && !chunk.isEmpty())
                                .map(chunk -> event("delta", requestId, chunk)),
                        Flux.fromIterable(prepared.citations())
                                .map(citation -> event("citation", requestId, citation)),
                        Flux.just(event("done", requestId, new Done("COMPLETED")))))
                .onErrorResume(error -> Flux.just(event(
                        "error",
                        requestId,
                        new ErrorData("AI_SERVICE_UNAVAILABLE", "AI 请求暂时无法完成"))));
    }

    public Mono<AiDraftVO> lessonSummary(
            String authorization,
            Long userId,
            String role,
            Long courseId,
            Long lessonId,
            String traceId) {
        return Mono.fromCallable(() -> {
                    AiCourseContextResponse context = contextService.courseContext(
                            authorization,
                            userId,
                            role,
                            courseId,
                            lessonId,
                            AiContextPurpose.LESSON_SUMMARY_DRAFT,
                            traceId);
                    String content = generator.generate(
                            systemPrompt(context, "生成结构清晰的课时摘要草稿，不得修改正式课程内容。"),
                            "请为课时 " + lessonId + " 生成摘要草稿。上下文不足时明确说明。 ");
                    return new AiDraftVO(
                            UUID.randomUUID().toString(),
                            "LESSON_SUMMARY",
                            String.valueOf(lessonId),
                            content,
                            generator.provider(),
                            generator.model(),
                            generator.configured() ? "DRAFT" : "FRAMEWORK_ONLY",
                            citations(context, lessonId),
                            now());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public AiServiceStatusVO status() {
        return new AiServiceStatusVO(
                "UP",
                "Spring AI",
                "1.1.8",
                generator.provider(),
                generator.model(),
                generator.configured(),
                false,
                now());
    }

    private String systemPrompt(AiCourseContextResponse context, String instruction) {
        String lessonNames = context.lessons().stream()
                .map(AiLessonRef::title)
                .limit(20)
                .reduce((left, right) -> left + "、" + right)
                .orElse("无可用课时");
        String materialNames = context.materials().stream()
                .map(AiMaterialRef::name)
                .limit(20)
                .reduce((left, right) -> left + "、" + right)
                .orElse("无可用资料");
        return """
                你是在线教育辅助教学系统中的课程助手。
                课程：%s（%s）
                可见课时：%s
                可见资料：%s
                约束：%s 不展示模型内部推理，不虚构引用。
                """.formatted(
                context.courseName(), context.courseCode(), lessonNames, materialNames, instruction);
    }

    private List<AiCitationVO> citations(AiCourseContextResponse context, Long lessonId) {
        List<AiCitationVO> values = new ArrayList<>();
        context.lessons().stream()
                .filter(lesson -> lessonId == null || lessonId.equals(lesson.lessonId()))
                .limit(3)
                .map(lesson -> new AiCitationVO(
                        "LESSON", String.valueOf(lesson.lessonId()), lesson.title(), "lesson:" + lesson.lessonId()))
                .forEach(values::add);
        context.materials().stream()
                .filter(material -> lessonId == null || lessonId.equals(material.lessonId()))
                .limit(5)
                .map(material -> new AiCitationVO(
                        "MATERIAL",
                        String.valueOf(material.materialId()),
                        material.name(),
                        material.fileUrl() != null ? material.fileUrl() : material.fileKey()))
                .forEach(values::add);
        return List.copyOf(values);
    }

    private AiStreamEvent event(String type, String requestId, Object data) {
        return new AiStreamEvent(type, requestId, data, now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private record PreparedRequest(
            String systemPrompt,
            String userPrompt,
            List<AiCitationVO> citations) {}

    private record Meta(Long courseId, String provider, String model, boolean modelConfigured) {}

    private record Done(String status) {}

    private record ErrorData(String code, String message) {}
}
