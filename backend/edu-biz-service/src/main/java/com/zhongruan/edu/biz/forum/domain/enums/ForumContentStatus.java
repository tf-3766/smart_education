package com.zhongruan.edu.biz.forum.domain.enums;

public enum ForumContentStatus {
    VISIBLE("可见"),
    HIDDEN("已隐藏");

    private final String label;

    ForumContentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
