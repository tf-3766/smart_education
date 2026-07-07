package com.zhongruan.edu.ai.shared;

import com.zhongruan.edu.common.api.ApiError;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.error.ErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class AiGlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AiGlobalExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(
            WebExchangeBindException exception, ServerWebExchange exchange) {
        List<ApiError> errors = exception.getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return response(
                CommonErrorCode.PARAM_VALIDATION_ERROR,
                CommonErrorCode.PARAM_VALIDATION_ERROR.message(),
                errors,
                exchange);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception, ServerWebExchange exchange) {
        log.info("AI request rejected errorCode={} message={}", exception.errorCode().code(), exception.getMessage());
        return response(exception.errorCode(), exception.getMessage(), List.of(), exchange);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnknown(Exception exception, ServerWebExchange exchange) {
        log.error("Unhandled AI request failure", exception);
        return response(
                CommonErrorCode.INTERNAL_ERROR,
                CommonErrorCode.INTERNAL_ERROR.message(),
                List.of(),
                exchange);
    }

    private ApiError toApiError(FieldError error) {
        return ApiError.of(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> response(
            ErrorCode errorCode, String message, List<ApiError> errors, ServerWebExchange exchange) {
        String traceId = exchange.getAttributeOrDefault(
                TraceIdWebFilter.ATTRIBUTE,
                com.zhongruan.edu.common.context.TraceIds.normalizeOrCreate(null));
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode, message, errors, traceId));
    }
}
