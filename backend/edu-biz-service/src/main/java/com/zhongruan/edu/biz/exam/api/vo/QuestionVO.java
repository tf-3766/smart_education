package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.util.List;

public record QuestionVO(
        String questionId,
        String bankId,
        String courseId,
        String questionType,
        String stem,
        String analysis,
        String difficulty,
        BigDecimal score,
        String status,
        List<QuestionOptionVO> options,
        Integer version) {
    public QuestionVO {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
