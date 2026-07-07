package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record CourseReviewVO(
        String reviewId,
        String courseId,
        CodeLabelVO reviewStatus,
        String reviewerId,
        String reviewerName,
        String reason,
        String remark,
        OffsetDateTime reviewedAt) {}
