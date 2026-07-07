package com.zhongruan.edu.biz.course.domain.enums;

public enum CourseTeacherRole implements LabeledEnum {
    OWNER("负责人"),
    COLLABORATOR("协作者");

    private final String label;

    CourseTeacherRole(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
