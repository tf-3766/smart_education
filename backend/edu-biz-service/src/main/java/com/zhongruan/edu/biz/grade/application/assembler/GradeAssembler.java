package com.zhongruan.edu.biz.grade.application.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.grade.api.vo.AssignmentStatisticsVO;
import com.zhongruan.edu.biz.grade.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.grade.api.vo.StudentGradeVO;
import com.zhongruan.edu.biz.grade.api.vo.TeacherSubmissionGradeVO;
import com.zhongruan.edu.biz.grade.domain.enums.GradeStatus;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.entity.GradeRecordEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GradeAssembler {
    private static final TypeReference<Map<String, List<String>>> ANSWER_MAP = new TypeReference<>() {};
    private final ObjectMapper objectMapper;

    public GradeAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public TeacherSubmissionGradeVO toTeacherSubmission(
            AssignmentSubmissionEntity submission,
            AssignmentEntity assignment,
            GradeRecordEntity grade,
            String studentName) {
        GradeStatus gradeStatus = grade == null ? null : GradeStatus.valueOf(grade.getGradeStatus());
        return new TeacherSubmissionGradeVO(
                id(submission.getId()),
                id(submission.getAssignmentId()),
                id(submission.getCourseId()),
                id(submission.getStudentId()),
                studentName,
                CodeLabelVO.of(SubmissionStatus.valueOf(submission.getStatus())),
                time(submission.getSubmittedAt()),
                submission.getContent(),
                answers(submission.getAnswersJson()),
                id(submission.getFileId()),
                submission.getFileKey(),
                submission.getFileUrl(),
                submission.getScore(),
                assignment.getMaxScore(),
                submission.getTeacherComment(),
                id(submission.getAiCommentDraftId()),
                id(submission.getGradedBy()),
                time(submission.getGradedAt()),
                grade == null ? null : id(grade.getId()),
                gradeStatus == null ? null : CodeLabelVO.of(gradeStatus),
                grade == null ? time(submission.getPublishedAt()) : time(grade.getPublishedAt()),
                submission.getVersion(),
                grade == null ? null : grade.getVersion());
    }

    public StudentGradeVO toStudentGrade(GradeRecordEntity grade, AssignmentEntity assignment) {
        BigDecimal scoreRate = grade.getMaxScore().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(4)
                : grade.getScore().divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP);
        return new StudentGradeVO(
                id(grade.getId()),
                id(grade.getCourseId()),
                id(grade.getSourceId()),
                assignment == null ? null : assignment.getTitle(),
                grade.getScore(),
                grade.getMaxScore(),
                scoreRate,
                grade.getComment(),
                time(grade.getPublishedAt()));
    }

    public AssignmentStatisticsVO toStatistics(
            Long assignmentId,
            Long courseId,
            long totalStudentCount,
            long submittedCount,
            long gradedCount,
            long publishedGradeCount,
            BigDecimal averageScore,
            long lowScoreCount) {
        return new AssignmentStatisticsVO(
                id(assignmentId),
                id(courseId),
                totalStudentCount,
                submittedCount,
                Math.max(totalStudentCount - submittedCount, 0),
                gradedCount,
                publishedGradeCount,
                averageScore,
                lowScoreCount);
    }

    private Map<String, List<String>> answers(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, ANSWER_MAP);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
