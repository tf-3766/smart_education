package com.zhongruan.edu.biz.course.domain.enums;

public enum LessonContentType implements LabeledEnum {
    RICH_TEXT("Markdown 富文本"),
    VIDEO("视频"),
    DOCUMENT("文档"),
    MIXED("混合内容");

    private final String label;

    LessonContentType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
