package com.zhongruan.edu.biz.warning.api.controller;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.biz.warning.api.dto.query.WarningListQuery;
import com.zhongruan.edu.biz.warning.api.dto.request.GenerateCourseWarningsRequest;
import com.zhongruan.edu.biz.warning.api.dto.request.WarningHandleRequest;
import com.zhongruan.edu.biz.warning.api.vo.LearningWarningVO;
import com.zhongruan.edu.biz.warning.api.vo.WarningGenerationResultVO;
import com.zhongruan.edu.biz.warning.application.service.WarningApplicationService;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherWarningController {
    private final WarningApplicationService service;
    private final RequestContextFactory contextFactory;

    public TeacherWarningController(WarningApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @PostMapping("/courses/{courseId}/warnings/generation")
    public ApiResponse<WarningGenerationResultVO> generate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @RequestBody GenerateCourseWarningsRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.generate(user.userId(), courseId, body), trace(request));
    }

    @GetMapping("/courses/{courseId}/warnings")
    public ApiResponse<PageResponse<LearningWarningVO>> warnings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid WarningListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForTeacher(user.userId(), courseId, query), trace(request));
    }

    @GetMapping("/warnings/{warningId}")
    public ApiResponse<LearningWarningVO> warning(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long warningId,
            HttpServletRequest request) {
        return ApiResponse.success(service.teacherDetail(user.userId(), warningId), trace(request));
    }

    @PostMapping("/warnings/{warningId}/handle")
    public ApiResponse<LearningWarningVO> handle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long warningId,
            @Valid @RequestBody WarningHandleRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.handle(user.userId(), warningId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
