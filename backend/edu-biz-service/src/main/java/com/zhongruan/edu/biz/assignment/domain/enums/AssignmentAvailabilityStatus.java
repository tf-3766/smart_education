package com.zhongruan.edu.biz.assignment.domain.enums;

public enum AssignmentAvailabilityStatus {
    NOT_OPEN("未开放"),
    OPEN("进行中"),
    OVERDUE("已截止"),
    CLOSED("已关闭");

    private final String label;

    AssignmentAvailabilityStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
