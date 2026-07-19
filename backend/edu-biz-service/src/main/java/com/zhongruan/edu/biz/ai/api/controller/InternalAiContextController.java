package com.zhongruan.edu.biz.ai.api.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionMapper;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.TermEnrollmentWindowEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.TermEnrollmentWindowMapper;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.biz.storage.application.service.MaterialTextExtractionService;
import com.zhongruan.edu.biz.storage.application.service.MaterialTextExtractionService.ExtractedText;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.LearningWarningEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.WarningEvidenceEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.mapper.LearningWarningMapper;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.mapper.WarningEvidenceMapper;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiAssistantContextRequest;
import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import com.zhongruan.edu.feign.ai.AiCourseContextRequest;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.AiPaperContextRequest;
import com.zhongruan.edu.feign.ai.AiPaperContextResponse;
import com.zhongruan.edu.feign.ai.AiQuestionRef;
import com.zhongruan.edu.feign.ai.AiResourceContextRequest;
import com.zhongruan.edu.feign.ai.AiSubmissionContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningContextResponse;
import com.zhongruan.edu.feign.ai.AiWarningEvidenceRef;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BizAiContextFeignClient.BASE_PATH)
public class InternalAiContextController implements BizAiContextFeignClient {
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String PUBLISHED = "PUBLISHED";

    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CourseLessonMapper courseLessonMapper;
    private final CourseMaterialMapper courseMaterialMapper;
    private final CoursePermissionService coursePermissionService;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper assignmentSubmissionMapper;
    private final LearningWarningMapper learningWarningMapper;
    private final WarningEvidenceMapper warningEvidenceMapper;
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;
    private final TermEnrollmentWindowMapper termEnrollmentWindowMapper;
    private final MaterialTextExtractionService extractionService;
    private final HttpServletRequest servletRequest;

    public InternalAiContextController(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            CourseLessonMapper courseLessonMapper,
            CourseMaterialMapper courseMaterialMapper,
            CoursePermissionService coursePermissionService,
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper assignmentSubmissionMapper,
            LearningWarningMapper learningWarningMapper,
            WarningEvidenceMapper warningEvidenceMapper,
            QuestionMapper questionMapper,
            ExamMapper examMapper,
            TermEnrollmentWindowMapper termEnrollmentWindowMapper,
            MaterialTextExtractionService extractionService,
            HttpServletRequest servletRequest) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.courseLessonMapper = courseLessonMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.coursePermissionService = coursePermissionService;
        this.assignmentMapper = assignmentMapper;
        this.assignmentSubmissionMapper = assignmentSubmissionMapper;
        this.learningWarningMapper = learningWarningMapper;
        this.warningEvidenceMapper = warningEvidenceMapper;
        this.questionMapper = questionMapper;
        this.examMapper = examMapper;
        this.termEnrollmentWindowMapper = termEnrollmentWindowMapper;
        this.extractionService = extractionService;
        this.servletRequest = servletRequest;
    }

    @Override
    public ApiResponse<AiAssistantContextResponse> getAssistantContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAssistantContextRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        boolean student = STUDENT_ROLE.equals(user.activeRole());
        boolean teacher = TEACHER_ROLE.equals(user.activeRole());
        boolean admin = ADMIN_ROLES.contains(user.activeRole());
        if (!student && !teacher && !admin) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        List<Long> courseIds;
        List<CourseEntity> courses;
        if (student) {
            courseIds = courseEnrollmentMapper.selectList(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                            .eq(CourseEnrollmentEntity::getStudentId, user.userId())
                            .in(CourseEnrollmentEntity::getStatus,
                                    EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name())
                            .orderByDesc(CourseEnrollmentEntity::getEnrolledAt))
                    .stream().map(CourseEnrollmentEntity::getCourseId).distinct().toList();
            courses = courses(courseIds);
        } else if (teacher) {
            courseIds = courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                            .eq(CourseTeacherEntity::getTeacherId, user.userId())
                            .eq(CourseTeacherEntity::getStatus, "ACTIVE")
                            .orderByDesc(CourseTeacherEntity::getUpdatedAt))
                    .stream().map(CourseTeacherEntity::getCourseId).distinct().toList();
            courses = courses(courseIds);
        } else {
            courses = courseMapper.selectList(Wrappers.<CourseEntity>lambdaQuery()
                    .orderByDesc(CourseEntity::getUpdatedAt).last("LIMIT 100"));
            courseIds = courses.stream().map(CourseEntity::getId).toList();
        }
        Map<Long, CourseEntity> courseById = courses.stream()
                .collect(Collectors.toMap(CourseEntity::getId, Function.identity(), (left, right) -> left));

        List<LearningWarningEntity> warnings = student
                ? learningWarningMapper.selectList(Wrappers.<LearningWarningEntity>lambdaQuery()
                        .eq(LearningWarningEntity::getStudentId, user.userId())
                        .orderByDesc(LearningWarningEntity::getGeneratedAt).last("LIMIT 50"))
                : teacher && !courseIds.isEmpty()
                        ? learningWarningMapper.selectList(Wrappers.<LearningWarningEntity>lambdaQuery()
                                .in(LearningWarningEntity::getCourseId, courseIds)
                                .orderByDesc(LearningWarningEntity::getGeneratedAt).last("LIMIT 50"))
                        : List.of();
        List<AssignmentEntity> assignments = courseIds.isEmpty() || admin
                ? List.of()
                : assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                        .in(AssignmentEntity::getCourseId, courseIds)
                        .eq(student, AssignmentEntity::getStatus, PUBLISHED)
                        .orderByAsc(AssignmentEntity::getDueAt).last("LIMIT 50"));
        List<ExamEntity> exams = courseIds.isEmpty() || admin
                ? List.of()
                : examMapper.selectList(Wrappers.<ExamEntity>lambdaQuery()
                        .in(ExamEntity::getCourseId, courseIds)
                        .eq(student, ExamEntity::getStatus, PUBLISHED)
                        .orderByAsc(ExamEntity::getStartAt).last("LIMIT 50"));
        List<String> windows = termEnrollmentWindowMapper
                .selectList(Wrappers.<TermEnrollmentWindowEntity>lambdaQuery()
                        .orderByAsc(TermEnrollmentWindowEntity::getEnrollmentOpenAt))
                .stream()
                .map(window -> "学期 %s：开放 %s，截止 %s".formatted(
                        value(window.getTerm()), value(window.getEnrollmentOpenAt()), value(window.getEnrollmentCloseAt())))
                .toList();
        List<String> courseFacts = courses.stream()
                .map(course -> "%s（课程代码 %s，课程ID %s，学期 %s，状态 %s）".formatted(
                        value(course.getName()), value(course.getCourseCode()), course.getId(),
                        value(course.getTerm()), value(course.getStatus())))
                .toList();
        List<String> warningFacts = warnings.stream()
                .map(warning -> "%s：级别 %s，状态 %s，%s；建议：%s%s".formatted(
                        courseLabel(courseById, warning.getCourseId()), value(warning.getWarningLevel()),
                        value(warning.getWarningStatus()), value(warning.getSummary()), value(warning.getSuggestion()),
                        teacher ? "；学生ID " + warning.getStudentId() : ""))
                .toList();
        List<String> assignmentFacts = assignments.stream()
                .map(assignment -> "%s：%s，状态 %s，开放 %s，截止 %s，满分 %s".formatted(
                        courseLabel(courseById, assignment.getCourseId()), value(assignment.getTitle()),
                        value(assignment.getStatus()), value(assignment.getOpenAt()),
                        value(assignment.getDueAt()), value(assignment.getMaxScore())))
                .toList();
        List<String> examFacts = exams.stream()
                .map(exam -> "%s：%s，状态 %s，开始 %s，结束 %s，时长 %s 分钟".formatted(
                        courseLabel(courseById, exam.getCourseId()), value(exam.getTitle()), value(exam.getStatus()),
                        value(exam.getStartAt()), value(exam.getEndAt()), value(exam.getDurationMinutes())))
                .toList();
        List<String> metrics = admin ? List.of(
                "课程总数：" + courseMapper.selectCount(Wrappers.<CourseEntity>lambdaQuery()),
                "开放学习预警：" + learningWarningMapper.selectCount(Wrappers.<LearningWarningEntity>lambdaQuery()
                        .eq(LearningWarningEntity::getWarningStatus, "OPEN")),
                "作业总数：" + assignmentMapper.selectCount(Wrappers.<AssignmentEntity>lambdaQuery()),
                "考试总数：" + examMapper.selectCount(Wrappers.<ExamEntity>lambdaQuery())) : List.of();

        return ApiResponse.success(new AiAssistantContextResponse(
                user.userId(), user.username(), user.activeRole(), OffsetDateTime.now(ZoneOffset.UTC),
                windows, courseFacts, warningFacts, assignmentFacts, examFacts, metrics),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiCourseContextResponse> getCourseContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiCourseContextRequest request) {
        AuthenticatedUser user = authenticatedUser();
        if (!user.userId().equals(request.userId()) || !user.activeRole().equals(request.roleCode())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "AI 上下文身份与访问令牌不一致");
        }
        CourseEntity course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        boolean teacherMember = isTeacherMember(user.userId(), request.courseId());
        boolean enrolled = isEnrolled(user.userId(), request.courseId());
        boolean admin = ADMIN_ROLES.contains(user.activeRole());

        if (!admin && !teacherMember && !enrolled) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (STUDENT_ROLE.equals(user.activeRole())
                && !coursePermissionService.canViewCourseAsStudent(user.userId(), request.courseId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (TEACHER_ROLE.equals(user.activeRole()) && !teacherMember) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        boolean includeDraft = admin || teacherMember;
        if (request.lessonId() != null && courseLessonMapper.selectCount(
                        Wrappers.<CourseLessonEntity>lambdaQuery()
                                .eq(CourseLessonEntity::getId, request.lessonId())
                                .eq(CourseLessonEntity::getCourseId, request.courseId())) == 0) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课时不属于当前课程");
        }
        if (request.lessonId() != null
                && !includeDraft
                && !coursePermissionService.canAccessLesson(user.userId(), request.lessonId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "当前用户不能访问该课时");
        }
        if (request.materialId() != null && courseMaterialMapper.selectCount(
                        Wrappers.<CourseMaterialEntity>lambdaQuery()
                                .eq(CourseMaterialEntity::getId, request.materialId())
                                .eq(CourseMaterialEntity::getCourseId, request.courseId())) == 0) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "资料不属于当前课程");
        }
        if (request.materialId() != null
                && !includeDraft
                && !coursePermissionService.canAccessMaterial(user.userId(), request.materialId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "当前用户不能访问该资料");
        }

        AiCourseContextResponse response = new AiCourseContextResponse(
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                course.getStatus(),
                course.getReviewStatus(),
                course.getOwnerTeacherId(),
                teacherMember,
                enrolled,
                lessons(request.courseId(), includeDraft, user.userId()),
                materials(request.courseId(), includeDraft, user.userId()));

        return ApiResponse.success(response, RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiSubmissionContextResponse> getSubmissionContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiResourceContextRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        requirePurpose(request.purpose(), com.zhongruan.edu.feign.ai.AiContextPurpose.GRADING_COMMENT_DRAFT);
        AssignmentSubmissionEntity submission = assignmentSubmissionMapper.selectById(request.resourceId());
        if (submission == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        requireCourseTeacher(user, submission.getCourseId());
        AssignmentEntity assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null || !submission.getCourseId().equals(assignment.getCourseId())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        return ApiResponse.success(
                new AiSubmissionContextResponse(
                        submission.getCourseId(),
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getDescription(),
                        assignment.getMaxScore(),
                        submission.getId(),
                        submission.getContent(),
                        submission.getScore()),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiWarningContextResponse> getWarningContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiResourceContextRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        requirePurpose(request.purpose(), com.zhongruan.edu.feign.ai.AiContextPurpose.RISK_EXPLANATION);
        LearningWarningEntity warning = learningWarningMapper.selectById(request.resourceId());
        if (warning == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        requireCourseTeacher(user, warning.getCourseId());
        List<AiWarningEvidenceRef> evidences = warningEvidenceMapper
                .selectList(Wrappers.<WarningEvidenceEntity>lambdaQuery()
                        .eq(WarningEvidenceEntity::getWarningId, warning.getId())
                        .orderByAsc(WarningEvidenceEntity::getId))
                .stream()
                .map(evidence -> new AiWarningEvidenceRef(
                        evidence.getId(),
                        evidence.getEvidenceType(),
                        evidence.getSourceId(),
                        evidence.getMetricCode(),
                        evidence.getMetricValue(),
                        evidence.getDescription()))
                .toList();
        return ApiResponse.success(
                new AiWarningContextResponse(
                        warning.getCourseId(),
                        warning.getId(),
                        warning.getWarningType(),
                        warning.getWarningLevel(),
                        warning.getSummary(),
                        warning.getSuggestion(),
                        evidences),
                RequestTrace.from(servletRequest));
    }

    @Override
    public ApiResponse<AiPaperContextResponse> getPaperContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiPaperContextRequest request) {
        AuthenticatedUser user = requireIdentity(request.userId(), request.roleCode());
        requirePurpose(request.purpose(), com.zhongruan.edu.feign.ai.AiContextPurpose.PAPER_SUGGESTION);
        CourseEntity course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        requireCourseTeacher(user, course.getId());
        List<AiQuestionRef> questions = questionMapper
                .selectList(Wrappers.<QuestionEntity>lambdaQuery()
                        .eq(QuestionEntity::getCourseId, course.getId())
                        .eq(QuestionEntity::getStatus, "ACTIVE")
                        .orderByAsc(QuestionEntity::getId))
                .stream()
                .map(question -> new AiQuestionRef(
                        question.getId(),
                        question.getBankId(),
                        question.getQuestionType(),
                        question.getStem(),
                        question.getDifficulty(),
                        question.getScore()))
                .toList();
        return ApiResponse.success(
                new AiPaperContextResponse(course.getId(), course.getCourseCode(), course.getName(), questions),
                RequestTrace.from(servletRequest));
    }

    private List<CourseEntity> courses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return courseMapper.selectList(Wrappers.<CourseEntity>lambdaQuery()
                .in(CourseEntity::getId, courseIds)
                .orderByDesc(CourseEntity::getUpdatedAt));
    }

    private String courseLabel(Map<Long, CourseEntity> courseById, Long courseId) {
        CourseEntity course = courseById.get(courseId);
        return course == null ? "课程ID " + courseId : course.getName() + "（" + course.getCourseCode() + "）";
    }

    private String value(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "未设置" : String.valueOf(value);
    }

    private boolean isTeacherMember(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return false;
        }
        Long count = courseTeacherMapper.selectCount(Wrappers.<CourseTeacherEntity>lambdaQuery()
                .eq(CourseTeacherEntity::getTeacherId, userId)
                .eq(CourseTeacherEntity::getCourseId, courseId));
        return count != null && count > 0;
    }

    private boolean isEnrolled(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return false;
        }
        Long count = courseEnrollmentMapper.selectCount(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getStudentId, userId)
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .in(CourseEnrollmentEntity::getStatus,
                        EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()));
        return count != null && count > 0;
    }

    private List<AiLessonRef> lessons(Long courseId, boolean includeDraft, Long userId) {
        var query = Wrappers.<CourseLessonEntity>lambdaQuery()
                .eq(CourseLessonEntity::getCourseId, courseId)
                .orderByAsc(CourseLessonEntity::getChapterId)
                .orderByAsc(CourseLessonEntity::getSortOrder)
                .orderByAsc(CourseLessonEntity::getId);
        if (!includeDraft) {
            query.eq(CourseLessonEntity::getStatus, PUBLISHED);
        }
        return courseLessonMapper.selectList(query).stream()
                .filter(lesson -> includeDraft || coursePermissionService.canAccessLesson(userId, lesson.getId()))
                .map(lesson -> new AiLessonRef(
                        lesson.getId(),
                        lesson.getChapterId(),
                        lesson.getTitle(),
                        lesson.getStatus(),
                        lesson.getContentType(),
                        lesson.getContent(),
                        lesson.getEstimatedMinutes()))
                .toList();
    }

    private List<AiMaterialRef> materials(Long courseId, boolean includeDraft, Long userId) {
        var query = Wrappers.<CourseMaterialEntity>lambdaQuery()
                .eq(CourseMaterialEntity::getCourseId, courseId)
                .orderByAsc(CourseMaterialEntity::getSortOrder)
                .orderByAsc(CourseMaterialEntity::getId);
        if (!includeDraft) {
            query.eq(CourseMaterialEntity::getStatus, PUBLISHED);
        }
        return courseMaterialMapper.selectList(query).stream()
                .filter(material -> includeDraft || coursePermissionService.canAccessMaterial(userId, material.getId()))
                .map(this::toAiMaterialRef)
                .toList();
    }

    private AiMaterialRef toAiMaterialRef(CourseMaterialEntity material) {
        ExtractedText extracted = material.getFileId() == null
                ? new ExtractedText("", "NO_FILE", "外部链接或未关联平台文件", false)
                : extractionService.extract(material.getFileId());
        return new AiMaterialRef(
                material.getId(), material.getChapterId(), material.getLessonId(), material.getName(),
                material.getMaterialType(), material.getFileKey(), material.getFileUrl(), material.getVisibility(),
                material.getStatus(), extracted.text(), extracted.status(), extracted.message());
    }
    private AuthenticatedUser authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private AuthenticatedUser requireIdentity(Long userId, String roleCode) {
        AuthenticatedUser user = authenticatedUser();
        if (!user.userId().equals(userId) || !user.activeRole().equals(roleCode)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "AI 上下文身份与访问令牌不一致");
        }
        return user;
    }

    private void requireCourseTeacher(AuthenticatedUser user, Long courseId) {
        if (!TEACHER_ROLE.equals(user.activeRole()) || !isTeacherMember(user.userId(), courseId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void requirePurpose(
            com.zhongruan.edu.feign.ai.AiContextPurpose actual,
            com.zhongruan.edu.feign.ai.AiContextPurpose expected) {
        if (actual != expected) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "AI 上下文用途不匹配");
        }
    }
}
