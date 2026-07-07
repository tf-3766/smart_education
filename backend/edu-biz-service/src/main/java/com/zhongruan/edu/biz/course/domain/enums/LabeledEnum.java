package com.zhongruan.edu.biz.course.domain.enums;

public interface LabeledEnum {
    String label();

    default String code() {
        return ((Enum<?>) this).name();
    }
}
