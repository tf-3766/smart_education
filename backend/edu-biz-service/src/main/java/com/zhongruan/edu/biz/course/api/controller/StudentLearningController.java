package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.vo.CourseOutlineVO;
import com.zhongruan.edu.biz.course.api.vo.CourseProgressVO;
import com.zhongruan.edu.biz.course.api.vo.LearningRecordVO;
import com.zhongruan.edu.biz.course.api.vo.MaterialAccessVO;
import com.zhongruan.edu.biz.course.api.vo.StudentLessonDetailVO;
import com.zhongruan.edu.biz.course.application.service.StudentLearningService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentLearningController {
    private final StudentLearningService service;
    private final RequestContextFactory contextFactory;

    public StudentLearningController(StudentLearningService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/outline")
    public ApiResponse<CourseOutlineVO> outline(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.outline(user.userId(), courseId), trace(request));
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<StudentLessonDetailVO> lesson(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.lesson(user.userId(), lessonId), trace(request));
    }

    @PostMapping("/lessons/{lessonId}/start")
    public ApiResponse<LearningRecordVO> start(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.start(user.userId(), lessonId), trace(request));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ApiResponse<LearningRecordVO> complete(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.complete(user.userId(), lessonId), trace(request));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ApiResponse<CourseProgressVO> progress(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.progress(user.userId(), courseId), trace(request));
    }

    @GetMapping("/materials/{materialId}")
    public ApiResponse<MaterialAccessVO> material(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long materialId, HttpServletRequest request) {
        return ApiResponse.success(service.material(user.userId(), materialId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
