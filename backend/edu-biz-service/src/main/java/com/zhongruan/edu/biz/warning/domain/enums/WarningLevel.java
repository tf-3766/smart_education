package com.zhongruan.edu.biz.warning.domain.enums;

public enum WarningLevel {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String label;

    WarningLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
