package com.zhongruan.edu.biz.assignment.application.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentQuestionRequest;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentAttachmentVO;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.assignment.api.vo.StudentAssignmentListItemVO;
import com.zhongruan.edu.biz.assignment.api.vo.SubmissionDetailVO;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentAvailabilityStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentAttachmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AssignmentAssembler {
    private static final TypeReference<List<AssignmentQuestionRequest>> QUESTION_LIST =
            new TypeReference<>() {};
    private static final TypeReference<Map<String, List<String>>> ANSWER_MAP =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public AssignmentAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssignmentDetailVO toDetail(
            AssignmentEntity assignment,
            List<AssignmentAttachmentEntity> attachments,
            AssignmentAvailabilityStatus availabilityStatus) {
        return toDetail(assignment, attachments, availabilityStatus, true);
    }

    public AssignmentDetailVO toStudentDetail(
            AssignmentEntity assignment,
            List<AssignmentAttachmentEntity> attachments,
            AssignmentAvailabilityStatus availabilityStatus) {
        return toDetail(assignment, attachments, availabilityStatus, false);
    }

    private AssignmentDetailVO toDetail(
            AssignmentEntity assignment,
            List<AssignmentAttachmentEntity> attachments,
            AssignmentAvailabilityStatus availabilityStatus,
            boolean includeCorrectAnswers) {
        List<AssignmentQuestionRequest> questions = questions(assignment.getQuestionsJson());
        if (!includeCorrectAnswers) {
            questions = questions.stream().map(AssignmentQuestionRequest::withoutCorrectAnswers).toList();
        }
        return new AssignmentDetailVO(
                id(assignment.getId()),
                id(assignment.getCourseId()),
                id(assignment.getLessonId()),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getResponseMode() == null ? "MIXED" : assignment.getResponseMode(),
                questions,
                assignment.getMaxScore(),
                CodeLabelVO.of(AssignmentStatus.valueOf(assignment.getStatus())),
                CodeLabelVO.of(availabilityStatus),
                time(assignment.getOpenAt()),
                time(assignment.getDueAt()),
                time(assignment.getPublishedAt()),
                attachments.stream().map(this::toAttachment).toList(),
                assignment.getVersion());
    }

    public StudentAssignmentListItemVO toStudentListItem(
            AssignmentEntity assignment,
            AssignmentSubmissionEntity submission,
            AssignmentAvailabilityStatus availabilityStatus) {
        SubmissionStatus submissionStatus = submission == null ? null : SubmissionStatus.valueOf(submission.getStatus());
        return new StudentAssignmentListItemVO(
                id(assignment.getId()),
                id(assignment.getCourseId()),
                id(assignment.getLessonId()),
                assignment.getTitle(),
                assignment.getMaxScore(),
                CodeLabelVO.of(availabilityStatus),
                time(assignment.getDueAt()),
                submissionStatus == null ? null : CodeLabelVO.of(submissionStatus),
                submission == null ? null : time(submission.getSubmittedAt()),
                submissionStatus == SubmissionStatus.GRADED);
    }

    public SubmissionDetailVO toSubmissionDetail(AssignmentSubmissionEntity submission) {
        return new SubmissionDetailVO(
                id(submission.getId()),
                id(submission.getAssignmentId()),
                id(submission.getCourseId()),
                id(submission.getStudentId()),
                submission.getAttemptNo(),
                submission.getContent(),
                answers(submission.getAnswersJson()),
                id(submission.getFileId()),
                submission.getFileKey(),
                submission.getFileUrl(),
                CodeLabelVO.of(SubmissionStatus.valueOf(submission.getStatus())),
                time(submission.getSubmittedAt()),
                submission.getScore(),
                submission.getTeacherComment(),
                id(submission.getAiCommentDraftId()),
                id(submission.getGradedBy()),
                time(submission.getGradedAt()),
                time(submission.getPublishedAt()),
                submission.getVersion());
    }

    private List<AssignmentQuestionRequest> questions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, QUESTION_LIST);
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
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

    private AssignmentAttachmentVO toAttachment(AssignmentAttachmentEntity attachment) {
        return new AssignmentAttachmentVO(
                id(attachment.getId()),
                attachment.getName(),
                id(attachment.getFileId()),
                attachment.getFileKey(),
                attachment.getFileUrl(),
                attachment.getFileSize(),
                attachment.getMimeType(),
                attachment.getSortOrder());
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}