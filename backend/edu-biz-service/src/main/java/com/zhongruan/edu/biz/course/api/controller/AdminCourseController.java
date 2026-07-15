package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.dto.request.UpdateCourseRequest;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).ADMIN.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_ACCESS.code())")
public class AdminCourseController {
    private final CourseManagementService service;
    private final RequestContextFactory contextFactory;

    public AdminCourseController(CourseManagementService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> detail(@PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.getForAdmin(courseId), trace(request));
    }

    @PutMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> update(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateAsAdmin(courseId, body), trace(request));
    }

    @PostMapping("/{courseId}/offline")
    public ApiResponse<CourseDetailVO> offline(@PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.offlineAsAdmin(courseId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
