package com.zhongruan.edu.biz.auth.domain.enums;

public enum SystemPermission {
    AUTH_PROFILE_READ("auth:profile:read"),
    STUDENT_ACCESS("student:access"),
    TEACHER_ACCESS("teacher:access"),
    ADMIN_ACCESS("admin:access"),
    ADMIN_MANAGE("admin:manage");

    private final String code;

    SystemPermission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
