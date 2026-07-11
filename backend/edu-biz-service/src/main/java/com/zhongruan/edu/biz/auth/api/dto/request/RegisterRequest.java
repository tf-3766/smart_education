package com.zhongruan.edu.biz.auth.api.dto.request;

import com.zhongruan.edu.biz.auth.domain.enums.RegistrationRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度必须为 3 到 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "用户名只能包含字母、数字、点、下划线或连字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度必须为 8 到 128 个字符")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 128, message = "显示名称长度不能超过 128 个字符")
        String displayName,
        @NotNull(message = "注册角色不能为空") RegistrationRole role) {}
