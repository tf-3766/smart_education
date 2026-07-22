package com.zhongruan.edu.biz.ai.api.controller;

import com.zhongruan.edu.biz.ai.application.AiActionService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiActionPlanRequest;
import com.zhongruan.edu.feign.ai.AiActionResponse;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BizAiActionFeignClient.BASE_PATH)
public class InternalAiActionController implements BizAiActionFeignClient {
    private final AiActionService service;
    private final HttpServletRequest servletRequest;

    public InternalAiActionController(AiActionService service, HttpServletRequest servletRequest) {
        this.service = service;
        this.servletRequest = servletRequest;
    }

    @Override
    public ApiResponse<AiActionResponse> plan(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiActionPlanRequest request) {
        requireIdentity(request.userId(), request.roleCode());
        String traceId = RequestTrace.from(servletRequest);
        return ApiResponse.success(service.plan(request, traceId), traceId);
    }

    private void requireIdentity(Long userId, String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (!user.userId().equals(userId) || !user.activeRole().equals(roleCode)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "AI 动作身份与访问令牌不一致");
        }
    }
}
