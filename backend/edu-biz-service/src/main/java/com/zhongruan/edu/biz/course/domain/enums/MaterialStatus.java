package com.zhongruan.edu.biz.course.domain.enums;

public enum MaterialStatus implements LabeledEnum {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    OFFLINE("已下线");

    private final String label;

    MaterialStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
