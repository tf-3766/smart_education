package com.zhongruan.edu.biz.auth.api.vo;

public record LogoutVO(String mode, boolean serverSideRevoked) {
    public static LogoutVO clientDiscardOnly() {
        return new LogoutVO("CLIENT_DISCARD_TOKEN", false);
    }
}
