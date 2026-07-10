package com.zhongruan.edu.biz.warning.domain.enums;

public enum WarningStatus {
    OPEN("待处理"),
    HANDLED("已处理"),
    IGNORED("已忽略");

    private final String label;

    WarningStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
