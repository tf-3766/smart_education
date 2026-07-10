package com.zhongruan.edu.biz.forum.api.controller;

import com.zhongruan.edu.biz.forum.api.dto.query.ForumListQuery;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumReplyCreateRequest;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumTopicCreateRequest;
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
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
public class StudentForumController {
    private final ForumApplicationService service;
    private final RequestContextFactory contextFactory;

    public StudentForumController(ForumApplicationService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/forum/topics")
    public ApiResponse<PageResponse<ForumTopicListItemVO>> topics(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid ForumListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listStudentTopics(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/forum/topics")
    public ResponseEntity<ApiResponse<ForumTopicDetailVO>> createTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody ForumTopicCreateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createTopic(user.userId(), courseId, body), trace(request)));
    }

    @GetMapping("/forum/topics/{topicId}")
    public ApiResponse<ForumTopicDetailVO> topic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long topicId,
            HttpServletRequest request) {
        return ApiResponse.success(service.studentTopic(user.userId(), topicId), trace(request));
    }

    @GetMapping("/forum/topics/{topicId}/replies")
    public ApiResponse<PageResponse<ForumReplyVO>> replies(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long topicId,
            @Valid ForumListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.studentReplies(user.userId(), topicId, query), trace(request));
    }

    @PostMapping("/forum/topics/{topicId}/replies")
    public ResponseEntity<ApiResponse<ForumReplyVO>> createReply(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long topicId,
            @Valid @RequestBody ForumReplyCreateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createReply(user.userId(), topicId, body), trace(request)));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
