package com.zhongruan.edu.ai.api.controller;

import com.zhongruan.edu.ai.api.dto.CourseQaRequest;
import com.zhongruan.edu.ai.api.dto.AssistantChatRequest;
import com.zhongruan.edu.ai.api.dto.AiDraftInstructionRequest;
import com.zhongruan.edu.ai.api.dto.LessonSummaryRequest;
import com.zhongruan.edu.ai.api.dto.PaperSuggestionRequest;
import com.zhongruan.edu.ai.api.vo.AiDraftVO;
import com.zhongruan.edu.ai.api.vo.AiKnowledgeBaseStatusVO;
import com.zhongruan.edu.ai.api.vo.AiServiceStatusVO;
import com.zhongruan.edu.ai.api.vo.AiStreamEvent;
import com.zhongruan.edu.ai.application.AiApplicationService;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private static final Set<String> QA_ROLES = Set.of("STUDENT", "TEACHER", "ADMIN", "SUPER_ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");

    private final AiApplicationService service;

    public AiController(AiApplicationService service) {
        this.service = service;
    }

    @PostMapping(value = "/courses/{courseId}/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiStreamEvent>> courseQa(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseQaRequest body) {
        requireRole(role, QA_ROLES);
        return service.courseQa(
                        authorization, userId, role, courseId, body.lessonId(), body.question(), body.conversationId(), traceId)
                .map(event -> ServerSentEvent.<AiStreamEvent>builder(event)
                        .id(event.requestId())
                        .event(event.type())
                        .build());
    }

    @PostMapping(value = "/assistant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiStreamEvent>> assistant(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @Valid @RequestBody AssistantChatRequest body) {
        requireRole(role, Set.of("STUDENT", "TEACHER", "ADMIN", "SUPER_ADMIN"));
        return service.assistantChat(
                        authorization, userId, role, body.courseId(), body.lessonId(), body.pagePath(),
                        body.pageTitle(), body.question(), body.conversationId(), traceId)
                .map(event -> ServerSentEvent.<AiStreamEvent>builder(event)
                        .id(event.requestId())
                        .event(event.type())
                        .build());
    }
    @GetMapping("/courses/{courseId}/knowledge-base/status")
    public Mono<ApiResponse<AiKnowledgeBaseStatusVO>> knowledgeBaseStatus(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long courseId) {
        requireRole(role, Set.of("TEACHER"));
        return service.knowledgeBaseStatus(authorization, userId, role, courseId, traceId)
                .map(status -> ApiResponse.success(status, traceId));
    }

    @PostMapping("/courses/{courseId}/knowledge-base/sync")
    public Mono<ApiResponse<AiKnowledgeBaseStatusVO>> syncKnowledgeBase(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long courseId) {
        requireRole(role, Set.of("TEACHER"));
        return service.syncKnowledgeBase(authorization, userId, role, courseId, traceId)
                .map(status -> ApiResponse.success(status, traceId));
    }
    @PostMapping("/lessons/{lessonId}/summary-draft")
    public Mono<ApiResponse<AiDraftVO>> lessonSummary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonSummaryRequest body) {
        requireRole(role, Set.of("TEACHER"));
        return service.lessonSummary(authorization, userId, role, body.courseId(), lessonId, traceId)
                .map(draft -> ApiResponse.success(draft, traceId));
    }

    @PostMapping("/submissions/{submissionId}/comment-draft")
    public Mono<ApiResponse<AiDraftVO>> submissionComment(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long submissionId,
            @Valid @RequestBody AiDraftInstructionRequest body) {
        requireRole(role, Set.of("TEACHER"));
        return service.submissionComment(
                        authorization, userId, role, submissionId, body.instruction(), traceId)
                .map(draft -> ApiResponse.success(draft, traceId));
    }

    @PostMapping("/warnings/{warningId}/explanation")
    public Mono<ApiResponse<AiDraftVO>> warningExplanation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @PathVariable Long warningId,
            @Valid @RequestBody AiDraftInstructionRequest body) {
        requireRole(role, Set.of("TEACHER"));
        return service.warningExplanation(
                        authorization, userId, role, warningId, body.instruction(), traceId)
                .map(draft -> ApiResponse.success(draft, traceId));
    }

    @PostMapping("/exams/paper-suggestions")
    public Mono<ApiResponse<AiDraftVO>> paperSuggestion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @Valid @RequestBody PaperSuggestionRequest body) {
        requireRole(role, Set.of("TEACHER"));
        return service.paperSuggestion(
                        authorization,
                        userId,
                        role,
                        body.courseId(),
                        body.questionCount(),
                        body.totalScore(),
                        body.requirements(),
                        traceId)
                .map(draft -> ApiResponse.success(draft, traceId));
    }

    @GetMapping("/admin/status")
    public ApiResponse<AiServiceStatusVO> status(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId) {
        requireRole(role, ADMIN_ROLES);
        return ApiResponse.success(service.status(), traceId);
    }

    private void requireRole(String role, Set<String> allowed) {
        if (!allowed.contains(role)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
