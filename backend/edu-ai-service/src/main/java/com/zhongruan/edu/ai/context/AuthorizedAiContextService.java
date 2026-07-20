package com.zhongruan.edu.ai.context;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiAssistantContextRequest;
import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import com.zhongruan.edu.feign.ai.AiContextPurpose;
import com.zhongruan.edu.feign.ai.AiCourseContextRequest;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiPaperContextRequest;
import com.zhongruan.edu.feign.ai.AiPaperContextResponse;
import com.zhongruan.edu.feign.ai.AiResourceContextRequest;
import com.zhongruan.edu.feign.ai.AiSubmissionContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningContextResponse;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthorizedAiContextService {
    private static final Logger log = LoggerFactory.getLogger(AuthorizedAiContextService.class);
    private final BizAiContextFeignClient contextClient;

    public AuthorizedAiContextService(BizAiContextFeignClient contextClient) {
        this.contextClient = contextClient;
    }

    public AiAssistantContextResponse assistantContext(
            String authorization, Long userId, String role, String traceId) {
        try {
            ApiResponse<AiAssistantContextResponse> response = contextClient.getAssistantContext(
                    authorization, new AiAssistantContextRequest(userId, role, traceId));
            return requireData(response);
        } catch (FeignException exception) {
            throw translate(exception);
        }
    }

    public AiCourseContextResponse courseContext(
            String authorization,
            Long userId,
            String role,
            Long courseId,
            Long lessonId,
            AiContextPurpose purpose,
            String traceId) {
        ApiResponse<AiCourseContextResponse> response;
        try {
            response = contextClient.getCourseContext(
                    authorization,
                    new AiCourseContextRequest(userId, role, courseId, lessonId, null, purpose, traceId));
        } catch (FeignException exception) {
            throw translate(exception);
        }
        if (response == null || response.data() == null || !"SUCCESS".equals(response.code())) {
            throw new BusinessException(CommonErrorCode.AI_SERVICE_UNAVAILABLE, "无法获取授权课程上下文");
        }
        return response.data();
    }

    public AiSubmissionContextResponse submissionContext(
            String authorization, Long userId, String role, Long submissionId, String traceId) {
        try {
            ApiResponse<AiSubmissionContextResponse> response = contextClient.getSubmissionContext(
                    authorization,
                    new AiResourceContextRequest(
                            userId, role, submissionId, AiContextPurpose.GRADING_COMMENT_DRAFT, traceId));
            return requireData(response);
        } catch (FeignException exception) {
            throw translate(exception);
        }
    }

    public AiWarningContextResponse warningContext(
            String authorization, Long userId, String role, Long warningId, String traceId) {
        try {
            ApiResponse<AiWarningContextResponse> response = contextClient.getWarningContext(
                    authorization,
                    new AiResourceContextRequest(userId, role, warningId, AiContextPurpose.RISK_EXPLANATION, traceId));
            return requireData(response);
        } catch (FeignException exception) {
            throw translate(exception);
        }
    }

    public AiPaperContextResponse paperContext(
            String authorization, Long userId, String role, Long courseId, String traceId) {
        try {
            ApiResponse<AiPaperContextResponse> response = contextClient.getPaperContext(
                    authorization,
                    new AiPaperContextRequest(userId, role, courseId, AiContextPurpose.PAPER_SUGGESTION, traceId));
            return requireData(response);
        } catch (FeignException exception) {
            throw translate(exception);
        }
    }

    private <T> T requireData(ApiResponse<T> response) {
        if (response == null || response.data() == null || !"SUCCESS".equals(response.code())) {
            throw new BusinessException(CommonErrorCode.AI_SERVICE_UNAVAILABLE, "无法获取授权业务上下文");
        }
        return response.data();
    }

    private BusinessException translate(FeignException exception) {
        log.warn("Authorized Biz context request failed. status={} url={} response={}",
                exception.status(), exception.request() == null ? null : exception.request().url(),
                exception.contentUTF8());
        log.warn("Authorized Biz context stack", exception);
        CommonErrorCode errorCode = switch (exception.status()) {
            case 400 -> CommonErrorCode.PARAM_VALIDATION_ERROR;
            case 401 -> CommonErrorCode.UNAUTHORIZED;
            case 403 -> CommonErrorCode.FORBIDDEN;
            case 404 -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case 409 -> CommonErrorCode.RESOURCE_CONFLICT;
            default -> CommonErrorCode.AI_SERVICE_UNAVAILABLE;
        };
        return new BusinessException(errorCode);
    }
}
