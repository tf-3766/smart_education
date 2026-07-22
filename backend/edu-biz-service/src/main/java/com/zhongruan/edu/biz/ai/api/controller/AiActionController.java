package com.zhongruan.edu.biz.ai.api.controller;

import com.zhongruan.edu.biz.ai.application.AiActionService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiActionResponse;
import com.zhongruan.edu.feign.ai.AiActionConfirmRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant-actions")
@PreAuthorize("isAuthenticated()")
public class AiActionController {
    private final AiActionService service;
    private final RequestContextFactory contextFactory;

    public AiActionController(AiActionService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<List<AiActionResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.listMine(user.userId(), user.activeRole(), limit), trace(request));
    }

    @GetMapping("/{actionId}")
    public ApiResponse<AiActionResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long actionId,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.get(user.userId(), user.activeRole(), actionId), trace(request));
    }

    @PostMapping("/{actionId}/confirm")
    public ApiResponse<AiActionResponse> confirm(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long actionId,
            @RequestBody(required = false) @jakarta.validation.Valid AiActionConfirmRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.confirm(
                        user.userId(), user.activeRole(), actionId,
                        body == null ? null : body.confirmationText()),
                trace(request));
    }

    @PostMapping("/{actionId}/cancel")
    public ApiResponse<AiActionResponse> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long actionId,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.cancel(user.userId(), user.activeRole(), actionId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
