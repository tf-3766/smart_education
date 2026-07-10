package com.zhongruan.edu.biz.warning.domain;

import com.zhongruan.edu.common.error.ErrorCode;

public enum WarningErrorCode implements ErrorCode {
    WARNING_NOT_FOUND("预警不存在或不可访问", 404),
    WARNING_FORBIDDEN("当前用户无权查看或处理", 403),
    WARNING_STATE_CONFLICT("当前预警状态不允许处理", 409),
    WARNING_GENERATION_CONFLICT("预警生成冲突", 409),
    WARNING_RULE_UNSUPPORTED("不支持的预警类型", 400),
    WARNING_EVIDENCE_MISSING("缺少预警依据", 409);

    private final String message;
    private final int httpStatus;

    WarningErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
