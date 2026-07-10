package com.zhongruan.edu.biz.assignment.api.controller;

import com.zhongruan.edu.biz.assignment.api.dto.query.AssignmentListQuery;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentCreateRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentUpdateRequest;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.application.service.AssignmentApplicationService;
import com.zhongruan.edu.biz.grade.api.vo.TeacherSubmissionGradeVO;
import com.zhongruan.edu.biz.grade.application.service.GradeApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherAssignmentController {
    private final AssignmentApplicationService service;
    private final GradeApplicationService gradeService;
    private final RequestContextFactory contextFactory;

    public TeacherAssignmentController(
            AssignmentApplicationService service,
            GradeApplicationService gradeService,
            RequestContextFactory contextFactory) {
        this.service = service;
        this.gradeService = gradeService;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/assignments")
    public ApiResponse<PageResponse<AssignmentDetailVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid AssignmentListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForTeacher(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentDetailVO>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentCreateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(user.userId(), courseId, body), trace(request)));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ApiResponse<AssignmentDetailVO> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.update(user.userId(), assignmentId, body), trace(request));
    }

    @PostMapping("/assignments/{assignmentId}/publish")
    public ApiResponse<AssignmentDetailVO> publish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        return ApiResponse.success(service.publish(user.userId(), assignmentId), trace(request));
    }

    @PostMapping("/assignments/{assignmentId}/close")
    public ApiResponse<AssignmentDetailVO> close(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        return ApiResponse.success(service.close(user.userId(), assignmentId), trace(request));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ApiResponse<PageResponse<TeacherSubmissionGradeVO>> submissions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            @Valid AssignmentListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(gradeService.listSubmissions(user.userId(), assignmentId, query), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
