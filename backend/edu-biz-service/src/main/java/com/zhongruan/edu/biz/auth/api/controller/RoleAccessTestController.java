package com.zhongruan.edu.biz.auth.api.controller;

import com.zhongruan.edu.biz.auth.api.vo.AccessCheckVO;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class RoleAccessTestController {
    private final RequestContextFactory requestContextFactory;

    public RoleAccessTestController(RequestContextFactory requestContextFactory) {
        this.requestContextFactory = requestContextFactory;
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).STUDENT.name())"
            + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).STUDENT_ACCESS.code())")
    public ApiResponse<AccessCheckVO> student(HttpServletRequest request) {
        return allowed("STUDENT", request);
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
            + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
    public ApiResponse<AccessCheckVO> teacher(HttpServletRequest request) {
        return allowed("TEACHER", request);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).ADMIN.name())"
            + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).ADMIN_ACCESS.code())")
    public ApiResponse<AccessCheckVO> admin(HttpServletRequest request) {
        return allowed("ADMIN", request);
    }

    private ApiResponse<AccessCheckVO> allowed(String scope, HttpServletRequest request) {
        return ApiResponse.success(
                new AccessCheckVO(scope, "ALLOWED"), requestContextFactory.current(request).traceId());
    }
}
