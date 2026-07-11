package com.zhongruan.edu.biz.auth.domain;

import com.zhongruan.edu.common.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    USERNAME_ALREADY_EXISTS("用户名已被使用", 409),
    USER_NOT_FOUND("用户不存在或已不可用", 404),
    TEACHER_REGISTRATION_NOT_PENDING("教师注册申请当前不可审核", 409),
    SUPER_ADMIN_PROTECTED("不能撤销超级管理员的管理员身份", 409);

    private final String message;
    private final int httpStatus;

    AuthErrorCode(String message, int httpStatus) {
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
