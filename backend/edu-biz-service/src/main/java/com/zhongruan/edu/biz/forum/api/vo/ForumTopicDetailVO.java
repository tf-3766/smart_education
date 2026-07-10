package com.zhongruan.edu.biz.forum.api.vo;

import java.time.OffsetDateTime;

public record ForumTopicDetailVO(
        String topicId,
        String courseId,
        String title,
        String content,
        String authorId,
        String authorName,
        CodeLabelVO status,
        OffsetDateTime createdAt,
        Integer version) {}
