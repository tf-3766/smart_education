package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;

public record ExamPaperQuestionVO(
        String questionId,
        Integer questionOrder,
        BigDecimal score,
        String questionType,
        String stem) {}
