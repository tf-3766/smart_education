package com.zhongruan.edu.biz.course.domain.enums;

public enum CourseReviewStatus implements LabeledEnum {
    NOT_SUBMITTED("未提交"),
    PENDING("审核中"),
    APPROVED("已通过"),
    REJECTED("已驳回");

    private final String label;

    CourseReviewStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean canTransitionTo(CourseReviewStatus target) {
        return switch (this) {
            case NOT_SUBMITTED -> target == PENDING;
            case PENDING -> target == APPROVED || target == REJECTED;
            case REJECTED -> target == PENDING;
            case APPROVED -> false;
        };
    }
}
