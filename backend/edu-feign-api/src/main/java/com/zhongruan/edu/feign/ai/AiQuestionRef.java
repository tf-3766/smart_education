package com.zhongruan.edu.feign.ai;

import java.math.BigDecimal;

public record AiQuestionRef(
        Long questionId,
        Long bankId,
        String questionType,
        String stem,
        String difficulty,
        BigDecimal defaultScore) {}
