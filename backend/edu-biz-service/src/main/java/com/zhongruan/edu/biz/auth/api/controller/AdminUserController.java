package com.zhongruan.edu.biz.auth.api.controller;

import com.zhongruan.edu.biz.auth.api.dto.query.AdminUserQuery;
import com.zhongruan.edu.biz.auth.api.vo.AdminUserVO;
import com.zhongruan.edu.biz.auth.application.service.AdminUserApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).SUPER_ADMIN.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_MANAGE.code())")
public class AdminUserController {
    private final AdminUserApplicationService service;
    private final RequestContextFactory contextFactory;

    public AdminUserController(AdminUserApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid AdminUserQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listUsers(user.userId(), query), trace(request));
    }

    @PutMapping("/{userId}/administrator")
    public ApiResponse<AdminUserVO> grantAdministrator(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest request) {
        return ApiResponse.success(service.grantAdministrator(user.userId(), userId), trace(request));
    }

    @DeleteMapping("/{userId}/administrator")
    public ApiResponse<AdminUserVO> revokeAdministrator(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest request) {
        return ApiResponse.success(service.revokeAdministrator(user.userId(), userId), trace(request));
    }

    @PutMapping("/{userId}/teacher-approval")
    public ApiResponse<AdminUserVO> approveTeacherRegistration(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest request) {
        return ApiResponse.success(service.approveTeacherRegistration(user.userId(), userId), trace(request));
    }

    @DeleteMapping("/{userId}/teacher-approval")
    public ApiResponse<AdminUserVO> rejectTeacherRegistration(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest request) {
        return ApiResponse.success(service.rejectTeacherRegistration(user.userId(), userId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
