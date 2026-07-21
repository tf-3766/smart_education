package com.zhongruan.edu.biz.exam.api.vo;

public record QuestionBankVO(
        String bankId,
        String courseId,
        String name,
        String description,
        String status,
        String source,
        Integer version) {}
