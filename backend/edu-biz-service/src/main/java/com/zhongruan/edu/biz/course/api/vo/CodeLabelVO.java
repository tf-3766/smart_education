package com.zhongruan.edu.biz.course.api.vo;

import com.zhongruan.edu.biz.course.domain.enums.LabeledEnum;

public record CodeLabelVO(String code, String label) {
    public static CodeLabelVO of(LabeledEnum value) {
        return new CodeLabelVO(value.code(), value.label());
    }
}
