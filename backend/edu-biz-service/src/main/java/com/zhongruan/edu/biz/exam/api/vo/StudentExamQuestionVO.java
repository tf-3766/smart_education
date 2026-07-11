package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.util.List;

public record StudentExamQuestionVO(
        String questionId,
        Integer questionOrder,
        BigDecimal score,
        String questionType,
        String stem,
        List<StudentExamOptionVO> options) {
    public StudentExamQuestionVO {
        options = List.copyOf(options);
    }
}
