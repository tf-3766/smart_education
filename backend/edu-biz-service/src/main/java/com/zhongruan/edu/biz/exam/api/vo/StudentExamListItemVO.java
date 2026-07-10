package com.zhongruan.edu.biz.exam.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentExamListItemVO(
        String examId,
        String courseId,
        String title,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer durationMinutes,
        BigDecimal totalScore) {}
