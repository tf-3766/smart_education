package com.zhongruan.edu.biz.auth.api.vo;

public record RegistrationVO(
        String userId,
        String username,
        String displayName,
        String role,
        String userStatus,
        boolean approvalRequired,
        LoginVO login) {}
