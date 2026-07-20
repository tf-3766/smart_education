package com.zhongruan.edu.biz.platform.api.controller;

import com.zhongruan.edu.biz.platform.api.dto.query.AnnouncementListQuery;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateCourseCategoryRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.UpdateCourseCategoryRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.UpsertTermEnrollmentWindowRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.WithdrawAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.vo.AdminStatisticsVO;
import com.zhongruan.edu.biz.platform.api.vo.AnnouncementVO;
import com.zhongruan.edu.biz.platform.api.vo.CourseCategoryVO;
import com.zhongruan.edu.biz.platform.api.vo.TermEnrollmentWindowVO;
import com.zhongruan.edu.biz.platform.application.service.AdminStatisticsService;
import com.zhongruan.edu.biz.platform.application.service.AnnouncementApplicationService;
import com.zhongruan.edu.biz.platform.application.service.CourseCategoryService;
import com.zhongruan.edu.biz.platform.application.service.TermEnrollmentWindowService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).ADMIN.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_ACCESS.code())")
public class AdminPlatformController {
    private final CourseCategoryService categoryService;
    private final AnnouncementApplicationService announcementService;
    private final AdminStatisticsService statisticsService;
    private final TermEnrollmentWindowService termWindowService;
    private final RequestContextFactory contextFactory;

    public AdminPlatformController(
            CourseCategoryService categoryService,
            AnnouncementApplicationService announcementService,
            AdminStatisticsService statisticsService,
            TermEnrollmentWindowService termWindowService,
            RequestContextFactory contextFactory) {
        this.categoryService = categoryService;
        this.announcementService = announcementService;
        this.statisticsService = statisticsService;
        this.termWindowService = termWindowService;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/term-enrollment-windows")
    public ApiResponse<List<TermEnrollmentWindowVO>> listTermEnrollmentWindows(HttpServletRequest request) {
        return ApiResponse.success(termWindowService.list(), trace(request));
    }

    @PutMapping("/term-enrollment-windows")
    public ApiResponse<TermEnrollmentWindowVO> upsertTermEnrollmentWindow(
            @Valid @RequestBody UpsertTermEnrollmentWindowRequest body, HttpServletRequest request) {
        return ApiResponse.success(termWindowService.upsert(body), trace(request));
    }
    @GetMapping("/course-categories")
    public ApiResponse<List<CourseCategoryVO>> listCategories(HttpServletRequest request) {
        return ApiResponse.success(categoryService.listForAdministration(), trace(request));
    }

    @PostMapping("/course-categories")
    public ResponseEntity<ApiResponse<CourseCategoryVO>> createCategory(
            @Valid @RequestBody CreateCourseCategoryRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(body), trace(request)));
    }

    @PutMapping("/course-categories/{categoryId}")
    public ApiResponse<CourseCategoryVO> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCourseCategoryRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(categoryService.update(categoryId, body), trace(request));
    }

    @DeleteMapping("/course-categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId, HttpServletRequest request) {
        categoryService.delete(categoryId);
        return ApiResponse.success(null, trace(request));
    }

    @GetMapping("/announcements")
    public ApiResponse<PageResponse<AnnouncementVO>> listAnnouncements(
            @Valid AnnouncementListQuery query, HttpServletRequest request) {
        return ApiResponse.success(announcementService.listForAdministration(query), trace(request));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<AnnouncementVO>> createAnnouncement(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateAnnouncementRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        announcementService.createSystemAnnouncement(user.userId(), body), trace(request)));
    }

    @PostMapping("/announcements/{announcementId}/withdrawal")
    public ApiResponse<AnnouncementVO> withdrawAnnouncement(
            @PathVariable Long announcementId,
            @Valid @RequestBody WithdrawAnnouncementRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(
                announcementService.withdrawSystemAnnouncement(announcementId, body), trace(request));
    }

    @GetMapping("/statistics")
    public ApiResponse<AdminStatisticsVO> statistics(HttpServletRequest request) {
        return ApiResponse.success(statisticsService.overview(), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
