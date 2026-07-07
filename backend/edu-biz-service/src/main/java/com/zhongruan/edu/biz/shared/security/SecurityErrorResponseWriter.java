package com.zhongruan.edu.biz.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.httpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(), ApiResponse.failure(errorCode, RequestTrace.from(request)));
    }
}

