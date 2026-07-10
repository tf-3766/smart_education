package com.zhongruan.edu.biz.warning.api.controller;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.biz.warning.api.dto.query.WarningListQuery;
import com.zhongruan.edu.biz.warning.api.vo.LearningWarningVO;
import com.zhongruan.edu.biz.warning.application.service.WarningApplicationService;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentWarningController {
    private final WarningApplicationService service;
    private final RequestContextFactory contextFactory;

    public StudentWarningController(WarningApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/warnings")
    public ApiResponse<PageResponse<LearningWarningVO>> warnings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid WarningListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForStudent(user.userId(), query), trace(request));
    }

    @GetMapping("/warnings/{warningId}")
    public ApiResponse<LearningWarningVO> warning(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long warningId,
            HttpServletRequest request) {
        return ApiResponse.success(service.studentDetail(user.userId(), warningId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
