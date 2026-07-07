package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.api.vo.EnrollmentVO;
import com.zhongruan.edu.biz.course.api.vo.StudentCourseListItemVO;
import com.zhongruan.edu.biz.course.application.service.StudentCourseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/courses")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentCourseController {
    private final StudentCourseService service;
    private final RequestContextFactory contextFactory;

    public StudentCourseController(StudentCourseService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/catalog")
    public ApiResponse<PageResponse<StudentCourseListItemVO>> catalog(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid CourseListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.catalog(user.userId(), query), trace(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentCourseListItemVO>> myCourses(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid CourseListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.myCourses(user.userId(), query), trace(request));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.detail(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/enroll")
    public ApiResponse<EnrollmentVO> enroll(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.enroll(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/withdraw")
    public ApiResponse<EnrollmentVO> withdraw(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.withdraw(user.userId(), courseId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
