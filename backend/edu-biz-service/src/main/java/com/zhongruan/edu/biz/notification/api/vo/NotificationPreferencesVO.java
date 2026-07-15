package com.zhongruan.edu.biz.notification.api.vo;

import java.util.Set;

public record NotificationPreferencesVO(Set<String> enabledCategories) {
    public NotificationPreferencesVO {
        enabledCategories = enabledCategories == null ? Set.of() : Set.copyOf(enabledCategories);
    }
}
