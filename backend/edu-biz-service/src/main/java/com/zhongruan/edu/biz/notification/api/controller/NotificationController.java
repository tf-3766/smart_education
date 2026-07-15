package com.zhongruan.edu.biz.notification.api.controller;

import com.zhongruan.edu.biz.notification.api.dto.query.NotificationListQuery;
import com.zhongruan.edu.biz.notification.api.dto.request.UpdateNotificationPreferencesRequest;
import com.zhongruan.edu.biz.notification.api.vo.NotificationPreferencesVO;
import com.zhongruan.edu.biz.notification.api.vo.NotificationVO;
import com.zhongruan.edu.biz.notification.application.service.NotificationApplicationService;
import com.zhongruan.edu.biz.notification.application.service.NotificationStreamService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class NotificationController {
    private final NotificationApplicationService service;
    private final NotificationStreamService streamService;
    private final RequestContextFactory contextFactory;

    public NotificationController(
            NotificationApplicationService service,
            NotificationStreamService streamService,
            RequestContextFactory contextFactory) {
        this.service = service;
        this.streamService = streamService;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationVO>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid NotificationListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.list(user.userId(), query), trace(request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success(service.unreadCount(user.userId()), trace(request));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationId,
            HttpServletRequest request) {
        service.markRead(user.userId(), notificationId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        service.markAllRead(user.userId());
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/{notificationId}/archive")
    public ApiResponse<Void> archive(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationId,
            HttpServletRequest request) {
        service.archive(user.userId(), notificationId);
        return ApiResponse.success(trace(request));
    }

    @GetMapping("/preferences")
    public ApiResponse<NotificationPreferencesVO> preferences(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success(service.preferences(user.userId()), trace(request));
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferencesVO> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateNotificationPreferencesRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updatePreferences(user.userId(), body), trace(request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) {
        return streamService.subscribe(user.userId());
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
