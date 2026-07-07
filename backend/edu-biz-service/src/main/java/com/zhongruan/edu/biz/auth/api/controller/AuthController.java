package com.zhongruan.edu.biz.auth.api.controller;

import com.zhongruan.edu.biz.auth.api.dto.request.LoginRequest;
import com.zhongruan.edu.biz.auth.api.vo.CurrentUserVO;
import com.zhongruan.edu.biz.auth.api.vo.LoginVO;
import com.zhongruan.edu.biz.auth.api.vo.LogoutVO;
import com.zhongruan.edu.biz.auth.application.service.AuthApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthApplicationService authApplicationService;
    private final RequestContextFactory requestContextFactory;

    public AuthController(
            AuthApplicationService authApplicationService, RequestContextFactory requestContextFactory) {
        this.authApplicationService = authApplicationService;
        this.requestContextFactory = requestContextFactory;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(
                authApplicationService.login(request), requestContextFactory.current(servletRequest).traceId());
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me(
            @AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest servletRequest) {
        return ApiResponse.success(
                authApplicationService.currentUser(principal), requestContextFactory.current(servletRequest).traceId());
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutVO> logout(HttpServletRequest servletRequest) {
        return ApiResponse.success(
                LogoutVO.clientDiscardOnly(), requestContextFactory.current(servletRequest).traceId());
    }
}
