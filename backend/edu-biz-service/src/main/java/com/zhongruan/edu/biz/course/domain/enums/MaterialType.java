package com.zhongruan.edu.biz.course.domain.enums;

public enum MaterialType implements LabeledEnum {
    COURSEWARE("课件"),
    DOCUMENT("文档"),
    VIDEO("视频"),
    CODE("代码"),
    LINK("链接"),
    OTHER("其他");

    private final String label;

    MaterialType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
