package com.zhongruan.edu.biz.warning.api.vo;

import com.zhongruan.edu.biz.warning.domain.enums.WarningLevel;
import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import com.zhongruan.edu.biz.warning.domain.enums.WarningType;

public record CodeLabelVO(String code, String label) {
    public static CodeLabelVO of(WarningType type) {
        return new CodeLabelVO(type.name(), type.label());
    }

    public static CodeLabelVO of(WarningLevel level) {
        return new CodeLabelVO(level.name(), level.label());
    }

    public static CodeLabelVO of(WarningStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }
}
