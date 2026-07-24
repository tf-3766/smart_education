package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.AddCourseTeacherRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateCourseRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateCourseRequest;
import com.zhongruan.edu.biz.course.api.vo.CollabInvitationVO;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseTemplateVO;
import com.zhongruan.edu.biz.course.api.vo.CourseTeacherVO;
import com.zhongruan.edu.biz.course.api.vo.TeacherCourseListItemVO;
import com.zhongruan.edu.biz.course.api.vo.TeacherOptionVO;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.platform.api.vo.TermEnrollmentWindowVO;
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
@RequestMapping("/api/v1/teacher/courses")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherCourseController {
    private final CourseManagementService service;
    private final TermEnrollmentWindowService termWindowService;
    private final RequestContextFactory contextFactory;

    public TeacherCourseController(
            CourseManagementService service,
            TermEnrollmentWindowService termWindowService,
            RequestContextFactory contextFactory) {
        this.service = service;
        this.termWindowService = termWindowService;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/templates")
    public ApiResponse<List<CourseTemplateVO>> templates(HttpServletRequest request) {
        return ApiResponse.success(service.listTemplates(), trace(request));
    }

    @GetMapping("/term-windows")
    public ApiResponse<List<TermEnrollmentWindowVO>> termWindows(HttpServletRequest request) {
        return ApiResponse.success(termWindowService.list(), trace(request));
    }

    @GetMapping("/teacher-directory")
    public ApiResponse<List<TeacherOptionVO>> teacherDirectory(HttpServletRequest request) {
        return ApiResponse.success(service.listTeacherDirectory(), trace(request));
    }

    @GetMapping("/collab-invitations")
    public ApiResponse<List<CollabInvitationVO>> invitations(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success(service.listInvitations(user.userId()), trace(request));
    }

    @PostMapping("/collab-invitations/{courseId}/accept")
    public ApiResponse<Void> acceptInvitation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        service.acceptInvitation(user.userId(), courseId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/collab-invitations/{courseId}/reject")
    public ApiResponse<Void> rejectInvitation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        service.rejectInvitation(user.userId(), courseId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDetailVO>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateCourseRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(user.userId(), request), trace(servletRequest)));
    }

    @GetMapping
    public ApiResponse<PageResponse<TeacherCourseListItemVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid CourseListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForTeacher(user.userId(), query), trace(request));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.getForTeacher(user.userId(), courseId), trace(request));
    }

    @PutMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.update(user.userId(), courseId, body), trace(request));
    }

    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> deleteDraft(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        service.deleteDraft(user.userId(), courseId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/{courseId}/submit-review")
    public ApiResponse<CourseDetailVO> submitReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.submitReview(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/publish")
    public ApiResponse<CourseDetailVO> publish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.publish(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/start")
    public ApiResponse<CourseDetailVO> start(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.start(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/finish")
    public ApiResponse<CourseDetailVO> finish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.finish(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/offline")
    public ApiResponse<CourseDetailVO> offline(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.offline(user.userId(), courseId), trace(request));
    }

    @GetMapping("/{courseId}/teachers")
    public ApiResponse<List<CourseTeacherVO>> teachers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        return ApiResponse.success(service.listTeachers(user.userId(), courseId), trace(request));
    }

    @PostMapping("/{courseId}/teachers")
    public ApiResponse<CourseTeacherVO> addTeacher(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody AddCourseTeacherRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.addTeacher(user.userId(), courseId, body), trace(request));
    }

    @DeleteMapping("/{courseId}/teachers/{teacherId}")
    public ApiResponse<Void> removeTeacher(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @PathVariable Long teacherId,
            HttpServletRequest request) {
        service.removeTeacher(user.userId(), courseId, teacherId);
        return ApiResponse.success(trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
