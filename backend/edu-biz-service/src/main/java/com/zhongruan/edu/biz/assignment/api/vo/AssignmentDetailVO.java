package com.zhongruan.edu.biz.assignment.api.vo;

import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentQuestionRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AssignmentDetailVO(
        String assignmentId,
        String courseId,
        String lessonId,
        String title,
        String description,
        String responseMode,
        List<AssignmentQuestionRequest> questions,
        BigDecimal maxScore,
        CodeLabelVO assignmentStatus,
        CodeLabelVO availabilityStatus,
        OffsetDateTime openAt,
        OffsetDateTime dueAt,
        OffsetDateTime publishedAt,
        List<AssignmentAttachmentVO> attachments,
        Integer version) {}
