package com.zhongruan.edu.common.error;

public interface ErrorCode {
    String code();

    String message();

    int httpStatus();
}

