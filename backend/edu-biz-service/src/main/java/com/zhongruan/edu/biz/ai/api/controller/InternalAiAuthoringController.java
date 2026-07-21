package com.zhongruan.edu.biz.ai.api.controller;

import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentCreateRequest;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.application.service.AssignmentApplicationService;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateExamRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionBankRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.QuestionOptionRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionBankVO;
import com.zhongruan.edu.biz.exam.application.service.ExamManagementService;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionDifficulty;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionType;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.vo.AnnouncementVO;
import com.zhongruan.edu.biz.platform.application.service.AnnouncementApplicationService;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementAudience;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiAnnouncementDraftRequest;
import com.zhongruan.edu.feign.ai.AiAssignmentDraftRequest;
import com.zhongruan.edu.feign.ai.AiAuthoringResultResponse;
import com.zhongruan.edu.feign.ai.AiExamDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionBankDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionDraft;
import com.zhongruan.edu.feign.ai.AiQuestionOptionDraft;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 服务的内部写授权入口。与 {@link InternalAiContextController} 一样透传用户 JWT：
 * 先用 JWT 主体核对声明身份，再交由领域服务做课程归属二次校验，最终落 source=AI 草稿。
 */
@RestController
@RequestMapping(BizAiAuthoringFeignClient.BASE_PATH)
public class InternalAiAuthoringController implements BizAiAuthoringFeignClient {
    private final ExamManagementService examManagementService;
    private final AssignmentApplicationService assignmentApplicationService;
    private final AnnouncementApplicationService announcementApplicationService;
    private final HttpServletRequest servletRequest;

    public InternalAiAuthoringController(
            ExamManagementService examManagementService,
            AssignmentApplicationService assignmentApplicationService,
            AnnouncementApplicationService announcementApplicationService,
            HttpServletRequest servletRequest) {
        this.examManagementService = examManagementService;
        this.assignmentApplicationService = assignmentApplicationService;
        this.announcementApplicationService = announcementApplicationService;
        this.servletRequest = servletRequest;
    }

    @Override
    public ApiResponse<AiAuthoringResultResponse> createQuestionBank(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiQuestionBankDraftRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        CreateQuestionBankRequest bankRequest =
                new CreateQuestionBankRequest(request.bankName(), request.description());
        List<CreateQuestionRequest> questions = request.questions().stream()
                .map(InternalAiAuthoringController::toCreateQuestion)
                .toList();
        QuestionBankVO bank = examManagementService.createAiDraftQuestionBank(
                user.userId(), request.courseId(), bankRequest, questions);
        return ApiResponse.success(
                new AiAuthoringResultResponse("QUESTION_BANK", bank.bankId(), bank.name(), questions.size()),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiAuthoringResultResponse> createAssignment(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAssignmentDraftRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        int dueInDays = request.dueInDays() == null || request.dueInDays() < 1 ? 7 : request.dueInDays();
        OffsetDateTime dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(dueInDays);
        AssignmentCreateRequest createRequest = new AssignmentCreateRequest(
                request.lessonId(), request.title(), request.description(), "TEXT",
                null, request.maxScore(), null, dueAt, null);
        AssignmentDetailVO created =
                assignmentApplicationService.createAiDraftAssignment(user.userId(), request.courseId(), createRequest);
        return ApiResponse.success(
                new AiAuthoringResultResponse("ASSIGNMENT", created.assignmentId(), created.title(), 0),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiAuthoringResultResponse> createExam(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiExamDraftRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        CreateExamRequest createRequest = new CreateExamRequest(
                request.title(), request.description(), null, null,
                request.durationMinutes(), request.totalScore());
        ExamVO created = examManagementService.createAiDraftExam(user.userId(), request.courseId(), createRequest);
        return ApiResponse.success(
                new AiAuthoringResultResponse("EXAM", created.examId(), created.title(), 0),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiAuthoringResultResponse> createAnnouncement(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAnnouncementDraftRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        CreateAnnouncementRequest createRequest = new CreateAnnouncementRequest(
                request.title(), request.content(), parseAudience(request.audience()));
        AnnouncementVO created = announcementApplicationService.createCourseAnnouncementDraft(
                user.userId(), request.courseId(), createRequest);
        return ApiResponse.success(
                new AiAuthoringResultResponse("ANNOUNCEMENT", created.announcementId(), created.title(), 0),
                RequestTrace.from(servletRequest));
    }

    private static AnnouncementAudience parseAudience(String value) {
        if (value == null || value.isBlank()) {
            return AnnouncementAudience.ALL;
        }
        try {
            return AnnouncementAudience.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "AI 生成了未知公告受众：" + value);
        }
    }

    private static CreateQuestionRequest toCreateQuestion(AiQuestionDraft draft) {
        List<QuestionOptionRequest> options = draft.options() == null
                ? List.of()
                : draft.options().stream().map(InternalAiAuthoringController::toOption).toList();
        return new CreateQuestionRequest(
                parseType(draft.questionType()),
                draft.stem(),
                draft.analysis(),
                parseDifficulty(draft.difficulty()),
                draft.score(),
                options);
    }

    private static QuestionOptionRequest toOption(AiQuestionOptionDraft option) {
        return new QuestionOptionRequest(option.label(), option.content(), option.correct(), option.sortOrder());
    }

    private static QuestionType parseType(String value) {
        try {
            return QuestionType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "AI 生成了未知题型：" + value);
        }
    }

    private static QuestionDifficulty parseDifficulty(String value) {
        try {
            return QuestionDifficulty.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "AI 生成了未知难度：" + value);
        }
    }

    private AuthenticatedUser requireIdentity(Long userId, String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (!user.userId().equals(userId) || !user.activeRole().equals(roleCode)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "AI 写授权身份与访问令牌不一致");
        }
        return user;
    }
}
