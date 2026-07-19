package com.zhongruan.edu.biz.assignment.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record SubmissionDetailVO(
        String submissionId,
        String assignmentId,
        String courseId,
        String studentId,
        Integer attemptNo,
        String content,
        Map<String, List<String>> answers,
        String fileId,
        String fileKey,
        String fileUrl,
        CodeLabelVO submissionStatus,
        OffsetDateTime submittedAt,
        BigDecimal score,
        String teacherComment,
        String aiCommentDraftId,
        String gradedBy,
        OffsetDateTime gradedAt,
        OffsetDateTime publishedAt,
        Integer version) {}
