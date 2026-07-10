package com.zhongruan.edu.biz.assignment.domain.enums;

public enum AssignmentStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    CLOSED("已关闭");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canTransitionTo(AssignmentStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
