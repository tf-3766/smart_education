package com.zhongruan.edu.biz.notification.api.vo;

import java.time.OffsetDateTime;

public record NotificationStreamEvent(String type, String notificationId, OffsetDateTime timestamp) {}
