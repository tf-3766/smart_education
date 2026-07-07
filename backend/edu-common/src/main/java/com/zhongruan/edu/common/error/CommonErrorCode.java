package com.zhongruan.edu.common.error;

public enum CommonErrorCode implements ErrorCode {
    PARAM_VALIDATION_ERROR("请求参数不正确", 400),
    UNAUTHORIZED("请先登录或检查 Token", 401),
    TOKEN_EXPIRED("Token 已过期", 401),
    FORBIDDEN("你没有查看或操作该资源的权限", 403),
    INVALID_CREDENTIALS("用户名或密码错误", 401),
    RESOURCE_NOT_FOUND("资源不存在或已不可访问", 404),
    RESOURCE_CONFLICT("数据已发生变化，请刷新后重试", 409),
    OPERATION_NOT_ALLOWED("当前状态不允许执行该操作", 409),
    FILE_UPLOAD_FAILED("文件上传失败", 500),
    AI_SERVICE_UNAVAILABLE("AI 服务暂时不可用", 503),
    AI_NO_RELIABLE_CONTEXT("未找到可靠课程资料", 422),
    SSE_STREAM_INTERRUPTED("AI 流式响应已中断", 503),
    INTERNAL_ERROR("系统内部错误，请稍后重试", 500);

    private final String message;
    private final int httpStatus;

    CommonErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
