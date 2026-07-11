package com.zhongruan.edu.biz.exam.api.controller;

import com.zhongruan.edu.biz.exam.api.dto.query.ExamListQuery;
import com.zhongruan.edu.biz.exam.api.dto.request.SubmitExamAttemptRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamAttemptVO;
import com.zhongruan.edu.biz.exam.api.vo.StudentExamListItemVO;
import com.zhongruan.edu.biz.exam.application.service.ExamParticipationService;
import com.zhongruan.edu.biz.exam.application.service.ExamManagementService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentExamController {
    private final ExamManagementService service;
    private final ExamParticipationService participationService;
    private final RequestContextFactory contextFactory;

    public StudentExamController(
            ExamManagementService service,
            ExamParticipationService participationService,
            RequestContextFactory contextFactory) {
        this.service = service;
        this.participationService = participationService;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/exams")
    public ApiResponse<PageResponse<StudentExamListItemVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid ExamListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listStudentExams(user.userId(), courseId, query), contextFactory.current(request).traceId());
    }

    @PostMapping("/exams/{examId}/attempts")
    public ApiResponse<ExamAttemptVO> start(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long examId,
            HttpServletRequest request) {
        return ApiResponse.success(
                participationService.start(user.userId(), examId), contextFactory.current(request).traceId());
    }

    @GetMapping("/exam-attempts/{attemptId}")
    public ApiResponse<ExamAttemptVO> getAttempt(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long attemptId,
            HttpServletRequest request) {
        return ApiResponse.success(
                participationService.get(user.userId(), attemptId), contextFactory.current(request).traceId());
    }

    @PostMapping("/exam-attempts/{attemptId}/submit")
    public ApiResponse<ExamAttemptVO> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitExamAttemptRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(
                participationService.submit(user.userId(), attemptId, body), contextFactory.current(request).traceId());
    }
}
