package com.zhongruan.edu.biz.warning.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.domain.enums.LearningStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.LessonLearningRecordEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseChapterMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.LessonLearningRecordMapper;
import com.zhongruan.edu.biz.grade.domain.enums.GradeSourceType;
import com.zhongruan.edu.biz.grade.domain.enums.GradeStatus;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.entity.GradeRecordEntity;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.mapper.GradeRecordMapper;
import com.zhongruan.edu.biz.notification.application.service.NotificationApplicationService;
import com.zhongruan.edu.biz.warning.api.dto.query.WarningListQuery;
import com.zhongruan.edu.biz.warning.api.dto.request.GenerateCourseWarningsRequest;
import com.zhongruan.edu.biz.warning.api.dto.request.WarningHandleRequest;
import com.zhongruan.edu.biz.warning.api.vo.LearningWarningVO;
import com.zhongruan.edu.biz.warning.api.vo.WarningGenerationResultVO;
import com.zhongruan.edu.biz.warning.application.assembler.WarningAssembler;
import com.zhongruan.edu.biz.warning.domain.WarningErrorCode;
import com.zhongruan.edu.biz.warning.domain.enums.WarningEvidenceType;
import com.zhongruan.edu.biz.warning.domain.enums.WarningLevel;
import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import com.zhongruan.edu.biz.warning.domain.enums.WarningType;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.LearningWarningEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.WarningEvidenceEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.mapper.LearningWarningMapper;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.mapper.WarningEvidenceMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarningApplicationService {
    private static final BigDecimal PROGRESS_THRESHOLD_PERCENT = new BigDecimal("50.00");
    private static final BigDecimal HIGH_PROGRESS_THRESHOLD_PERCENT = new BigDecimal("25.00");
    private static final BigDecimal LOW_SCORE_RATE = new BigDecimal("0.60");
    private static final BigDecimal HIGH_LOW_SCORE_RATE = new BigDecimal("0.40");

    private final LearningWarningMapper warningMapper;
    private final WarningEvidenceMapper evidenceMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseLessonMapper lessonMapper;
    private final LessonLearningRecordMapper learningRecordMapper;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final GradeRecordMapper gradeMapper;
    private final UserMapper userMapper;
    private final CourseManagementService courseManagementService;
    private final CoursePermissionService coursePermissionService;
    private final WarningAssembler assembler;
    private final NotificationApplicationService notificationService;
    private final Clock clock = Clock.systemUTC();

    public WarningApplicationService(
            LearningWarningMapper warningMapper,
            WarningEvidenceMapper evidenceMapper,
            CourseEnrollmentMapper enrollmentMapper,
            CourseChapterMapper chapterMapper,
            CourseLessonMapper lessonMapper,
            LessonLearningRecordMapper learningRecordMapper,
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            GradeRecordMapper gradeMapper,
            UserMapper userMapper,
            CourseManagementService courseManagementService,
            CoursePermissionService coursePermissionService,
            WarningAssembler assembler,
            NotificationApplicationService notificationService) {
        this.warningMapper = warningMapper;
        this.evidenceMapper = evidenceMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.gradeMapper = gradeMapper;
        this.userMapper = userMapper;
        this.courseManagementService = courseManagementService;
        this.coursePermissionService = coursePermissionService;
        this.assembler = assembler;
        this.notificationService = notificationService;
    }

    @Transactional
    public WarningGenerationResultVO generate(
            Long teacherId, Long courseId, GenerateCourseWarningsRequest request) {
        courseManagementService.requireEditor(teacherId, courseId);
        EnumSet<WarningType> types = requestedTypes(request.warningTypes());
        List<Long> studentIds = activeStudentIds(courseId, request.studentId());
        List<LearningWarningVO> warnings = new ArrayList<>();
        int skipped = 0;
        for (Long studentId : studentIds) {
            for (WarningCandidate candidate : candidates(courseId, studentId, types)) {
                if (hasOpenWarning(candidate)) {
                    skipped++;
                    continue;
                }
                warnings.add(Boolean.TRUE.equals(request.dryRun())
                        ? preview(candidate)
                        : save(candidate));
            }
        }
        return new WarningGenerationResultVO(Boolean.TRUE.equals(request.dryRun()) ? 0 : warnings.size(), skipped, warnings);
    }

    @Transactional(readOnly = true)
    public PageResponse<LearningWarningVO> listForStudent(Long studentId, WarningListQuery query) {
        var wrapper = Wrappers.<LearningWarningEntity>lambdaQuery()
                .eq(LearningWarningEntity::getStudentId, studentId);
        if (query.getCourseId() != null) {
            wrapper.eq(LearningWarningEntity::getCourseId, query.getCourseId());
        }
        applyFilters(wrapper, query, true);
        wrapper.orderByDesc(LearningWarningEntity::getGeneratedAt).orderByDesc(LearningWarningEntity::getId);
        return warningPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public LearningWarningVO studentDetail(Long studentId, Long warningId) {
        LearningWarningEntity warning = warningMapper.selectById(warningId);
        if (warning == null || !studentId.equals(warning.getStudentId())) {
            throw new BusinessException(WarningErrorCode.WARNING_NOT_FOUND);
        }
        return assemble(warning);
    }

    @Transactional(readOnly = true)
    public PageResponse<LearningWarningVO> listForTeacher(Long teacherId, Long courseId, WarningListQuery query) {
        courseManagementService.requireEditor(teacherId, courseId);
        var wrapper = Wrappers.<LearningWarningEntity>lambdaQuery()
                .eq(LearningWarningEntity::getCourseId, courseId);
        applyFilters(wrapper, query, true);
        wrapper.orderByAsc(LearningWarningEntity::getWarningStatus)
                .orderByDesc(LearningWarningEntity::getGeneratedAt)
                .orderByDesc(LearningWarningEntity::getId);
        return warningPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public LearningWarningVO teacherDetail(Long teacherId, Long warningId) {
        LearningWarningEntity warning = requireWarning(warningId);
        courseManagementService.requireEditor(teacherId, warning.getCourseId());
        return assemble(warning);
    }

    @Transactional
    public LearningWarningVO handle(Long teacherId, Long warningId, WarningHandleRequest request) {
        LearningWarningEntity warning = requireWarning(warningId);
        courseManagementService.requireEditor(teacherId, warning.getCourseId());
        if (request.action() == WarningStatus.OPEN) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "action must be HANDLED or IGNORED");
        }
        if (!WarningStatus.OPEN.name().equals(warning.getWarningStatus())) {
            throw new BusinessException(WarningErrorCode.WARNING_STATE_CONFLICT);
        }
        warning.setVersion(request.version());
        warning.setWarningStatus(request.action().name());
        warning.setHandledBy(teacherId);
        warning.setHandleRemark(trimToNull(request.remark()));
        warning.setHandledAt(now());
        updateOrConflict(warning);
        return assemble(requireWarning(warningId));
    }

    private EnumSet<WarningType> requestedTypes(List<WarningType> types) {
        if (types == null || types.isEmpty()) {
            return EnumSet.allOf(WarningType.class);
        }
        return EnumSet.copyOf(types);
    }

    private List<Long> activeStudentIds(Long courseId, Long requestedStudentId) {
        var wrapper = Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .in(CourseEnrollmentEntity::getStatus,
                        EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name());
        if (requestedStudentId != null) {
            wrapper.eq(CourseEnrollmentEntity::getStudentId, requestedStudentId);
        }
        return enrollmentMapper.selectList(wrapper).stream()
                .map(CourseEnrollmentEntity::getStudentId)
                .distinct()
                .toList();
    }

    private List<WarningCandidate> candidates(Long courseId, Long studentId, EnumSet<WarningType> types) {
        List<WarningCandidate> candidates = new ArrayList<>();
        if (types.contains(WarningType.PROGRESS_LAG)) {
            progressCandidate(courseId, studentId).forEach(candidates::add);
        }
        if (types.contains(WarningType.MISSING_ASSIGNMENT)) {
            candidates.addAll(missingAssignmentCandidates(courseId, studentId));
        }
        if (types.contains(WarningType.LOW_SCORE)) {
            candidates.addAll(lowScoreCandidates(courseId, studentId));
        }
        return candidates;
    }

    private List<WarningCandidate> progressCandidate(Long courseId, Long studentId) {
        List<Long> publishedChapterIds = chapterMapper.selectList(Wrappers.<CourseChapterEntity>lambdaQuery()
                        .eq(CourseChapterEntity::getCourseId, courseId)
                        .eq(CourseChapterEntity::getStatus, ChapterStatus.PUBLISHED.name()))
                .stream()
                .map(CourseChapterEntity::getId)
                .toList();
        if (publishedChapterIds.isEmpty()) {
            return List.of();
        }
        List<CourseLessonEntity> lessons = lessonMapper.selectList(Wrappers.<CourseLessonEntity>lambdaQuery()
                .eq(CourseLessonEntity::getCourseId, courseId)
                .in(CourseLessonEntity::getChapterId, publishedChapterIds)
                .eq(CourseLessonEntity::getStatus, LessonStatus.PUBLISHED.name()))
                .stream()
                .filter(coursePermissionService::isUnlocked)
                .toList();
        if (lessons.isEmpty()) {
            return List.of();
        }
        List<Long> lessonIds = lessons.stream().map(CourseLessonEntity::getId).toList();
        long completed = learningRecordMapper.selectCount(Wrappers.<LessonLearningRecordEntity>lambdaQuery()
                .eq(LessonLearningRecordEntity::getCourseId, courseId)
                .eq(LessonLearningRecordEntity::getStudentId, studentId)
                .in(LessonLearningRecordEntity::getLessonId, lessonIds)
                .eq(LessonLearningRecordEntity::getStatus, LearningStatus.COMPLETED.name()));
        BigDecimal progress = BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(lessons.size()), 2, RoundingMode.HALF_UP);
        if (progress.compareTo(PROGRESS_THRESHOLD_PERCENT) >= 0) {
            return List.of();
        }
        WarningLevel level = progress.compareTo(HIGH_PROGRESS_THRESHOLD_PERCENT) < 0
                ? WarningLevel.HIGH
                : WarningLevel.MEDIUM;
        return List.of(new WarningCandidate(
                courseId,
                studentId,
                WarningType.PROGRESS_LAG,
                level,
                "学习进度低于课程阈值",
                "建议教师联系学生确认学习阻碍，并督促完成未学习课时。",
                WarningEvidenceType.LESSON_PROGRESS,
                courseId,
                "progressPercent",
                progress.toPlainString(),
                "已完成课时 " + completed + "/" + lessons.size()));
    }

    private List<WarningCandidate> missingAssignmentCandidates(Long courseId, Long studentId) {
        List<AssignmentEntity> assignments = assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                .eq(AssignmentEntity::getCourseId, courseId)
                .eq(AssignmentEntity::getStatus, AssignmentStatus.PUBLISHED.name())
                .isNotNull(AssignmentEntity::getDueAt)
                .lt(AssignmentEntity::getDueAt, now()));
        List<WarningCandidate> candidates = new ArrayList<>();
        for (AssignmentEntity assignment : assignments) {
            long finalSubmissions = submissionMapper.selectCount(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                    .eq(AssignmentSubmissionEntity::getAssignmentId, assignment.getId())
                    .eq(AssignmentSubmissionEntity::getStudentId, studentId)
                    .in(AssignmentSubmissionEntity::getStatus,
                            SubmissionStatus.SUBMITTED.name(), SubmissionStatus.GRADED.name()));
            if (finalSubmissions == 0) {
                candidates.add(new WarningCandidate(
                        courseId,
                        studentId,
                        WarningType.MISSING_ASSIGNMENT,
                        WarningLevel.MEDIUM,
                        "存在已截止但未正式提交的作业",
                        "建议提醒学生补交或说明缺交原因。",
                        WarningEvidenceType.ASSIGNMENT_SUBMISSION,
                        assignment.getId(),
                        "submissionStatus",
                        "MISSING",
                        "作业已截止：" + assignment.getTitle()));
            }
        }
        return candidates;
    }

    private List<WarningCandidate> lowScoreCandidates(Long courseId, Long studentId) {
        List<GradeRecordEntity> grades = gradeMapper.selectList(Wrappers.<GradeRecordEntity>lambdaQuery()
                .eq(GradeRecordEntity::getCourseId, courseId)
                .eq(GradeRecordEntity::getStudentId, studentId)
                .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name())
                .eq(GradeRecordEntity::getGradeStatus, GradeStatus.PUBLISHED.name()));
        List<WarningCandidate> candidates = new ArrayList<>();
        for (GradeRecordEntity grade : grades) {
            if (grade.getScore() == null || grade.getMaxScore() == null
                    || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal rate = grade.getScore().divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP);
            if (rate.compareTo(LOW_SCORE_RATE) >= 0) {
                continue;
            }
            WarningLevel level = rate.compareTo(HIGH_LOW_SCORE_RATE) < 0 ? WarningLevel.HIGH : WarningLevel.MEDIUM;
            candidates.add(new WarningCandidate(
                    courseId,
                    studentId,
                    WarningType.LOW_SCORE,
                    level,
                    "已发布成绩低于课程阈值",
                    "建议教师结合提交内容给出针对性反馈，并安排补救学习。",
                    WarningEvidenceType.GRADE_RECORD,
                    grade.getId(),
                    "scoreRate",
                    rate.toPlainString(),
                    "成绩 " + grade.getScore().toPlainString() + "/" + grade.getMaxScore().toPlainString()));
        }
        return candidates;
    }

    private boolean hasOpenWarning(WarningCandidate candidate) {
        List<LearningWarningEntity> warnings = warningMapper.selectList(Wrappers.<LearningWarningEntity>lambdaQuery()
                .eq(LearningWarningEntity::getCourseId, candidate.courseId())
                .eq(LearningWarningEntity::getStudentId, candidate.studentId())
                .eq(LearningWarningEntity::getWarningType, candidate.type().name())
                .eq(LearningWarningEntity::getWarningStatus, WarningStatus.OPEN.name()));
        for (LearningWarningEntity warning : warnings) {
            long evidenceCount = evidenceMapper.selectCount(Wrappers.<WarningEvidenceEntity>lambdaQuery()
                    .eq(WarningEvidenceEntity::getWarningId, warning.getId())
                    .eq(WarningEvidenceEntity::getEvidenceType, candidate.evidenceType().name())
                    .eq(WarningEvidenceEntity::getSourceId, candidate.sourceId()));
            if (evidenceCount > 0) {
                return true;
            }
        }
        return false;
    }

    private LearningWarningVO save(WarningCandidate candidate) {
        LearningWarningEntity warning = warning(candidate);
        warningMapper.insert(warning);
        WarningEvidenceEntity evidence = evidence(candidate, warning.getId());
        evidenceMapper.insert(evidence);
        notificationService.publishWarning(warning);
        return assembler.toVO(warning, List.of(evidence), studentName(candidate.studentId()));
    }

    private LearningWarningVO preview(WarningCandidate candidate) {
        return assembler.toVO(warning(candidate), List.of(evidence(candidate, null)), studentName(candidate.studentId()));
    }

    private LearningWarningEntity warning(WarningCandidate candidate) {
        LearningWarningEntity warning = new LearningWarningEntity();
        warning.setCourseId(candidate.courseId());
        warning.setStudentId(candidate.studentId());
        warning.setWarningType(candidate.type().name());
        warning.setWarningLevel(candidate.level().name());
        warning.setWarningStatus(WarningStatus.OPEN.name());
        warning.setSummary(candidate.summary());
        warning.setSuggestion(candidate.suggestion());
        warning.setGeneratedAt(now());
        return warning;
    }

    private WarningEvidenceEntity evidence(WarningCandidate candidate, Long warningId) {
        WarningEvidenceEntity evidence = new WarningEvidenceEntity();
        evidence.setWarningId(warningId);
        evidence.setEvidenceType(candidate.evidenceType().name());
        evidence.setSourceId(candidate.sourceId());
        evidence.setMetricCode(candidate.metricCode());
        evidence.setMetricValue(candidate.metricValue());
        evidence.setDescription(candidate.description());
        return evidence;
    }

    private PageResponse<LearningWarningVO> warningPage(
            WarningListQuery query,
            LambdaQueryWrapper<LearningWarningEntity> wrapper) {
        IPage<LearningWarningEntity> page =
                warningMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<LearningWarningVO> records = page.getRecords().stream().map(this::assemble).toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private LearningWarningVO assemble(LearningWarningEntity warning) {
        List<WarningEvidenceEntity> evidences = evidenceMapper.selectList(Wrappers.<WarningEvidenceEntity>lambdaQuery()
                .eq(WarningEvidenceEntity::getWarningId, warning.getId())
                .orderByAsc(WarningEvidenceEntity::getId));
        return assembler.toVO(warning, evidences, studentName(warning.getStudentId()));
    }

    private void applyFilters(
            LambdaQueryWrapper<LearningWarningEntity> wrapper,
            WarningListQuery query,
            boolean allowStudentFilter) {
        if (allowStudentFilter && query.getStudentId() != null) {
            wrapper.eq(LearningWarningEntity::getStudentId, query.getStudentId());
        }
        if (query.getWarningType() != null) {
            wrapper.eq(LearningWarningEntity::getWarningType, query.getWarningType().name());
        }
        if (query.getWarningLevel() != null) {
            wrapper.eq(LearningWarningEntity::getWarningLevel, query.getWarningLevel().name());
        }
        if (query.getWarningStatus() != null) {
            wrapper.eq(LearningWarningEntity::getWarningStatus, query.getWarningStatus().name());
        }
    }

    private LearningWarningEntity requireWarning(Long warningId) {
        LearningWarningEntity warning = warningMapper.selectById(warningId);
        if (warning == null) {
            throw new BusinessException(WarningErrorCode.WARNING_NOT_FOUND);
        }
        return warning;
    }

    private String studentName(Long studentId) {
        UserEntity user = userMapper.selectById(studentId);
        return user == null ? null : user.getDisplayName();
    }

    private void updateOrConflict(LearningWarningEntity warning) {
        if (warningMapper.updateById(warning) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "Warning changed, refresh and retry");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record WarningCandidate(
            Long courseId,
            Long studentId,
            WarningType type,
            WarningLevel level,
            String summary,
            String suggestion,
            WarningEvidenceType evidenceType,
            Long sourceId,
            String metricCode,
            String metricValue,
            String description) {}
}
