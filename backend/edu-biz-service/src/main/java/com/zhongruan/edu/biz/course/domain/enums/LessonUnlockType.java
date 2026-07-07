package com.zhongruan.edu.biz.course.domain.enums;

public enum LessonUnlockType implements LabeledEnum {
    IMMEDIATE("立即解锁"),
    SCHEDULED("定时解锁");

    private final String label;

    LessonUnlockType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
