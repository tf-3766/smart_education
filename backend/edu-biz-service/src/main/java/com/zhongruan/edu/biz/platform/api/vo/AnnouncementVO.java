package com.zhongruan.edu.biz.platform.api.vo;

import java.time.OffsetDateTime;

public record AnnouncementVO(
        String announcementId,
        String scopeType,
        String courseId,
        String title,
        String content,
        String audience,
        String status,
        OffsetDateTime publishedAt,
        OffsetDateTime withdrawnAt,
        String publisherId,
        String source,
        Integer version) {}
