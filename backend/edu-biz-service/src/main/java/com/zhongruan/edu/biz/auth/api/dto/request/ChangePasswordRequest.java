package com.zhongruan.edu.biz.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        @Size(max = 128, message = "当前密码长度不能超过 128 个字符")
        String currentPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 128, message = "新密码长度必须为 8 到 128 个字符")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码必须同时包含字母和数字")
        String newPassword) {}