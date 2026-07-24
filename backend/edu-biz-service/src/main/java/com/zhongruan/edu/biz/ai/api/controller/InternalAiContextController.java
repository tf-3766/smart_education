package com.zhongruan.edu.biz.ai.api.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserRoleMapper;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.LessonLearningRecordEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseChapterMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.LessonLearningRecordMapper;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.entity.GradeRecordEntity;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.mapper.GradeRecordMapper;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.AnnouncementEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.AnnouncementMapper;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumTopicEntity;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.mapper.ForumTopicMapper;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.entity.NotificationEntity;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.mapper.NotificationMapper;
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
import com.zhongruan.edu.feign.ai.AiChapterRef;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.AiPaperContextRequest;
import com.zhongruan.edu.feign.ai.AiPaperContextResponse;
import com.zhongruan.edu.feign.ai.AiQuestionRef;
import com.zhongruan.edu.feign.ai.AiResourceContextRequest;
import com.zhongruan.edu.feign.ai.AiSubmissionContextResponse;
import com.zhongruan.edu.feign.ai.AiTeacherRegistrationCandidate;
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
    private final CourseChapterMapper courseChapterMapper;
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
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final LessonLearningRecordMapper lessonLearningRecordMapper;
    private final GradeRecordMapper gradeRecordMapper;
    private final AnnouncementMapper announcementMapper;
    private final ForumTopicMapper forumTopicMapper;
    private final NotificationMapper notificationMapper;
    private final MaterialTextExtractionService extractionService;
    private final HttpServletRequest servletRequest;

    public InternalAiContextController(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            CourseChapterMapper courseChapterMapper,
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
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            LessonLearningRecordMapper lessonLearningRecordMapper,
            GradeRecordMapper gradeRecordMapper,
            AnnouncementMapper announcementMapper,
            ForumTopicMapper forumTopicMapper,
            NotificationMapper notificationMapper,
            MaterialTextExtractionService extractionService,
            HttpServletRequest servletRequest) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.courseChapterMapper = courseChapterMapper;
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
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.lessonLearningRecordMapper = lessonLearningRecordMapper;
        this.gradeRecordMapper = gradeRecordMapper;
        this.announcementMapper = announcementMapper;
        this.forumTopicMapper = forumTopicMapper;
        this.notificationMapper = notificationMapper;
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
        boolean superAdmin = "SUPER_ADMIN".equals(user.activeRole());
        Set<String> domains = request.domains();
        boolean allDomains = domains != null && domains.contains("ALL");
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

        List<LearningWarningEntity> warnings = !requested(domains, allDomains, "WARNINGS") ? List.of() : student
                ? learningWarningMapper.selectList(Wrappers.<LearningWarningEntity>lambdaQuery()
                        .eq(LearningWarningEntity::getStudentId, user.userId())
                        .orderByDesc(LearningWarningEntity::getGeneratedAt).last("LIMIT 50"))
                : teacher && !courseIds.isEmpty()
                        ? learningWarningMapper.selectList(Wrappers.<LearningWarningEntity>lambdaQuery()
                                .in(LearningWarningEntity::getCourseId, courseIds)
                                .orderByDesc(LearningWarningEntity::getGeneratedAt).last("LIMIT 50"))
                        : List.of();
        List<AssignmentEntity> assignments = !requested(domains, allDomains, "ASSIGNMENTS") || courseIds.isEmpty() || admin
                ? List.of()
                : assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                        .in(AssignmentEntity::getCourseId, courseIds)
                        .eq(student, AssignmentEntity::getStatus, PUBLISHED)
                        .orderByAsc(AssignmentEntity::getDueAt).last("LIMIT 50"));
        List<ExamEntity> exams = !requested(domains, allDomains, "EXAMS") || courseIds.isEmpty() || admin
                ? List.of()
                : examMapper.selectList(Wrappers.<ExamEntity>lambdaQuery()
                        .in(ExamEntity::getCourseId, courseIds)
                        .eq(student, ExamEntity::getStatus, PUBLISHED)
                        .orderByAsc(ExamEntity::getStartAt).last("LIMIT 50"));
        List<String> windows = !requested(domains, allDomains, "WINDOWS") ? List.of() : termEnrollmentWindowMapper
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
                .map(warning -> "%s：预警ID %s，级别 %s，状态 %s，%s；建议：%s%s".formatted(
                        courseLabel(courseById, warning.getCourseId()), warning.getId(), value(warning.getWarningLevel()),
                        value(warning.getWarningStatus()), value(warning.getSummary()), value(warning.getSuggestion()),
                        teacher ? "；学生ID " + warning.getStudentId() : ""))
                .toList();
        List<String> assignmentFacts = assignments.stream()
                .map(assignment -> "%s：%s（作业ID %s），状态 %s，开放 %s，截止 %s，满分 %s".formatted(
                        courseLabel(courseById, assignment.getCourseId()), value(assignment.getTitle()),
                        assignment.getId(),
                        value(assignment.getStatus()), value(assignment.getOpenAt()),
                        value(assignment.getDueAt()), value(assignment.getMaxScore())))
                .toList();
        List<String> examFacts = exams.stream()
                .map(exam -> "%s：%s，状态 %s，开始 %s，结束 %s，时长 %s 分钟".formatted(
                        courseLabel(courseById, exam.getCourseId()), value(exam.getTitle()), value(exam.getStatus()),
                        value(exam.getStartAt()), value(exam.getEndAt()), value(exam.getDurationMinutes())))
                .toList();
        List<String> metrics = admin && requested(domains, allDomains, "METRICS") ? List.of(
                "课程总数：" + courseMapper.selectCount(Wrappers.<CourseEntity>lambdaQuery()),
                "开放学习预警：" + learningWarningMapper.selectCount(Wrappers.<LearningWarningEntity>lambdaQuery()
                        .eq(LearningWarningEntity::getWarningStatus, "OPEN")),
                "作业总数：" + assignmentMapper.selectCount(Wrappers.<AssignmentEntity>lambdaQuery()),
                "考试总数：" + examMapper.selectCount(Wrappers.<ExamEntity>lambdaQuery()),
                "指标口径说明：上述数量直接实时统计业务表；管理员上下文中明细未提供不代表底层表为空，"
                        + "不得据此判断指标失真。",
                "课程业务口径：同一课程代码跨教师或学期多次开课属于正常业务；"
                        + "DRAFT 未进入审核、OFFLINE 计入课程总数均不构成异常。",
                "异常信号口径：当前上下文未提供经规则引擎确认的异常信号；"
                        + "不得仅凭数量、状态分布、名称重复或缺少未授权明细推断异常。") : List.of();
        List<UserEntity> pendingTeacherEntities = superAdmin && requested(domains, allDomains, "USERS")
                ? userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUserStatus, "PENDING")
                        .orderByAsc(UserEntity::getCreatedAt)
                        .last("LIMIT 100"))
                : List.of();
        List<String> pendingTeachers = pendingTeacherEntities.stream()
                .map(candidate -> "%s（用户名 %s，用户ID %s，版本 %s）".formatted(
                        value(candidate.getDisplayName()), value(candidate.getUsername()),
                        candidate.getId(), candidate.getVersion()))
                .toList();
        List<AiTeacherRegistrationCandidate> pendingTeacherCandidates = pendingTeacherEntities.stream()
                .map(candidate -> new AiTeacherRegistrationCandidate(
                        candidate.getId(), candidate.getUsername(), candidate.getDisplayName(),
                        candidate.getVersion(), offset(candidate.getCreatedAt())))
                .toList();
        List<String> submissionFacts = teacher && !courseIds.isEmpty() && requested(domains, allDomains, "SUBMISSIONS")
                ? assignmentSubmissionMapper.selectList(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                        .in(AssignmentSubmissionEntity::getCourseId, courseIds)
                        .in(AssignmentSubmissionEntity::getStatus, "SUBMITTED", "GRADED")
                        .orderByDesc(AssignmentSubmissionEntity::getSubmittedAt)
                        .last("LIMIT 100"))
                    .stream()
                    .map(submission -> "提交ID %s：作业ID %s，课程ID %s，学生ID %s，状态 %s，当前分数 %s，版本 %s".formatted(
                            submission.getId(), submission.getAssignmentId(), submission.getCourseId(),
                            submission.getStudentId(), value(submission.getStatus()), value(submission.getScore()),
                            submission.getVersion()))
                    .toList()
                : List.of();

        List<String> progressFacts = (student || teacher) && !courseIds.isEmpty()
                && requested(domains, allDomains, "PROGRESS")
                ? lessonLearningRecordMapper.selectList(Wrappers.<LessonLearningRecordEntity>lambdaQuery()
                        .in(LessonLearningRecordEntity::getCourseId, courseIds)
                        .eq(student, LessonLearningRecordEntity::getStudentId, user.userId())
                        .orderByDesc(LessonLearningRecordEntity::getLastStudiedAt)
                        .last("LIMIT 100"))
                    .stream()
                    .map(item -> "%s：课时ID %s，状态 %s，学习 %s 秒，最近学习 %s%s".formatted(
                            courseLabel(courseById, item.getCourseId()), item.getLessonId(), value(item.getStatus()),
                            value(item.getStudySeconds()), value(item.getLastStudiedAt()),
                            teacher ? "，学生ID " + item.getStudentId() : ""))
                    .toList()
                : List.of();
        List<String> gradeFacts = (student || teacher) && !courseIds.isEmpty()
                && requested(domains, allDomains, "GRADES")
                ? gradeRecordMapper.selectList(Wrappers.<GradeRecordEntity>lambdaQuery()
                        .in(GradeRecordEntity::getCourseId, courseIds)
                        .eq(student, GradeRecordEntity::getStudentId, user.userId())
                        .eq(student, GradeRecordEntity::getGradeStatus, PUBLISHED)
                        .orderByDesc(GradeRecordEntity::getPublishedAt)
                        .last("LIMIT 100"))
                    .stream()
                    .map(item -> "%s：%s ID %s，成绩 %s/%s，状态 %s，评语 %s%s".formatted(
                            courseLabel(courseById, item.getCourseId()), value(item.getSourceType()),
                            item.getSourceId(), value(item.getScore()), value(item.getMaxScore()),
                            value(item.getGradeStatus()), value(item.getComment()),
                            teacher ? "，学生ID " + item.getStudentId() : ""))
                    .toList()
                : List.of();
        List<AnnouncementEntity> visibleAnnouncements = requested(domains, allDomains, "ANNOUNCEMENTS")
                ? announcementMapper.selectList(Wrappers.<AnnouncementEntity>lambdaQuery()
                        .orderByDesc(AnnouncementEntity::getCreatedAt).last("LIMIT 100"))
                : List.of();
        List<String> announcementFacts = visibleAnnouncements.stream()
                .filter(item -> admin
                        || (student && PUBLISHED.equals(item.getStatus())
                                && Set.of("ALL", "STUDENT").contains(item.getAudience())
                                && (item.getCourseId() == null || courseIds.contains(item.getCourseId())))
                        || (teacher && ((item.getCourseId() != null && courseIds.contains(item.getCourseId()))
                                || (item.getCourseId() == null && PUBLISHED.equals(item.getStatus())
                                        && Set.of("ALL", "TEACHER").contains(item.getAudience())))))
                .map(item -> "%s公告《%s》：状态 %s，受众 %s，来源 %s，%s".formatted(
                        item.getCourseId() == null ? "平台" : courseLabel(courseById, item.getCourseId()),
                        value(item.getTitle()), value(item.getStatus()), value(item.getAudience()),
                        value(item.getSource()), value(item.getContent())))
                .toList();
        List<String> forumFacts = !requested(domains, allDomains, "FORUM") ? List.of() : forumTopicMapper.selectList(
                        Wrappers.<ForumTopicEntity>lambdaQuery().orderByDesc(ForumTopicEntity::getUpdatedAt).last("LIMIT 100"))
                .stream()
                .filter(item -> admin || (courseIds.contains(item.getCourseId())
                        && (!student || !"HIDDEN".equals(item.getStatus()))))
                .map(item -> "%s讨论《%s》：状态 %s，回复 %s，置顶 %s，%s".formatted(
                        courseLabel(courseById, item.getCourseId()), value(item.getTitle()),
                        value(item.getStatus()), value(item.getReplyCount()), value(item.getPinned()), value(item.getContent())))
                .toList();
        List<String> notificationFacts = !requested(domains, allDomains, "NOTIFICATIONS") ? List.of()
                : notificationMapper.selectList(Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getRecipientUserId, user.userId())
                        .orderByDesc(NotificationEntity::getCreatedAt).last("LIMIT 100"))
                .stream()
                .map(item -> "《%s》：分类 %s，状态 %s，来源 %s，%s".formatted(
                        value(item.getTitle()), value(item.getCategory()), value(item.getStatus()),
                        value(item.getSourceType()), value(item.getContent())))
                .toList();
        List<UserEntity> userEntities = superAdmin && requested(domains, allDomains, "USERS")
                ? userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .orderByDesc(UserEntity::getUpdatedAt).last("LIMIT 100"))
                : List.of();
        Map<Long, String> rolesByUserId = rolesByUserId(userEntities);
        List<String> userFacts = !userEntities.isEmpty()
                ? java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(userRoleSummary(userEntities, rolesByUserId)),
                    userEntities
                    .stream()
                    .map(item -> "%s（用户名 %s，用户ID %s，角色 %s，状态 %s，版本 %s）".formatted(
                            value(item.getDisplayName()), value(item.getUsername()), item.getId(),
                            rolesByUserId.getOrDefault(item.getId(), "未分配"),
                            value(item.getUserStatus()), item.getVersion())))
                    .toList()
                : List.of();

        return ApiResponse.success(new AiAssistantContextResponse(
                user.userId(), user.username(), user.activeRole(), OffsetDateTime.now(ZoneOffset.UTC),
                windows, courseFacts, warningFacts, assignmentFacts, examFacts, metrics,
                pendingTeachers, submissionFacts, pendingTeacherCandidates, progressFacts, gradeFacts,
                announcementFacts, forumFacts, notificationFacts, userFacts),
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

        List<AiLessonRef> visibleLessons = lessons(request.courseId(), includeDraft, user.userId());
        List<AiMaterialRef> visibleMaterials = materials(request.courseId(), includeDraft, user.userId());
        AiCourseContextResponse response = new AiCourseContextResponse(
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                course.getStatus(),
                course.getReviewStatus(),
                course.getOwnerTeacherId(),
                teacherMember,
                enrolled,
                visibleLessons,
                visibleMaterials,
                course.getSummary(), course.getCategoryId(), course.getTerm(), course.getDepartment(),
                course.getCredit(), offset(course.getEnrollmentOpenAt()), offset(course.getEnrollmentCloseAt()),
                offset(course.getStartAt()), offset(course.getEndAt()), course.getVersion(),
                chapters(request.courseId(), includeDraft, visibleLessons, visibleMaterials));

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

    private Map<Long, String> rolesByUserId(List<UserEntity> users) {
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> roleCodesById = roleMapper.selectList(Wrappers.<RoleEntity>lambdaQuery()
                        .eq(RoleEntity::getEnabled, 1))
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getRoleCode));
        Set<Long> userIds = users.stream().map(UserEntity::getId).collect(Collectors.toSet());
        return userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .in(UserRoleEntity::getUserId, userIds))
                .stream()
                .map(relation -> Map.entry(relation.getUserId(), roleCodesById.get(relation.getRoleId())))
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.joining("/"))));
    }

    private String userRoleSummary(List<UserEntity> users, Map<Long, String> rolesByUserId) {
        long studentCount = users.stream()
                .filter(user -> hasRole(rolesByUserId.get(user.getId()), "STUDENT"))
                .count();
        long teacherCount = users.stream()
                .filter(user -> hasRole(rolesByUserId.get(user.getId()), "TEACHER"))
                .count();
        long administratorCount = users.stream()
                .filter(user -> hasRole(rolesByUserId.get(user.getId()), "ADMIN")
                        || hasRole(rolesByUserId.get(user.getId()), "SUPER_ADMIN"))
                .count();
        long pendingTeacherCount = users.stream()
                .filter(user -> "PENDING".equals(user.getUserStatus()))
                .filter(user -> hasRole(rolesByUserId.get(user.getId()), "TEACHER"))
                .count();
        return "用户角色汇总：用户总数 %d；STUDENT %d；TEACHER %d；ADMIN/SUPER_ADMIN 用户 %d；待审核教师 %d。"
                .formatted(users.size(), studentCount, teacherCount, administratorCount, pendingTeacherCount);
    }

    private boolean hasRole(String roles, String expectedRole) {
        return roles != null && java.util.Arrays.stream(roles.split("/"))
                .anyMatch(expectedRole::equals);
    }

    private String value(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "未设置" : String.valueOf(value);
    }

    private boolean requested(Set<String> domains, boolean allDomains, String domain) {
        return allDomains || domains.contains(domain);
    }

    private OffsetDateTime offset(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
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

    private List<AiChapterRef> chapters(
            Long courseId, boolean includeDraft, List<AiLessonRef> visibleLessons, List<AiMaterialRef> visibleMaterials) {
        Set<Long> visibleChapterIds = java.util.stream.Stream.concat(
                        visibleLessons.stream().map(AiLessonRef::chapterId),
                        visibleMaterials.stream().map(AiMaterialRef::chapterId))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        var query = Wrappers.<CourseChapterEntity>lambdaQuery()
                .eq(CourseChapterEntity::getCourseId, courseId)
                .orderByAsc(CourseChapterEntity::getSortOrder)
                .orderByAsc(CourseChapterEntity::getId);
        if (!includeDraft) query.eq(CourseChapterEntity::getStatus, PUBLISHED);
        return courseChapterMapper.selectList(query).stream()
                .filter(chapter -> includeDraft || visibleChapterIds.contains(chapter.getId()))
                .map(chapter -> new AiChapterRef(
                        chapter.getId(), chapter.getTitle(), chapter.getDescription(),
                        chapter.getSortOrder(), chapter.getStatus()))
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
        ExtractedText extracted;
        try {
            extracted = material.getFileId() == null
                    ? new ExtractedText("", "NO_FILE", "外部链接或未关联平台文件", false)
                    : extractionService.extract(material.getFileId());
        } catch (RuntimeException exception) {
            extracted = new ExtractedText("", "FILE_UNAVAILABLE", "资料文件暂不可读取，请教师重新上传", false);
        }
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
