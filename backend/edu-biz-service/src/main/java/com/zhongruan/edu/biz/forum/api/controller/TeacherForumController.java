package com.zhongruan.edu.biz.forum.api.controller;

import com.zhongruan.edu.biz.forum.api.dto.query.ForumListQuery;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumVisibilityRequest;
import com.zhongruan.edu.biz.forum.api.vo.ForumReplyVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicDetailVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicListItemVO;
import com.zhongruan.edu.biz.forum.application.service.ForumApplicationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherForumController {
    private final ForumApplicationService service;
    private final RequestContextFactory contextFactory;

    public TeacherForumController(ForumApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/forum/topics")
    public ApiResponse<PageResponse<ForumTopicListItemVO>> topics(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid ForumListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listTeacherTopics(user.userId(), courseId, query), trace(request));
    }

    @PatchMapping("/forum/topics/{topicId}/visibility")
    public ApiResponse<ForumTopicDetailVO> topicVisibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long topicId,
            @Valid @RequestBody ForumVisibilityRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.setTopicVisibilityByTeacher(user.userId(), topicId, body), trace(request));
    }

    @PatchMapping("/forum/replies/{replyId}/visibility")
    public ApiResponse<ForumReplyVO> replyVisibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replyId,
            @Valid @RequestBody ForumVisibilityRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.setReplyVisibilityByTeacher(user.userId(), replyId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
