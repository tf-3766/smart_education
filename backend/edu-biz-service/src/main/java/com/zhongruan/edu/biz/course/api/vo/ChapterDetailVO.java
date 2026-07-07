package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record ChapterDetailVO(
        String chapterId,
        String courseId,
        String title,
        String description,
        Integer sortOrder,
        CodeLabelVO status,
        OffsetDateTime publishedAt,
        Integer version) {}
