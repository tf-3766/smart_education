package com.zhongruan.edu.biz.warning.domain.enums;

public enum WarningType {
    PROGRESS_LAG("学习进度落后"),
    MISSING_ASSIGNMENT("作业缺交"),
    LOW_SCORE("成绩偏低");

    private final String label;

    WarningType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
