package com.zhongruan.edu.biz.grade.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TeacherSubmissionGradeVO(
        String submissionId,
        String assignmentId,
        String courseId,
        String studentId,
        String studentName,
        CodeLabelVO submissionStatus,
        OffsetDateTime submittedAt,
        String content,
        String fileKey,
        String fileUrl,
        BigDecimal score,
        BigDecimal maxScore,
        String teacherComment,
        String aiCommentDraftId,
        String gradedBy,
        OffsetDateTime gradedAt,
        String gradeId,
        CodeLabelVO gradeStatus,
        OffsetDateTime publishedAt,
        Integer version,
        Integer gradeVersion) {}
