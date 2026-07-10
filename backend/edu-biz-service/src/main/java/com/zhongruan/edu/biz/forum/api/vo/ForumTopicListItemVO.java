package com.zhongruan.edu.biz.forum.api.vo;

import java.time.OffsetDateTime;

public record ForumTopicListItemVO(
        String topicId,
        String courseId,
        String title,
        String authorId,
        String authorName,
        CodeLabelVO status,
        boolean pinned,
        int replyCount,
        OffsetDateTime lastRepliedAt,
        OffsetDateTime createdAt,
        Integer version) {}
