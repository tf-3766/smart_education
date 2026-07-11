package com.zhongruan.edu.biz.platform.api.controller;

import com.zhongruan.edu.biz.platform.api.dto.query.AnnouncementListQuery;
import com.zhongruan.edu.biz.platform.api.vo.AnnouncementVO;
import com.zhongruan.edu.biz.platform.application.service.AnnouncementApplicationService;
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
@RequestMapping("/api/v1/student/announcements")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentAnnouncementController {
    private final AnnouncementApplicationService service;
    private final RequestContextFactory contextFactory;

    public StudentAnnouncementController(
            AnnouncementApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<AnnouncementVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid AnnouncementListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.listForStudent(user.userId(), query), contextFactory.current(request).traceId());
    }
}
