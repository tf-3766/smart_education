package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ExamAttemptVO(
        String attemptId,
        String examId,
        String paperId,
        String studentId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime deadlineAt,
        OffsetDateTime submittedAt,
        BigDecimal score,
        List<StudentExamQuestionVO> questions,
        List<ExamAnswerVO> answers,
        Integer version) {
    public ExamAttemptVO {
        questions = List.copyOf(questions);
        answers = List.copyOf(answers);
    }
}
