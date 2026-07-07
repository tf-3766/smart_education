package com.zhongruan.edu.biz.course.domain.enums;

public enum LessonStatus implements LabeledEnum {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    OFFLINE("已下线");

    private final String label;

    LessonStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean canTransitionTo(LessonStatus target) {
        return (this == DRAFT && target == PUBLISHED) || (this == PUBLISHED && target == OFFLINE);
    }
}
