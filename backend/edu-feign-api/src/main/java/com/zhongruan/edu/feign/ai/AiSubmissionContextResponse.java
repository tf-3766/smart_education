package com.zhongruan.edu.feign.ai;

import java.math.BigDecimal;

public record AiSubmissionContextResponse(
        Long courseId,
        Long assignmentId,
        String assignmentTitle,
        String assignmentDescription,
        BigDecimal maxScore,
        Long submissionId,
        String submissionContent,
        BigDecimal currentScore) {}
