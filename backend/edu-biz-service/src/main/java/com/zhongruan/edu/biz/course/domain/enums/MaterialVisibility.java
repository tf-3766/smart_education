package com.zhongruan.edu.biz.course.domain.enums;

public enum MaterialVisibility implements LabeledEnum {
    COURSE("课程"),
    CHAPTER("章节"),
    LESSON("课时");

    private final String label;

    MaterialVisibility(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
