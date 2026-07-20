package com.zhongruan.edu.biz.course.domain.enums;

public enum CourseTeacherStatus implements LabeledEnum {
    PENDING("待确认"),
    ACTIVE("已加入");

    private final String label;

    CourseTeacherStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
