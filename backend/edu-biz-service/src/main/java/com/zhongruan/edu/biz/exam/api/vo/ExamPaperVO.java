package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.util.List;

public record ExamPaperVO(
        String paperId,
        String examId,
        String courseId,
        String title,
        BigDecimal totalScore,
        String status,
        List<ExamPaperQuestionVO> questions,
        Integer version) {
    public ExamPaperVO {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
