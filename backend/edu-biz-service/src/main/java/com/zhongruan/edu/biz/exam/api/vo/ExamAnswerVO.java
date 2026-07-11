package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;

public record ExamAnswerVO(String questionId, String answerContent, BigDecimal score) {}
