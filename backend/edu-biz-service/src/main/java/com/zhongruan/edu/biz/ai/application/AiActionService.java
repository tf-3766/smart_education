package com.zhongruan.edu.biz.ai.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.biz.ai.infrastructure.persistence.entity.AiActionEntity;
import com.zhongruan.edu.biz.ai.infrastructure.persistence.mapper.AiActionMapper;
import com.zhongruan.edu.biz.assignment.api.vo.AssignmentDetailVO;
import com.zhongruan.edu.biz.assignment.application.service.AssignmentApplicationService;
import com.zhongruan.edu.biz.auth.api.vo.AdminUserVO;
import com.zhongruan.edu.biz.auth.application.service.AdminUserApplicationService;
import com.zhongruan.edu.biz.grade.api.dto.request.GradeSubmissionRequest;
import com.zhongruan.edu.biz.grade.api.vo.TeacherSubmissionGradeVO;
import com.zhongruan.edu.biz.grade.application.service.GradeApplicationService;
import com.zhongruan.edu.biz.platform.api.dto.request.UpsertTermEnrollmentWindowRequest;
import com.zhongruan.edu.biz.platform.api.vo.TermEnrollmentWindowVO;
import com.zhongruan.edu.biz.platform.application.service.TermEnrollmentWindowService;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiActionPlanRequest;
import com.zhongruan.edu.feign.ai.AiActionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiActionService {
    public static final String TERM_WINDOW_UPSERT = "platform.term-enrollment-window.upsert";
    public static final String ASSIGNMENT_PUBLISH = "course.assignment.publish";
    public static final String SUBMISSION_GRADE = "course.submission.grade";
    public static final String TEACHER_REGISTRATION_REVIEW = "admin.teacher-registration.review";

    private static final String WAITING_CONFIRMATION = "WAITING_CONFIRMATION";
    private static final String EXECUTING = "EXECUTING";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";
    private static final String CANCELLED = "CANCELLED";
    private static final String EXPIRED = "EXPIRED";

    private final AiActionMapper mapper;
    private final TermEnrollmentWindowService termWindowService;
    private final AssignmentApplicationService assignmentService;
    private final GradeApplicationService gradeService;
    private final AdminUserApplicationService adminUserService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Clock clock;

    @Autowired
    public AiActionService(
            AiActionMapper mapper,
            TermEnrollmentWindowService termWindowService,
            AssignmentApplicationService assignmentService,
            GradeApplicationService gradeService,
            AdminUserApplicationService adminUserService,
            ObjectMapper objectMapper,
            Validator validator) {
        this(mapper, termWindowService, assignmentService, gradeService, adminUserService,
                objectMapper, validator, Clock.systemUTC());
    }

    AiActionService(
            AiActionMapper mapper,
            TermEnrollmentWindowService termWindowService,
            AssignmentApplicationService assignmentService,
            GradeApplicationService gradeService,
            AdminUserApplicationService adminUserService,
            ObjectMapper objectMapper,
            Validator validator,
            Clock clock) {
        this.mapper = mapper;
        this.termWindowService = termWindowService;
        this.assignmentService = assignmentService;
        this.gradeService = gradeService;
        this.adminUserService = adminUserService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.clock = clock;
    }

    public AiActionResponse plan(AiActionPlanRequest request, String traceId) {
        AiActionEntity existing = findByIdempotencyKey(request.idempotencyKey());
        if (existing != null) {
            requireSamePlan(existing, request);
            return response(existing);
        }

        PlanDraft draft = prepare(request);
        AiActionEntity entity = new AiActionEntity();
        entity.setRequesterId(request.userId());
        entity.setRoleCode(request.roleCode());
        entity.setCapabilityId(request.capabilityId());
        entity.setIdempotencyKey(request.idempotencyKey());
        entity.setStatus(WAITING_CONFIRMATION);
        entity.setRiskLevel(draft.riskLevel());
        entity.setConfirmationPolicy(draft.confirmationPolicy());
        entity.setTargetType(draft.targetType());
        entity.setTargetId(draft.targetId());
        entity.setTargetVersion(draft.targetVersion());
        entity.setTitle(draft.title());
        entity.setSummary(draft.summary());
        entity.setParametersJson(request.parametersJson());
        entity.setPreviewJson(json(draft.preview()));
        entity.setTraceId(traceId);
        entity.setExpiresAt(now().plusMinutes(30));
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            AiActionEntity raced = findByIdempotencyKey(request.idempotencyKey());
            if (raced == null) {
                throw exception;
            }
            requireSamePlan(raced, request);
            return response(raced);
        }
        return response(entity);
    }

    public AiActionResponse get(Long userId, String roleCode, Long actionId) {
        return response(refreshExpiry(requireOwned(userId, roleCode, actionId)));
    }

    public List<AiActionResponse> listMine(Long userId, String roleCode, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        return mapper.selectList(Wrappers.<AiActionEntity>lambdaQuery()
                        .eq(AiActionEntity::getRequesterId, userId)
                        .eq(AiActionEntity::getRoleCode, roleCode)
                        .orderByDesc(AiActionEntity::getCreatedAt)
                        .last("LIMIT " + limit))
                .stream()
                .map(this::refreshExpiry)
                .map(this::response)
                .toList();
    }

    public AiActionResponse confirm(
            Long userId, String roleCode, Long actionId, String confirmationText) {
        AiActionEntity entity = requireOwned(userId, roleCode, actionId);
        if (SUCCEEDED.equals(entity.getStatus()) || FAILED.equals(entity.getStatus())) {
            return response(entity);
        }
        if (!WAITING_CONFIRMATION.equals(entity.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该 AI 动作当前不能确认");
        }
        if ("STRONG_CONFIRM".equals(entity.getConfirmationPolicy())
                && !"确认执行".equals(confirmationText == null ? null : confirmationText.trim())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "请输入“确认执行”完成强确认");
        }
        if (entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(now())) {
            entity.setStatus(EXPIRED);
            entity.setErrorCode("ACTION_EXPIRED");
            entity.setErrorMessage("动作计划已过期，请重新向助手发起请求");
            updateOrConflict(entity);
            return response(entity);
        }

        entity.setStatus(EXECUTING);
        entity.setConfirmedBy(userId);
        entity.setConfirmedAt(now());
        updateOrConflict(entity);
        try {
            ExecutionResult result = execute(entity);
            entity.setStatus(SUCCEEDED);
            entity.setResultJson(json(result.result()));
            entity.setResourceType(result.resourceType());
            entity.setResourceId(result.resourceId());
            entity.setResourceHref(result.href());
            entity.setExecutedAt(now());
            updateOrConflict(entity);
        } catch (RuntimeException exception) {
            entity.setStatus(FAILED);
            entity.setExecutedAt(now());
            entity.setErrorCode(exception instanceof BusinessException business
                    ? business.errorCode().code()
                    : CommonErrorCode.INTERNAL_ERROR.code());
            entity.setErrorMessage(safeMessage(exception));
            updateOrConflict(entity);
        }
        return response(entity);
    }

    public AiActionResponse cancel(Long userId, String roleCode, Long actionId) {
        AiActionEntity entity = requireOwned(userId, roleCode, actionId);
        if (CANCELLED.equals(entity.getStatus()) || EXPIRED.equals(entity.getStatus())) {
            return response(entity);
        }
        if (!WAITING_CONFIRMATION.equals(entity.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该 AI 动作当前不能取消");
        }
        entity.setStatus(CANCELLED);
        updateOrConflict(entity);
        return response(entity);
    }
    /**
     * Failure compensation entry: rebuild a fresh plan from the original, audited parameters.
     * The normal prepare path runs again so permissions, target state and target version are
     * re-read instead of blindly replaying a stale write.
     */
    public AiActionResponse retry(Long userId, String roleCode, Long actionId, String traceId) {
        AiActionEntity source = refreshExpiry(requireOwned(userId, roleCode, actionId));
        if (!Set.of(FAILED, CANCELLED, EXPIRED).contains(source.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "仅失败、取消或过期的 AI 动作可以重新规划");
        }
        return plan(new AiActionPlanRequest(
                userId,
                roleCode,
                source.getCapabilityId(),
                "retry:" + source.getId() + ":" + clock.millis(),
                source.getParametersJson()), traceId);
    }


    private PlanDraft prepare(AiActionPlanRequest request) {
        return switch (request.capabilityId()) {
            case TERM_WINDOW_UPSERT -> prepareTermWindow(request);
            case ASSIGNMENT_PUBLISH -> prepareAssignmentPublish(request);
            case SUBMISSION_GRADE -> prepareSubmissionGrade(request);
            case TEACHER_REGISTRATION_REVIEW -> prepareTeacherRegistrationReview(request);
            default -> throw new BusinessException(
                    CommonErrorCode.OPERATION_NOT_ALLOWED, "该 AI 能力尚未接入通用动作执行器");
        };
    }

    private PlanDraft prepareTermWindow(AiActionPlanRequest request) {
        requireRole(request.roleCode(), Set.of("ADMIN", "SUPER_ADMIN"));
        UpsertTermEnrollmentWindowRequest parameters = read(
                request.parametersJson(), UpsertTermEnrollmentWindowRequest.class);
        validate(parameters);
        String term = parameters.term().trim();
        TermEnrollmentWindowVO current = termWindowService.list().stream()
                .filter(item -> term.equals(item.term()))
                .findFirst()
                .orElse(null);
        Map<String, String> preview = new LinkedHashMap<>();
        preview.put("学期", term);
        preview.put("选课开始", text(parameters.enrollmentOpenAt()));
        preview.put("选课截止", text(parameters.enrollmentCloseAt()));
        if (current != null) {
            preview.put("当前设置", text(current.enrollmentOpenAt()) + " 至 " + text(current.enrollmentCloseAt()));
        }
        return new PlanDraft(
                "HIGH", "STRONG_CONFIRM", "TERM_ENROLLMENT_WINDOW",
                current == null ? null : Long.valueOf(current.windowId()),
                current == null ? null : current.version(),
                "更新“" + term + "”选课时间",
                "确认后将更新该学期的统一选课窗口，影响采用学期默认设置的课程。",
                preview);
    }

    private PlanDraft prepareAssignmentPublish(AiActionPlanRequest request) {
        requireRole(request.roleCode(), Set.of("TEACHER"));
        AssignmentPublishParameters parameters = read(
                request.parametersJson(), AssignmentPublishParameters.class);
        validate(parameters);
        AssignmentDetailVO assignment = assignmentService.getForTeacher(request.userId(), parameters.assignmentId());
        if (!"DRAFT".equals(assignment.assignmentStatus().code())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "只有草稿作业可以计划发布");
        }
        Map<String, String> preview = new LinkedHashMap<>();
        preview.put("作业", assignment.title());
        preview.put("满分", assignment.maxScore().stripTrailingZeros().toPlainString());
        preview.put("开放时间", text(assignment.openAt()));
        preview.put("截止时间", text(assignment.dueAt()));
        return new PlanDraft(
                "MEDIUM", "EXPLICIT_CONFIRM", "ASSIGNMENT", parameters.assignmentId(), assignment.version(),
                "发布作业《" + assignment.title() + "》",
                "确认后作业将对课程学生可见，并按现有业务规则发送通知。",
                preview);
    }

    private PlanDraft prepareSubmissionGrade(AiActionPlanRequest request) {
        requireRole(request.roleCode(), Set.of("TEACHER"));
        SubmissionGradeParameters parameters = read(request.parametersJson(), SubmissionGradeParameters.class);
        validate(parameters);
        TeacherSubmissionGradeVO submission =
                gradeService.getSubmissionForTeacher(request.userId(), parameters.submissionId());
        if (!("SUBMITTED".equals(submission.submissionStatus().code())
                || "GRADED".equals(submission.submissionStatus().code()))) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该提交当前不能评分");
        }
        if (parameters.score().compareTo(java.math.BigDecimal.ZERO) < 0
                || parameters.score().compareTo(submission.maxScore()) > 0) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "评分必须在 0 到满分之间");
        }
        Map<String, String> preview = new LinkedHashMap<>();
        preview.put("学生", submission.studentName());
        preview.put("提交ID", submission.submissionId());
        preview.put("分数", parameters.score().stripTrailingZeros().toPlainString()
                + " / " + submission.maxScore().stripTrailingZeros().toPlainString());
        preview.put("评语", text(parameters.teacherComment()));
        preview.put("结果", parameters.publishNow() ? "保存并立即向学生发布" : "仅保存为评分草稿");
        return new PlanDraft(
                "HIGH", parameters.publishNow() ? "STRONG_CONFIRM" : "EXPLICIT_CONFIRM",
                "ASSIGNMENT_SUBMISSION", parameters.submissionId(),
                submission.version(), "确认评分：" + submission.studentName(),
                "确认后将按预览保存评分；选择立即发布时学生会收到正式成绩通知。", preview);
    }

    private PlanDraft prepareTeacherRegistrationReview(AiActionPlanRequest request) {
        requireRole(request.roleCode(), Set.of("ADMIN", "SUPER_ADMIN"));
        TeacherReviewParameters parameters = read(request.parametersJson(), TeacherReviewParameters.class);
        validate(parameters);
        String decision = parameters.decision().trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("APPROVE", "REJECT").contains(decision)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "审核决策只能是 APPROVE 或 REJECT");
        }
        AdminUserVO candidate = adminUserService.getPendingTeacherRegistration(
                request.userId(), parameters.userId());
        Map<String, String> preview = new LinkedHashMap<>();
        preview.put("申请人", candidate.displayName());
        preview.put("用户名", candidate.username());
        preview.put("用户ID", candidate.userId());
        preview.put("当前状态", candidate.userStatus());
        preview.put("审核决定", "APPROVE".equals(decision) ? "通过教师注册" : "驳回教师注册");
        return new PlanDraft(
                "HIGH", "STRONG_CONFIRM", "USER", parameters.userId(), candidate.version(),
                ("APPROVE".equals(decision) ? "通过" : "驳回") + "教师申请：" + candidate.displayName(),
                "该治理动作会改变账号状态，必须由当前超级管理员明确确认。", preview);
    }

    private ExecutionResult execute(AiActionEntity entity) {
        return switch (entity.getCapabilityId()) {
            case TERM_WINDOW_UPSERT -> executeTermWindow(entity);
            case ASSIGNMENT_PUBLISH -> executeAssignmentPublish(entity);
            case SUBMISSION_GRADE -> executeSubmissionGrade(entity);
            case TEACHER_REGISTRATION_REVIEW -> executeTeacherRegistrationReview(entity);
            default -> throw new BusinessException(
                    CommonErrorCode.OPERATION_NOT_ALLOWED, "该 AI 能力尚未接入通用动作执行器");
        };
    }

    private ExecutionResult executeTermWindow(AiActionEntity entity) {
        UpsertTermEnrollmentWindowRequest parameters = read(
                entity.getParametersJson(), UpsertTermEnrollmentWindowRequest.class);
        String term = parameters.term().trim();
        TermEnrollmentWindowVO current = termWindowService.list().stream()
                .filter(item -> term.equals(item.term()))
                .findFirst()
                .orElse(null);
        requireTargetUnchanged(entity, current == null ? null : Long.valueOf(current.windowId()),
                current == null ? null : current.version());
        TermEnrollmentWindowVO result = termWindowService.upsert(parameters);
        return new ExecutionResult(
                "TERM_ENROLLMENT_WINDOW", Long.valueOf(result.windowId()), "/admin/term-enrollment",
                Map.of("term", result.term(), "version", String.valueOf(result.version())));
    }

    private ExecutionResult executeAssignmentPublish(AiActionEntity entity) {
        AssignmentPublishParameters parameters = read(
                entity.getParametersJson(), AssignmentPublishParameters.class);
        AssignmentDetailVO current = assignmentService.getForTeacher(entity.getRequesterId(), parameters.assignmentId());
        requireTargetUnchanged(entity, parameters.assignmentId(), current.version());
        AssignmentDetailVO result = assignmentService.publish(entity.getRequesterId(), parameters.assignmentId());
        return new ExecutionResult(
                "ASSIGNMENT", parameters.assignmentId(),
                "/teacher/assignments?assignmentId=" + parameters.assignmentId(),
                Map.of("title", result.title(), "status", result.assignmentStatus().code()));
    }

    private ExecutionResult executeSubmissionGrade(AiActionEntity entity) {
        SubmissionGradeParameters parameters = read(entity.getParametersJson(), SubmissionGradeParameters.class);
        TeacherSubmissionGradeVO current =
                gradeService.getSubmissionForTeacher(entity.getRequesterId(), parameters.submissionId());
        requireTargetUnchanged(entity, parameters.submissionId(), current.version());
        TeacherSubmissionGradeVO result = gradeService.gradeSubmission(
                entity.getRequesterId(),
                parameters.submissionId(),
                new GradeSubmissionRequest(
                        parameters.score(), current.maxScore(), parameters.teacherComment(),
                        parameters.aiCommentDraftId(), parameters.publishNow(), current.version()));
        return new ExecutionResult(
                "ASSIGNMENT_SUBMISSION", parameters.submissionId(),
                "/teacher/assignments?assignmentId=" + result.assignmentId(),
                Map.of(
                        "score", result.score().stripTrailingZeros().toPlainString(),
                        "status", result.gradeStatus() == null ? "DRAFT" : result.gradeStatus().code()));
    }

    private ExecutionResult executeTeacherRegistrationReview(AiActionEntity entity) {
        TeacherReviewParameters parameters = read(entity.getParametersJson(), TeacherReviewParameters.class);
        AdminUserVO current = adminUserService.getPendingTeacherRegistration(
                entity.getRequesterId(), parameters.userId());
        requireTargetUnchanged(entity, parameters.userId(), current.version());
        String decision = parameters.decision().trim().toUpperCase(java.util.Locale.ROOT);
        AdminUserVO result = "APPROVE".equals(decision)
                ? adminUserService.approveTeacherRegistration(entity.getRequesterId(), parameters.userId())
                : adminUserService.rejectTeacherRegistration(entity.getRequesterId(), parameters.userId());
        return new ExecutionResult(
                "USER", parameters.userId(), "/admin/users",
                Map.of("username", result.username(), "status", result.userStatus(), "decision", decision));
    }

    private void requireTargetUnchanged(AiActionEntity entity, Long targetId, Integer targetVersion) {
        if (!java.util.Objects.equals(entity.getTargetId(), targetId)
                || !java.util.Objects.equals(entity.getTargetVersion(), targetVersion)) {
            throw new BusinessException(
                    CommonErrorCode.RESOURCE_CONFLICT, "目标对象在确认前已发生变化，请重新生成动作计划");
        }
    }

    private AiActionEntity requireOwned(Long userId, String roleCode, Long actionId) {
        AiActionEntity entity = mapper.selectById(actionId);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "AI 动作不存在或已不可访问");
        }
        if (!userId.equals(entity.getRequesterId()) || !roleCode.equals(entity.getRoleCode())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "不能访问其他账号或角色创建的 AI 动作");
        }
        return entity;
    }

    private AiActionEntity refreshExpiry(AiActionEntity entity) {
        if (WAITING_CONFIRMATION.equals(entity.getStatus())
                && entity.getExpiresAt() != null
                && !entity.getExpiresAt().isAfter(now())) {
            entity.setStatus(EXPIRED);
            entity.setErrorCode("ACTION_EXPIRED");
            entity.setErrorMessage("动作计划已过期，请重新规划后再确认");
            updateOrConflict(entity);
        }
        return entity;
    }


    private AiActionEntity findByIdempotencyKey(String idempotencyKey) {
        return mapper.selectOne(Wrappers.<AiActionEntity>lambdaQuery()
                .eq(AiActionEntity::getIdempotencyKey, idempotencyKey));
    }

    private void requireSamePlan(AiActionEntity entity, AiActionPlanRequest request) {
        if (!request.userId().equals(entity.getRequesterId())
                || !request.roleCode().equals(entity.getRoleCode())
                || !request.capabilityId().equals(entity.getCapabilityId())
                || !request.parametersJson().equals(entity.getParametersJson())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "幂等键已用于其他 AI 动作计划");
        }
    }

    private void updateOrConflict(AiActionEntity entity) {
        if (mapper.updateById(entity) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "AI 动作状态已发生变化，请刷新后重试");
        }
    }

    private void requireRole(String actual, Set<String> allowed) {
        if (!allowed.contains(actual)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "当前角色不能创建该 AI 动作");
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "AI 动作参数无法解析");
        }
    }

    private void validate(Object value) {
        Set<ConstraintViolation<Object>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .findFirst()
                    .orElse("AI 动作参数不正确");
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, message);
        }
    }

    private AiActionResponse response(AiActionEntity entity) {
        return new AiActionResponse(
                String.valueOf(entity.getId()), entity.getCapabilityId(), entity.getStatus(),
                entity.getRiskLevel(), entity.getConfirmationPolicy(), entity.getTargetType(),
                id(entity.getTargetId()), entity.getTargetVersion(), entity.getTitle(), entity.getSummary(),
                preview(entity.getPreviewJson()), entity.getResourceType(), id(entity.getResourceId()),
                entity.getResourceHref(), WAITING_CONFIRMATION.equals(entity.getStatus()),
                entity.getErrorCode(), entity.getErrorMessage(), time(entity.getExpiresAt()),
                time(entity.getConfirmedAt()), time(entity.getExecutedAt()), time(entity.getCreatedAt()));
    }

    private Map<String, String> preview(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "AI 动作数据序列化失败");
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "动作执行失败，请稍后重试";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? "未设置" : String.valueOf(value);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record AssignmentPublishParameters(@jakarta.validation.constraints.NotNull Long assignmentId) {}

    private record SubmissionGradeParameters(
            @jakarta.validation.constraints.NotNull Long submissionId,
            @jakarta.validation.constraints.NotNull java.math.BigDecimal score,
            @jakarta.validation.constraints.Size(max = 1000) String teacherComment,
            Long aiCommentDraftId,
            boolean publishNow) {}

    private record TeacherReviewParameters(
            @jakarta.validation.constraints.NotNull Long userId,
            @jakarta.validation.constraints.NotBlank String decision) {}

    private record PlanDraft(
            String riskLevel,
            String confirmationPolicy,
            String targetType,
            Long targetId,
            Integer targetVersion,
            String title,
            String summary,
            Map<String, String> preview) {}

    private record ExecutionResult(
            String resourceType,
            Long resourceId,
            String href,
            Map<String, String> result) {}
}
