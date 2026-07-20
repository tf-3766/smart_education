package com.zhongruan.edu.biz.forum.api.vo;

import java.time.OffsetDateTime;

public record ForumReplyVO(
        String replyId,
        String topicId,
        String courseId,
        String authorId,
        String authorName,
        String authorAvatarFileId,
        String parentReplyId,
        String content,
        CodeLabelVO status,
        String moderationReason,
        String moderatedBy,
        OffsetDateTime moderatedAt,
        OffsetDateTime createdAt,
        Integer version) {}
