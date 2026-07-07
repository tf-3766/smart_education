package com.zhongruan.edu.biz.course.domain.enums;

public enum EnrollmentStatus implements LabeledEnum {
    ENROLLED("已选课"),
    WITHDRAWN("已退选"),
    COMPLETED("已完成");

    private final String label;

    EnrollmentStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean canTransitionTo(EnrollmentStatus target) {
        return this == ENROLLED && (target == WITHDRAWN || target == COMPLETED);
    }
}
