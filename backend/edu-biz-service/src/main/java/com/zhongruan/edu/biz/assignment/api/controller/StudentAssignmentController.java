package com.zhongruan.edu.biz.assignment.api.controller;

import com.zhongruan.edu.biz.assignment.api.dto.query.AssignmentListQuery;
import com.zhongruan.edu.biz.assignment.api.dto.request.SubmissionSaveRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.SubmissionSubmitRequest;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.api.vo.StudentAssignmentListItemVO;
import com.zhongruan.edu.biz.assignment.api.vo.StudentAssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.api.vo.SubmissionDetailVO;
import com.zhongruan.edu.biz.assignment.application.service.AssignmentApplicationService;
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
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentAssignmentController {
    private final AssignmentApplicationService service;
    private final RequestContextFactory contextFactory;

    public StudentAssignmentController(AssignmentApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/assignments")
    public ApiResponse<PageResponse<StudentAssignmentListItemVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid AssignmentListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForStudent(user.userId(), courseId, query), trace(request));
    }

    @GetMapping("/assignments/{assignmentId}")
    public ApiResponse<StudentAssignmentDetailVO> detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        return ApiResponse.success(service.getForStudent(user.userId(), assignmentId), trace(request));
    }

    @PutMapping("/assignments/{assignmentId}/submission-draft")
    public ApiResponse<SubmissionDetailVO> saveDraft(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionSaveRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.saveDraft(user.userId(), assignmentId, body), trace(request));
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionDetailVO>> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionSubmitRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.submit(user.userId(), assignmentId, body), trace(request)));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
