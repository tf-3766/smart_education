package com.zhongruan.edu.biz.grade.api.controller;

import com.zhongruan.edu.biz.grade.api.dto.query.GradeListQuery;
import com.zhongruan.edu.biz.grade.api.vo.StudentGradeVO;
import com.zhongruan.edu.biz.grade.application.service.GradeApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentGradeController {
    private final GradeApplicationService service;
    private final RequestContextFactory contextFactory;

    public StudentGradeController(GradeApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/grades")
    public ApiResponse<PageResponse<StudentGradeVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid GradeListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listStudentGrades(user.userId(), query), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
