package com.zhongruan.edu.biz.notification.api.dto.request;

import com.zhongruan.edu.biz.notification.domain.enums.NotificationCategory;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateNotificationPreferencesRequest(
        @NotNull Set<NotificationCategory> enabledCategories) {}
