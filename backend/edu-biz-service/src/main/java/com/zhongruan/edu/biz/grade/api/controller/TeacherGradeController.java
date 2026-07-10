package com.zhongruan.edu.biz.grade.api.controller;

import com.zhongruan.edu.biz.grade.api.dto.request.GradeSubmissionRequest;
import com.zhongruan.edu.biz.grade.api.dto.request.PublishGradeRequest;
import com.zhongruan.edu.biz.grade.api.vo.AssignmentStatisticsVO;
import com.zhongruan.edu.biz.grade.api.vo.CourseGradeStatisticsVO;
import com.zhongruan.edu.biz.grade.api.vo.TeacherSubmissionGradeVO;
import com.zhongruan.edu.biz.grade.application.service.GradeApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
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
public class TeacherGradeController {
    private final GradeApplicationService service;
    private final RequestContextFactory contextFactory;

    public TeacherGradeController(GradeApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ApiResponse<TeacherSubmissionGradeVO> grade(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeSubmissionRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.gradeSubmission(user.userId(), submissionId, body), trace(request));
    }

    @PostMapping("/grades/{gradeId}/publication")
    public ApiResponse<TeacherSubmissionGradeVO> publish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long gradeId,
            @Valid @RequestBody PublishGradeRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.publish(user.userId(), gradeId, body), trace(request));
    }

    @GetMapping("/assignments/{assignmentId}/statistics")
    public ApiResponse<AssignmentStatisticsVO> statistics(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        return ApiResponse.success(service.statistics(user.userId(), assignmentId), trace(request));
    }

    @GetMapping("/courses/{courseId}/grade-statistics")
    public ApiResponse<CourseGradeStatisticsVO> courseStatistics(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.courseStatistics(user.userId(), courseId), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
