package com.zhongruan.edu.biz.grade.domain.enums;

public enum GradeStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布");

    private final String label;

    GradeStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
