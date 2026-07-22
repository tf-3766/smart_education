package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExamVO(
        String examId,
        String courseId,
        String title,
        String description,
        String status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer durationMinutes,
        BigDecimal totalScore,
        String source,
        Integer version) {}
