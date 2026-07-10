package com.zhongruan.edu.biz.storage.domain;

import com.zhongruan.edu.common.error.ErrorCode;

public enum StorageErrorCode implements ErrorCode {
    FILE_NOT_FOUND("文件不存在或已不可访问", 404),
    FILE_ACCESS_DENIED("你没有访问该文件的权限", 403),
    FILE_TYPE_NOT_ALLOWED("不支持该文件类型", 400),
    FILE_TOO_LARGE("文件超过允许的大小", 400),
    FILE_IN_USE("文件正在被业务数据使用，不能删除", 409);

    private final String message;
    private final int httpStatus;

    StorageErrorCode(String message, int httpStatus) {
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
