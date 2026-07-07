package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.RejectCourseRequest;
import com.zhongruan.edu.biz.course.api.dto.request.ReviewCourseRequest;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewListItemVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewVO;
import com.zhongruan.edu.biz.course.application.service.CourseReviewService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
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
@RequestMapping("/api/v1/admin/course-reviews")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).ADMIN.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_ACCESS.code())")
public class AdminCourseReviewController {
    private final CourseReviewService service;
    private final RequestContextFactory contextFactory;

    public AdminCourseReviewController(CourseReviewService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<CourseReviewListItemVO>> list(
            @Valid CourseListQuery query, HttpServletRequest request) {
        return ApiResponse.success(service.list(query), trace(request));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseReviewDetailVO> detail(@PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.detail(courseId), trace(request));
    }

    @PostMapping("/{courseId}/approve")
    public ApiResponse<CourseReviewVO> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) ReviewCourseRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.approve(user.userId(), courseId, body), trace(request));
    }

    @PostMapping("/{courseId}/reject")
    public ApiResponse<CourseReviewVO> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody RejectCourseRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.reject(user.userId(), courseId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
