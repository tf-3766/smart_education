package com.zhongruan.edu.biz.forum.domain;

import com.zhongruan.edu.common.error.ErrorCode;

public enum ForumErrorCode implements ErrorCode {
    FORUM_TOPIC_NOT_FOUND("主题不存在或不可访问", 404),
    FORUM_REPLY_NOT_FOUND("回复不存在或不可访问", 404),
    FORUM_FORBIDDEN("非课程成员或无治理权限", 403),
    FORUM_TOPIC_HIDDEN("主题已隐藏，不允许回复", 409),
    FORUM_PARENT_REPLY_INVALID("父回复不属于当前主题", 400),
    FORUM_STATE_CONFLICT("论坛内容状态冲突", 409);

    private final String message;
    private final int httpStatus;

    ForumErrorCode(String message, int httpStatus) {
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
