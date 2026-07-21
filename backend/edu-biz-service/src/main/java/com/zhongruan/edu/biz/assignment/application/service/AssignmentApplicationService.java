package com.zhongruan.edu.biz.assignment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.assignment.api.dto.query.AssignmentListQuery;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentAttachmentRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentCreateRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentQuestionRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.AssignmentUpdateRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.SubmissionSaveRequest;
import com.zhongruan.edu.biz.assignment.api.dto.request.SubmissionSubmitRequest;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.api.vo.StudentAssignmentListItemVO;
import com.zhongruan.edu.biz.assignment.api.vo.StudentAssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.api.vo.SubmissionDetailVO;
import com.zhongruan.edu.biz.assignment.application.assembler.AssignmentAssembler;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentAvailabilityStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentAttachmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentAttachmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.notification.application.service.NotificationApplicationService;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService;
import com.zhongruan.edu.biz.storage.domain.FilePurpose;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.entity.StoredFileEntity;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentApplicationService {
    private final AssignmentMapper assignmentMapper;
    private final AssignmentAttachmentMapper attachmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseManagementService courseManagementService;
    private final CoursePermissionService coursePermissionService;
    private final AssignmentAssembler assembler;
    private final FileStorageService fileStorageService;
    private final NotificationApplicationService notificationService;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public AssignmentApplicationService(
            AssignmentMapper assignmentMapper,
            AssignmentAttachmentMapper attachmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            CourseLessonMapper lessonMapper,
            CourseManagementService courseManagementService,
            CoursePermissionService coursePermissionService,
            AssignmentAssembler assembler,
            FileStorageService fileStorageService,
            NotificationApplicationService notificationService,
            ObjectMapper objectMapper) {
        this.assignmentMapper = assignmentMapper;
        this.attachmentMapper = attachmentMapper;
        this.submissionMapper = submissionMapper;
        this.lessonMapper = lessonMapper;
        this.courseManagementService = courseManagementService;
        this.coursePermissionService = coursePermissionService;
        this.assembler = assembler;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssignmentDetailVO create(Long teacherId, Long courseId, AssignmentCreateRequest request) {
        courseManagementService.requireEditor(teacherId, courseId);
        validateTimes(request.openAt(), request.dueAt());
        validateLesson(courseId, request.lessonId());
        validateAssignmentDefinition(request.responseMode(), request.questions(), request.maxScore());

        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setCourseId(courseId);
        assignment.setLessonId(request.lessonId());
        assignment.setTitle(request.title().trim());
        assignment.setDescription(trim(request.description()));
        assignment.setResponseMode(responseMode(request.responseMode()));
        assignment.setQuestionsJson(json(request.questions()));
        assignment.setMaxScore(request.maxScore());
        assignment.setStatus(AssignmentStatus.DRAFT.name());
        assignment.setOpenAt(utc(request.openAt()));
        assignment.setDueAt(utc(request.dueAt()));
        assignmentMapper.insert(assignment);
        replaceAttachments(teacherId, assignment.getId(), request.attachments());
        return detail(assignment);
    }

    /**
     * AI 自动流：复用常规创建逻辑落 DRAFT 作业，再标记 source=AI 作为待确认草稿。
     * 教师用既有的发布流（publish）确认发布；权限、时间、题目定义校验全部复用 create。
     */
    @Transactional
    public AssignmentDetailVO createAiDraftAssignment(Long teacherId, Long courseId, AssignmentCreateRequest request) {
        AssignmentDetailVO created = create(teacherId, courseId, request);
        AssignmentEntity assignment = assignmentMapper.selectById(Long.valueOf(created.assignmentId()));
        assignment.setSource("AI");
        assignmentMapper.updateById(assignment);
        return detail(assignment);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssignmentDetailVO> listForTeacher(
            Long teacherId, Long courseId, AssignmentListQuery query) {
        courseManagementService.requireEditor(teacherId, courseId);
        var wrapper = Wrappers.<AssignmentEntity>lambdaQuery().eq(AssignmentEntity::getCourseId, courseId);
        applyAssignmentFilters(wrapper, query);
        applyAssignmentSort(wrapper, query.getSort());
        IPage<AssignmentEntity> page = assignmentMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<AssignmentDetailVO> records = page.getRecords().stream()
                .map(this::detail)
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public AssignmentDetailVO update(Long teacherId, Long assignmentId, AssignmentUpdateRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        courseManagementService.requireEditor(teacherId, assignment.getCourseId());
        AssignmentStatus status = AssignmentStatus.valueOf(assignment.getStatus());
        if (status == AssignmentStatus.CLOSED) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已关闭作业不能修改");
        }
        validateTimes(request.openAt(), request.dueAt());
        validateLesson(assignment.getCourseId(), request.lessonId());
        validateAssignmentDefinition(request.responseMode(), request.questions(), request.maxScore());
        ensurePublishedUpdateSafe(assignment, request);
        assignment.setLessonId(request.lessonId());
        assignment.setTitle(request.title().trim());
        assignment.setDescription(trim(request.description()));
        assignment.setResponseMode(responseMode(request.responseMode()));
        assignment.setQuestionsJson(json(request.questions()));
        assignment.setMaxScore(request.maxScore());
        assignment.setOpenAt(utc(request.openAt()));
        assignment.setDueAt(utc(request.dueAt()));
        assignment.setVersion(request.version());
        updateAssignmentOrConflict(assignment);
        replaceAttachments(teacherId, assignment.getId(), request.attachments());
        return detail(requireAssignment(assignmentId));
    }

    @Transactional
    public AssignmentDetailVO publish(Long teacherId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        courseManagementService.requireEditor(teacherId, assignment.getCourseId());
        AssignmentStatus status = AssignmentStatus.valueOf(assignment.getStatus());
        if (!status.canTransitionTo(AssignmentStatus.PUBLISHED)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "当前作业状态不能发布");
        }
        assignment.setStatus(AssignmentStatus.PUBLISHED.name());
        assignment.setPublishedAt(now());
        updateAssignmentOrConflict(assignment);
        notificationService.publishAssignment(assignment);
        return detail(assignment);
    }

    @Transactional
    public AssignmentDetailVO close(Long teacherId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        courseManagementService.requireEditor(teacherId, assignment.getCourseId());
        AssignmentStatus status = AssignmentStatus.valueOf(assignment.getStatus());
        if (!status.canTransitionTo(AssignmentStatus.CLOSED)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "当前作业状态不能关闭");
        }
        assignment.setStatus(AssignmentStatus.CLOSED.name());
        updateAssignmentOrConflict(assignment);
        notificationService.publishAssignmentDeadline(assignment);
        return detail(assignment);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentAssignmentListItemVO> listForStudent(
            Long studentId, Long courseId, AssignmentListQuery query) {
        requireStudentCourseAccess(studentId, courseId);
        var wrapper = Wrappers.<AssignmentEntity>lambdaQuery()
                .eq(AssignmentEntity::getCourseId, courseId)
                .eq(AssignmentEntity::getStatus, AssignmentStatus.PUBLISHED.name());
        applyAssignmentFilters(wrapper, query);
        applyAssignmentSort(wrapper, query.getSort());
        IPage<AssignmentEntity> page = assignmentMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, AssignmentSubmissionEntity> submissions = submissionMap(studentId, page.getRecords());
        List<StudentAssignmentListItemVO> records = page.getRecords().stream()
                .map(assignment -> assembler.toStudentListItem(
                        assignment, submissions.get(assignment.getId()), availability(assignment)))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public StudentAssignmentDetailVO getForStudent(Long studentId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        requireStudentAssignmentAccess(studentId, assignment);
        AssignmentSubmissionEntity submission = findSubmission(assignmentId, studentId);
        return new StudentAssignmentDetailVO(
                assembler.toStudentDetail(assignment, attachments(assignment.getId()), availability(assignment)),
                submission == null ? null : assembler.toSubmissionDetail(submission));
    }

    @Transactional
    public SubmissionDetailVO saveDraft(Long studentId, Long assignmentId, SubmissionSaveRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        requireStudentAssignmentAccess(studentId, assignment);
        ensureAcceptingWork(assignment, false);
        AssignmentSubmissionEntity existing = findSubmission(assignmentId, studentId);
        if (existing != null && SubmissionStatus.valueOf(existing.getStatus()).isFinalSubmission()) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "作业已正式提交，不能修改草稿");
        }
        AssignmentSubmissionEntity submission = existing == null ? new AssignmentSubmissionEntity() : existing;
        if (existing == null) {
            submission.setAssignmentId(assignmentId);
            submission.setCourseId(assignment.getCourseId());
            submission.setStudentId(studentId);
            submission.setAttemptNo(1);
            submission.setStatus(SubmissionStatus.DRAFT.name());
        } else if (request.version() != null) {
            submission.setVersion(request.version());
        }
        FileReference file = submissionFile(studentId, request.fileId(), request.fileKey(), request.fileUrl());
        submission.setContent(trim(request.content()));
        submission.setAnswersJson(json(request.answers()));
        submission.setFileId(file.fileId());
        submission.setFileKey(file.fileKey());
        submission.setFileUrl(file.fileUrl());
        if (existing == null) {
            submissionMapper.insert(submission);
        } else {
            updateSubmissionOrConflict(submission);
        }
        return assembler.toSubmissionDetail(submission);
    }

    @Transactional
    public SubmissionDetailVO submit(Long studentId, Long assignmentId, SubmissionSubmitRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        requireStudentAssignmentAccess(studentId, assignment);
        ensureAcceptingWork(assignment, true);
        if (isBlank(request.content()) && (request.answers() == null || request.answers().isEmpty())
                && request.fileId() == null && isBlank(request.fileKey()) && isBlank(request.fileUrl())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "正式提交至少需要在线回答、题目答案或附件");
        }
        AssignmentSubmissionEntity existing = findSubmission(assignmentId, studentId);
        if (existing != null && SubmissionStatus.valueOf(existing.getStatus()).isFinalSubmission()) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "作业已正式提交，请勿重复提交");
        }
        AssignmentSubmissionEntity submission = existing == null ? new AssignmentSubmissionEntity() : existing;
        if (existing == null) {
            submission.setAssignmentId(assignmentId);
            submission.setCourseId(assignment.getCourseId());
            submission.setStudentId(studentId);
            submission.setAttemptNo(1);
        } else if (request.version() != null) {
            submission.setVersion(request.version());
        }
        FileReference file = submissionFile(studentId, request.fileId(), request.fileKey(), request.fileUrl());
        submission.setContent(trim(request.content()));
        submission.setAnswersJson(json(request.answers()));
        submission.setFileId(file.fileId());
        submission.setFileKey(file.fileKey());
        submission.setFileUrl(file.fileUrl());
        submission.setStatus(SubmissionStatus.SUBMITTED.name());
        submission.setSubmittedAt(now());
        if (existing == null) {
            submissionMapper.insert(submission);
        } else {
            updateSubmissionOrConflict(submission);
        }
        notificationService.publishAssignmentSubmission(assignment, submission);
        return assembler.toSubmissionDetail(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionDetailVO> listSubmissions(
            Long teacherId, Long assignmentId, AssignmentListQuery query) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        courseManagementService.requireEditor(teacherId, assignment.getCourseId());
        var wrapper = Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                .eq(AssignmentSubmissionEntity::getAssignmentId, assignmentId);
        if (query.getSubmissionStatus() != null) {
            wrapper.eq(AssignmentSubmissionEntity::getStatus, query.getSubmissionStatus().name());
        }
        wrapper.orderByDesc(AssignmentSubmissionEntity::getSubmittedAt)
                .orderByDesc(AssignmentSubmissionEntity::getUpdatedAt)
                .orderByDesc(AssignmentSubmissionEntity::getId);
        IPage<AssignmentSubmissionEntity> page =
                submissionMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<SubmissionDetailVO> records = page.getRecords().stream()
                .map(assembler::toSubmissionDetail)
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private AssignmentDetailVO detail(AssignmentEntity assignment) {
        return assembler.toDetail(assignment, attachments(assignment.getId()), availability(assignment));
    }

    private AssignmentEntity requireAssignment(Long assignmentId) {
        AssignmentEntity assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "作业不存在或已不可访问");
        }
        return assignment;
    }

    private void requireStudentAssignmentAccess(Long studentId, AssignmentEntity assignment) {
        if (!AssignmentStatus.PUBLISHED.name().equals(assignment.getStatus())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "作业不存在或已不可访问");
        }
        requireStudentCourseAccess(studentId, assignment.getCourseId());
    }

    private void requireStudentCourseAccess(Long studentId, Long courseId) {
        if (!coursePermissionService.canViewCourseAsStudent(studentId, courseId)) {
            courseManagementService.requireCourse(courseId);
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "选课后才能访问课程作业");
        }
    }

    private void validateLesson(Long courseId, Long lessonId) {
        if (lessonId == null) {
            return;
        }
        CourseLessonEntity lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || !courseId.equals(lesson.getCourseId())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课时不属于当前课程");
        }
    }

    private void validateAssignmentDefinition(
            String requestedMode, List<AssignmentQuestionRequest> questions, BigDecimal maxScore) {
        String mode = responseMode(requestedMode);
        List<AssignmentQuestionRequest> items = questions == null ? List.of() : questions;
        if ("QUIZ".equals(mode) && items.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "题目作答模式至少需要一道题");
        }
        Set<String> ids = new HashSet<>();
        BigDecimal questionScore = BigDecimal.ZERO;
        for (AssignmentQuestionRequest question : items) {
            if (!ids.add(question.questionId())) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "作业题目编号不能重复");
            }
            List<String> options = question.options() == null ? List.of() : question.options();
            List<String> answers = question.correctAnswers() == null ? List.of() : question.correctAnswers();
            switch (question.questionType()) {
                case "SINGLE_CHOICE", "TRUE_FALSE" -> {
                    if (options.size() < 2 || answers.size() != 1) {
                        throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "单选题和判断题需要至少两个选项且只能有一个正确答案");
                    }
                }
                case "MULTI_CHOICE" -> {
                    if (options.size() < 2 || answers.isEmpty()) {
                        throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "多选题需要至少两个选项和一个正确答案");
                    }
                }
                case "FILL_BLANK" -> {
                    if (answers.isEmpty()) {
                        throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "填空题至少需要一个参考答案");
                    }
                }
                case "SHORT_ANSWER" -> {
                    // 简答题允许不配置标准答案，由教师人工评分。
                }
                default -> throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "不支持的作业题型");
            }
            questionScore = questionScore.add(question.score());
        }
        if (questionScore.compareTo(maxScore) > 0) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "题目分值合计不能超过作业满分");
        }
    }

    private String responseMode(String value) {
        return value == null || value.isBlank() ? "MIXED" : value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "作业题目或答案格式不正确");
        }
    }

    private void validateTimes(OffsetDateTime openAt, OffsetDateTime dueAt) {
        if (openAt != null && dueAt != null && !dueAt.isAfter(openAt)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "作业截止时间必须晚于开放时间");
        }
    }

    private void ensurePublishedUpdateSafe(AssignmentEntity assignment, AssignmentUpdateRequest request) {
        if (!AssignmentStatus.PUBLISHED.name().equals(assignment.getStatus())) {
            return;
        }
        boolean gradingBasisChanged = !Objects.equals(assignment.getLessonId(), request.lessonId())
                || !Objects.equals(assignment.getResponseMode(), responseMode(request.responseMode()))
                || !Objects.equals(assignment.getQuestionsJson(), json(request.questions()))
                || assignment.getMaxScore().compareTo(request.maxScore()) != 0
                || !Objects.equals(assignment.getOpenAt(), utc(request.openAt()))
                || !Objects.equals(assignment.getDueAt(), utc(request.dueAt()));
        if (!gradingBasisChanged) {
            return;
        }
        Long submissionCount = submissionMapper.selectCount(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                .eq(AssignmentSubmissionEntity::getAssignmentId, assignment.getId()));
        if (submissionCount != null && submissionCount > 0) {
            throw new BusinessException(
                    CommonErrorCode.OPERATION_NOT_ALLOWED,
                    "作业已有提交，不能修改关联课时、分值或开放截止时间");
        }
    }

    private void ensureAcceptingWork(AssignmentEntity assignment, boolean formalSubmit) {
        AssignmentAvailabilityStatus availability = availability(assignment);
        if (availability == AssignmentAvailabilityStatus.CLOSED) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "作业未发布或已关闭");
        }
        if (availability == AssignmentAvailabilityStatus.OVERDUE) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该作业已截止，当前不允许提交");
        }
        if (formalSubmit && availability == AssignmentAvailabilityStatus.NOT_OPEN) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "作业尚未开放，当前不允许提交");
        }
    }

    private AssignmentAvailabilityStatus availability(AssignmentEntity assignment) {
        AssignmentStatus status = AssignmentStatus.valueOf(assignment.getStatus());
        if (status != AssignmentStatus.PUBLISHED) {
            return AssignmentAvailabilityStatus.CLOSED;
        }
        LocalDateTime now = now();
        if (assignment.getOpenAt() != null && now.isBefore(assignment.getOpenAt())) {
            return AssignmentAvailabilityStatus.NOT_OPEN;
        }
        if (assignment.getDueAt() != null && now.isAfter(assignment.getDueAt())) {
            return AssignmentAvailabilityStatus.OVERDUE;
        }
        return AssignmentAvailabilityStatus.OPEN;
    }

    private List<AssignmentAttachmentEntity> attachments(Long assignmentId) {
        return attachmentMapper.selectList(Wrappers.<AssignmentAttachmentEntity>lambdaQuery()
                .eq(AssignmentAttachmentEntity::getAssignmentId, assignmentId)
                .orderByAsc(AssignmentAttachmentEntity::getSortOrder)
                .orderByAsc(AssignmentAttachmentEntity::getId));
    }

    private void replaceAttachments(
            Long teacherId, Long assignmentId, List<AssignmentAttachmentRequest> attachments) {
        attachmentMapper.delete(Wrappers.<AssignmentAttachmentEntity>lambdaQuery()
                .eq(AssignmentAttachmentEntity::getAssignmentId, assignmentId));
        if (attachments == null) {
            return;
        }
        for (AssignmentAttachmentRequest request : attachments) {
            FileReference file = attachmentFile(teacherId, request);
            AssignmentAttachmentEntity attachment = new AssignmentAttachmentEntity();
            attachment.setAssignmentId(assignmentId);
            attachment.setName(request.name().trim());
            attachment.setFileId(file.fileId());
            attachment.setFileKey(file.fileKey());
            attachment.setFileUrl(file.fileUrl());
            attachment.setFileSize(file.fileSize());
            attachment.setMimeType(file.mimeType());
            attachment.setSortOrder(request.sortOrder());
            attachmentMapper.insert(attachment);
        }
    }

    private FileReference attachmentFile(Long teacherId, AssignmentAttachmentRequest request) {
        if (request.fileId() != null) {
            rejectMixedFileReference(request.fileKey(), request.fileUrl());
            StoredFileEntity file = fileStorageService.requireOwnedFile(
                    teacherId, request.fileId(), FilePurpose.ASSIGNMENT_ATTACHMENT);
            return managedFile(file);
        }
        if (isBlank(request.fileKey()) && isBlank(request.fileUrl())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "作业附件必须提供 fileId、fileKey 或 fileUrl");
        }
        return new FileReference(
                null, trim(request.fileKey()), trim(request.fileUrl()), request.fileSize(), trim(request.mimeType()));
    }

    private FileReference submissionFile(Long studentId, Long fileId, String fileKey, String fileUrl) {
        if (fileId != null) {
            rejectMixedFileReference(fileKey, fileUrl);
            return managedFile(fileStorageService.requireOwnedFile(studentId, fileId, FilePurpose.SUBMISSION));
        }
        return new FileReference(null, trim(fileKey), trim(fileUrl), null, null);
    }

    private void rejectMixedFileReference(String fileKey, String fileUrl) {
        if (!isBlank(fileKey) || !isBlank(fileUrl)) {
            throw new BusinessException(
                    CommonErrorCode.PARAM_VALIDATION_ERROR, "fileId 不能与 fileKey 或 fileUrl 同时提交");
        }
    }

    private FileReference managedFile(StoredFileEntity file) {
        return new FileReference(
                file.getId(),
                file.getObjectKey(),
                fileStorageService.accessUrl(file.getId()),
                file.getFileSize(),
                file.getMimeType());
    }

    private record FileReference(
            Long fileId, String fileKey, String fileUrl, Long fileSize, String mimeType) {}

    private AssignmentSubmissionEntity findSubmission(Long assignmentId, Long studentId) {
        return submissionMapper.selectOne(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                .eq(AssignmentSubmissionEntity::getAssignmentId, assignmentId)
                .eq(AssignmentSubmissionEntity::getStudentId, studentId)
                .orderByDesc(AssignmentSubmissionEntity::getAttemptNo)
                .orderByDesc(AssignmentSubmissionEntity::getId)
                .last("LIMIT 1"));
    }

    private Map<Long, AssignmentSubmissionEntity> submissionMap(
            Long studentId, List<AssignmentEntity> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }
        List<Long> assignmentIds = assignments.stream().map(AssignmentEntity::getId).toList();
        return submissionMapper.selectList(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                        .eq(AssignmentSubmissionEntity::getStudentId, studentId)
                        .in(AssignmentSubmissionEntity::getAssignmentId, assignmentIds))
                .stream()
                .collect(Collectors.toMap(
                        AssignmentSubmissionEntity::getAssignmentId,
                        Function.identity(),
                        (left, right) -> left.getAttemptNo() >= right.getAttemptNo() ? left : right));
    }

    private void applyAssignmentFilters(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssignmentEntity> wrapper,
            AssignmentListQuery query) {
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(group -> group.like(AssignmentEntity::getTitle, keyword)
                    .or()
                    .like(AssignmentEntity::getDescription, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(AssignmentEntity::getStatus, query.getStatus().name());
        }
    }

    private void applyAssignmentSort(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssignmentEntity> wrapper, String sort) {
        String value = sort == null ? "createdAt,desc" : sort;
        boolean asc = value.endsWith(",asc");
        if (value.startsWith("dueAt,")) {
            wrapper.orderBy(true, asc, AssignmentEntity::getDueAt);
        } else if (value.startsWith("createdAt,")) {
            wrapper.orderBy(true, asc, AssignmentEntity::getCreatedAt);
        } else if (value.startsWith("publishedAt,")) {
            wrapper.orderBy(true, asc, AssignmentEntity::getPublishedAt);
        } else {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "不支持的作业排序字段");
        }
        wrapper.orderByDesc(AssignmentEntity::getId);
    }

    private void updateAssignmentOrConflict(AssignmentEntity assignment) {
        if (assignmentMapper.updateById(assignment) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "作业已被其他请求修改，请刷新后重试");
        }
    }

    private void updateSubmissionOrConflict(AssignmentSubmissionEntity submission) {
        if (submissionMapper.updateById(submission) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "提交已被其他请求修改，请刷新后重试");
        }
    }

    private LocalDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
