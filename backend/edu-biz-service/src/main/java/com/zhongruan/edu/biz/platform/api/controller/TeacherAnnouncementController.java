package com.zhongruan.edu.biz.platform.api.controller;

import com.zhongruan.edu.biz.platform.api.dto.query.AnnouncementListQuery;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.WithdrawAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.vo.AnnouncementVO;
import com.zhongruan.edu.biz.platform.application.service.AnnouncementApplicationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherAnnouncementController {
    private final AnnouncementApplicationService service;
    private final RequestContextFactory contextFactory;

    public TeacherAnnouncementController(
            AnnouncementApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/announcements")
    public ApiResponse<PageResponse<AnnouncementVO>> listRelevant(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid AnnouncementListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForTeacher(user.userId(), query), trace(request));
    }

    @GetMapping("/courses/{courseId}/announcements")
    public ApiResponse<PageResponse<AnnouncementVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid AnnouncementListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listCourse(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/announcements")
    public ResponseEntity<ApiResponse<AnnouncementVO>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateAnnouncementRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        service.createCourseAnnouncement(user.userId(), courseId, body), trace(request)));
    }

    @PostMapping("/announcements/{announcementId}/withdrawal")
    public ApiResponse<AnnouncementVO> withdraw(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long announcementId,
            @Valid @RequestBody WithdrawAnnouncementRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.withdrawCourseAnnouncement(user.userId(), announcementId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
