package com.zhongruan.edu.biz.assignment.application.assembler;

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
import org.springframework.stereotype.Component;

@Component
public class AssignmentAssembler {
    public AssignmentDetailVO toDetail(
            AssignmentEntity assignment,
            List<AssignmentAttachmentEntity> attachments,
            AssignmentAvailabilityStatus availabilityStatus) {
        return new AssignmentDetailVO(
                id(assignment.getId()),
                id(assignment.getCourseId()),
                id(assignment.getLessonId()),
                assignment.getTitle(),
                assignment.getDescription(),
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
