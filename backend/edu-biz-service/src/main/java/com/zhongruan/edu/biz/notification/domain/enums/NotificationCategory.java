package com.zhongruan.edu.biz.notification.domain.enums;

public enum NotificationCategory {
    COURSE("课程消息"),
    ASSIGNMENT("作业消息"),
    EXAM("考试消息"),
    WARNING("预警消息"),
    SYSTEM("系统消息");

    private final String label;

    NotificationCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
