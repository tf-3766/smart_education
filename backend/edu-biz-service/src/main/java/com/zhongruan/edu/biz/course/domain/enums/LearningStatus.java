package com.zhongruan.edu.biz.course.domain.enums;

public enum LearningStatus implements LabeledEnum {
    NOT_STARTED("未开始"),
    IN_PROGRESS("学习中"),
    COMPLETED("已完成");

    private final String label;

    LearningStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean canTransitionTo(LearningStatus target) {
        return switch (this) {
            case NOT_STARTED -> target == IN_PROGRESS || target == COMPLETED;
            case IN_PROGRESS -> target == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
