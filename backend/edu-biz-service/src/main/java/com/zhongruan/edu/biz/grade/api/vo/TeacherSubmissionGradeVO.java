package com.zhongruan.edu.biz.grade.api.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record TeacherSubmissionGradeVO(
        String submissionId,
        String assignmentId,
        String courseId,
        String studentId,
        String studentName,
        CodeLabelVO submissionStatus,
        OffsetDateTime submittedAt,
        String content,
        Map<String, List<String>> answers,
        String fileId,
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
