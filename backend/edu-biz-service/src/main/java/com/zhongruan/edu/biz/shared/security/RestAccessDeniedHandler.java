package com.zhongruan.edu.biz.shared.security;

import com.zhongruan.edu.common.error.CommonErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorResponseWriter writer;

    public RestAccessDeniedHandler(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        writer.write(request, response, CommonErrorCode.FORBIDDEN);
    }
}

