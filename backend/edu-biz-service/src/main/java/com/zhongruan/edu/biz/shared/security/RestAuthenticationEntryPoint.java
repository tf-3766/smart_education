package com.zhongruan.edu.biz.shared.security;

import com.zhongruan.edu.common.error.CommonErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorResponseWriter writer;

    public RestAuthenticationEntryPoint(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        writer.write(request, response, CommonErrorCode.UNAUTHORIZED);
    }
}
