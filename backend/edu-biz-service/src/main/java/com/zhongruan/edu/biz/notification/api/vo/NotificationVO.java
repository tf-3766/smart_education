package com.zhongruan.edu.biz.notification.api.vo;

import java.time.OffsetDateTime;

public record NotificationVO(
        String notificationId,
        String title,
        String content,
        String category,
        String categoryLabel,
        String status,
        String sourceType,
        String announcementId,
        String courseId,
        String assignmentId,
        String examId,
        String warningId,
        OffsetDateTime createdAt,
        boolean read,
        OffsetDateTime readAt) {}
