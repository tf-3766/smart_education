package com.zhongruan.edu.biz.forum.api.controller;

import com.zhongruan.edu.biz.forum.api.dto.request.ForumVisibilityRequest;
import com.zhongruan.edu.biz.forum.api.vo.ForumReplyVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicDetailVO;
import com.zhongruan.edu.biz.forum.application.service.ForumApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).ADMIN.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_ACCESS.code())")
public class AdminForumController {
    private final ForumApplicationService service;
    private final RequestContextFactory contextFactory;

    public AdminForumController(ForumApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @PatchMapping("/forum/topics/{topicId}/visibility")
    public ApiResponse<ForumTopicDetailVO> topicVisibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long topicId,
            @Valid @RequestBody ForumVisibilityRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.setTopicVisibilityByAdmin(user.userId(), topicId, body), trace(request));
    }

    @PatchMapping("/forum/replies/{replyId}/visibility")
    public ApiResponse<ForumReplyVO> replyVisibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replyId,
            @Valid @RequestBody ForumVisibilityRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.setReplyVisibilityByAdmin(user.userId(), replyId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
