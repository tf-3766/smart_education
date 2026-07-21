package com.zhongruan.edu.biz.exam.domain.enums;

public enum QuestionBankStatus {
    /** AI 自动流生成、尚待教师确认的草稿题库；确认后转 ACTIVE。 */
    DRAFT,
    ACTIVE,
    ARCHIVED
}
