package com.zhongruan.edu.biz.shared.exception;

import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.biz.storage.domain.StorageErrorCode;
import com.zhongruan.edu.common.api.ApiError;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.error.ErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return response(CommonErrorCode.PARAM_VALIDATION_ERROR, CommonErrorCode.PARAM_VALIDATION_ERROR.message(), errors, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        log.debug("Request validation failed: {}", exception.getMessage());
        return response(CommonErrorCode.PARAM_VALIDATION_ERROR, CommonErrorCode.PARAM_VALIDATION_ERROR.message(), List.of(), request);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        log.info("Business request rejected errorCode={} message={}", exception.errorCode().code(), exception.getMessage());
        return response(exception.errorCode(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return response(StorageErrorCode.FILE_TOO_LARGE, StorageErrorCode.FILE_TOO_LARGE.message(), List.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        log.info("Authentication rejected: {}", exception.getClass().getSimpleName());
        return response(CommonErrorCode.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED.message(), List.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.info("Authorization rejected: {}", exception.getMessage());
        return response(CommonErrorCode.FORBIDDEN, CommonErrorCode.FORBIDDEN.message(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnknown(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return response(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.message(), List.of(), request);
    }

    private ApiError toApiError(FieldError fieldError) {
        Object rejectedValue = isSensitive(fieldError.getField()) ? null : safeValue(fieldError.getRejectedValue());
        return new ApiError(fieldError.getField(), fieldError.getDefaultMessage(), rejectedValue);
    }

    private boolean isSensitive(String field) {
        String value = field.toLowerCase();
        return value.contains("password") || value.contains("token") || value.contains("secret");
    }

    private Object safeValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() <= 128 ? text : text.substring(0, 128);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            ErrorCode errorCode, String message, List<ApiError> errors, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode, message, errors, RequestTrace.from(request)));
    }
}
