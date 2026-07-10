package com.zhongruan.edu.biz.exam.api.vo;

public record QuestionOptionVO(
        String optionId,
        String label,
        String content,
        boolean correct,
        Integer sortOrder) {}
