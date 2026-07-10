package com.zhongruan.edu.biz.grade.domain;

import com.zhongruan.edu.common.error.ErrorCode;

public enum GradeErrorCode implements ErrorCode {
    SUBMISSION_NOT_FOUND("提交不存在或不可访问", 404),
    GRADE_NOT_FOUND("成绩不存在或不可访问", 404),
    GRADE_FORBIDDEN("当前用户无权批改或查看", 403),
    SUBMISSION_STATE_CONFLICT("提交状态不允许批改", 409),
    GRADE_STATE_CONFLICT("成绩状态不允许发布", 409),
    GRADE_SCORE_OUT_OF_RANGE("分数超出允许范围", 400),
    AI_DRAFT_NOT_ACCEPTABLE("AI 评语草稿不可采用", 409);

    private final String message;
    private final int httpStatus;

    GradeErrorCode(String message, int httpStatus) {
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
