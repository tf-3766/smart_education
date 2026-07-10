package com.zhongruan.edu.biz.grade.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.assignment.api.dto.query.AssignmentListQuery;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.grade.api.dto.query.GradeListQuery;
import com.zhongruan.edu.biz.grade.api.dto.request.GradeSubmissionRequest;
import com.zhongruan.edu.biz.grade.api.dto.request.PublishGradeRequest;
import com.zhongruan.edu.biz.grade.api.vo.AssignmentStatisticsVO;
import com.zhongruan.edu.biz.grade.api.vo.CourseGradeStatisticsVO;
import com.zhongruan.edu.biz.grade.api.vo.StudentGradeVO;
import com.zhongruan.edu.biz.grade.api.vo.TeacherSubmissionGradeVO;
import com.zhongruan.edu.biz.grade.application.assembler.GradeAssembler;
import com.zhongruan.edu.biz.grade.domain.GradeErrorCode;
import com.zhongruan.edu.biz.grade.domain.enums.GradeSourceType;
import com.zhongruan.edu.biz.grade.domain.enums.GradeStatus;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.entity.GradeRecordEntity;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.mapper.GradeRecordMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeApplicationService {
    private static final BigDecimal LOW_SCORE_RATE = new BigDecimal("0.60");

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final GradeRecordMapper gradeMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final CourseManagementService courseManagementService;
    private final GradeAssembler assembler;
    private final Clock clock = Clock.systemUTC();

    public GradeApplicationService(
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            GradeRecordMapper gradeMapper,
            CourseEnrollmentMapper enrollmentMapper,
            UserMapper userMapper,
            CourseManagementService courseManagementService,
            GradeAssembler assembler) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.gradeMapper = gradeMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.userMapper = userMapper;
        this.courseManagementService = courseManagementService;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherSubmissionGradeVO> listSubmissions(
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
        Map<Long, GradeRecordEntity> grades = gradeMap(assignmentId);
        Map<Long, String> studentNames = studentNames(page.getRecords().stream()
                .map(AssignmentSubmissionEntity::getStudentId)
                .distinct()
                .toList());
        List<TeacherSubmissionGradeVO> records = page.getRecords().stream()
                .map(submission -> assembler.toTeacherSubmission(
                        submission, assignment, grades.get(submission.getStudentId()),
                        studentNames.get(submission.getStudentId())))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public TeacherSubmissionGradeVO gradeSubmission(
            Long teacherId, Long submissionId, GradeSubmissionRequest request) {
        AssignmentSubmissionEntity submission = requireSubmission(submissionId);
        AssignmentEntity assignment = requireAssignment(submission.getAssignmentId());
        courseManagementService.requireEditor(teacherId, submission.getCourseId());
        GradeRecordEntity existing = findGrade(assignment.getId(), submission.getStudentId());
        validateGradeRequest(submission, assignment, existing, request);

        LocalDateTime now = now();
        boolean publishNow = Boolean.TRUE.equals(request.publishNow());
        submission.setVersion(request.version());
        submission.setStatus(SubmissionStatus.GRADED.name());
        submission.setScore(request.score());
        submission.setTeacherComment(trim(request.teacherComment()));
        submission.setAiCommentDraftId(request.aiCommentDraftId());
        submission.setGradedBy(teacherId);
        submission.setGradedAt(now);
        if (publishNow) {
            submission.setPublishedAt(now);
        }
        updateSubmissionOrConflict(submission);

        GradeRecordEntity grade = existing == null ? new GradeRecordEntity() : existing;
        grade.setCourseId(submission.getCourseId());
        grade.setStudentId(submission.getStudentId());
        grade.setSourceType(GradeSourceType.ASSIGNMENT.name());
        grade.setSourceId(assignment.getId());
        grade.setScore(request.score());
        grade.setMaxScore(assignment.getMaxScore());
        grade.setGradeStatus(publishNow ? GradeStatus.PUBLISHED.name() : GradeStatus.DRAFT.name());
        grade.setComment(trim(request.teacherComment()));
        grade.setPublishedAt(publishNow ? now : null);
        if (existing == null) {
            gradeMapper.insert(grade);
        } else {
            updateGradeOrConflict(grade);
        }
        AssignmentSubmissionEntity freshSubmission = requireSubmission(submissionId);
        GradeRecordEntity freshGrade = requireGrade(grade.getId());
        return assembler.toTeacherSubmission(
                freshSubmission, assignment, freshGrade, studentName(freshSubmission.getStudentId()));
    }

    @Transactional
    public TeacherSubmissionGradeVO publish(Long teacherId, Long gradeId, PublishGradeRequest request) {
        GradeRecordEntity grade = requireGrade(gradeId);
        courseManagementService.requireEditor(teacherId, grade.getCourseId());
        if (!GradeStatus.DRAFT.name().equals(grade.getGradeStatus())) {
            throw new BusinessException(GradeErrorCode.GRADE_STATE_CONFLICT);
        }
        LocalDateTime now = now();
        grade.setVersion(request.version());
        grade.setGradeStatus(GradeStatus.PUBLISHED.name());
        grade.setPublishedAt(now);
        updateGradeOrConflict(grade);

        AssignmentSubmissionEntity submission = requireSubmissionByGrade(grade);
        submission.setPublishedAt(now);
        updateSubmissionOrConflict(submission);

        AssignmentEntity assignment = requireAssignment(grade.getSourceId());
        GradeRecordEntity freshGrade = requireGrade(gradeId);
        AssignmentSubmissionEntity freshSubmission = requireSubmission(submission.getId());
        return assembler.toTeacherSubmission(
                freshSubmission, assignment, freshGrade, studentName(freshSubmission.getStudentId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentGradeVO> listStudentGrades(Long studentId, GradeListQuery query) {
        if (query.getSourceType() != null && !GradeSourceType.ASSIGNMENT.name().equals(query.getSourceType())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "Unsupported grade source type");
        }
        var wrapper = Wrappers.<GradeRecordEntity>lambdaQuery()
                .eq(GradeRecordEntity::getStudentId, studentId)
                .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name())
                .eq(GradeRecordEntity::getGradeStatus, GradeStatus.PUBLISHED.name());
        if (query.getCourseId() != null) {
            wrapper.eq(GradeRecordEntity::getCourseId, query.getCourseId());
        }
        if (query.getAssignmentId() != null) {
            wrapper.eq(GradeRecordEntity::getSourceId, query.getAssignmentId());
        }
        wrapper.orderByDesc(GradeRecordEntity::getPublishedAt).orderByDesc(GradeRecordEntity::getId);
        IPage<GradeRecordEntity> page = gradeMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, AssignmentEntity> assignments = assignments(page.getRecords().stream()
                .map(GradeRecordEntity::getSourceId)
                .distinct()
                .toList());
        List<StudentGradeVO> records = page.getRecords().stream()
                .map(grade -> assembler.toStudentGrade(grade, assignments.get(grade.getSourceId())))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public AssignmentStatisticsVO statistics(Long teacherId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        courseManagementService.requireEditor(teacherId, assignment.getCourseId());
        long totalStudents = enrollmentMapper.selectCount(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getCourseId, assignment.getCourseId())
                .in(CourseEnrollmentEntity::getStatus, EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()));
        List<AssignmentSubmissionEntity> submissions = submissionMapper.selectList(
                Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                        .eq(AssignmentSubmissionEntity::getAssignmentId, assignmentId)
                        .in(AssignmentSubmissionEntity::getStatus,
                                SubmissionStatus.SUBMITTED.name(), SubmissionStatus.GRADED.name()));
        long submittedCount = submissions.stream().map(AssignmentSubmissionEntity::getStudentId).distinct().count();
        long gradedCount = submissions.stream()
                .filter(submission -> SubmissionStatus.GRADED.name().equals(submission.getStatus()))
                .count();
        List<GradeRecordEntity> grades = gradeMapper.selectList(Wrappers.<GradeRecordEntity>lambdaQuery()
                .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name())
                .eq(GradeRecordEntity::getSourceId, assignmentId));
        long publishedCount = grades.stream()
                .filter(grade -> GradeStatus.PUBLISHED.name().equals(grade.getGradeStatus()))
                .count();
        BigDecimal average = grades.isEmpty() ? null : grades.stream()
                .map(GradeRecordEntity::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
        long lowScoreCount = grades.stream()
                .filter(grade -> grade.getMaxScore().compareTo(BigDecimal.ZERO) > 0)
                .filter(grade -> grade.getScore().divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP)
                        .compareTo(LOW_SCORE_RATE) < 0)
                .count();
        return assembler.toStatistics(
                assignmentId, assignment.getCourseId(), totalStudents, submittedCount,
                gradedCount, publishedCount, average, lowScoreCount);
    }

    @Transactional(readOnly = true)
    public CourseGradeStatisticsVO courseStatistics(Long teacherId, Long courseId) {
        courseManagementService.requireEditor(teacherId, courseId);
        List<AssignmentEntity> assignments = assignmentMapper.selectList(
                Wrappers.<AssignmentEntity>lambdaQuery().eq(AssignmentEntity::getCourseId, courseId));
        long publishedAssignments = assignments.stream()
                .filter(assignment -> AssignmentStatus.PUBLISHED.name().equals(assignment.getStatus())
                        || AssignmentStatus.CLOSED.name().equals(assignment.getStatus()))
                .count();
        long enrolledStudents = enrollmentMapper.selectCount(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .in(CourseEnrollmentEntity::getStatus, EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()));
        List<GradeRecordEntity> grades = gradeMapper.selectList(Wrappers.<GradeRecordEntity>lambdaQuery()
                .eq(GradeRecordEntity::getCourseId, courseId)
                .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name()));
        List<GradeRecordEntity> publishedGrades = grades.stream()
                .filter(grade -> GradeStatus.PUBLISHED.name().equals(grade.getGradeStatus()))
                .toList();
        long lowScores = publishedGrades.stream().filter(this::isLowScore).count();
        long passingScores = publishedGrades.stream().filter(grade -> !isLowScore(grade)).count();
        BigDecimal averageRate = percentageAverage(publishedGrades);
        BigDecimal passRate = publishedGrades.isEmpty()
                ? null
                : BigDecimal.valueOf(passingScores)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(publishedGrades.size()), 2, RoundingMode.HALF_UP);
        return new CourseGradeStatisticsVO(
                String.valueOf(courseId),
                assignments.size(),
                publishedAssignments,
                enrolledStudents,
                grades.size(),
                publishedGrades.size(),
                averageRate,
                passRate,
                lowScores);
    }

    private BigDecimal percentageAverage(List<GradeRecordEntity> grades) {
        List<BigDecimal> rates = grades.stream()
                .filter(grade -> grade.getMaxScore().compareTo(BigDecimal.ZERO) > 0)
                .map(grade -> grade.getScore()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP))
                .toList();
        if (rates.isEmpty()) {
            return null;
        }
        return rates.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rates.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean isLowScore(GradeRecordEntity grade) {
        return grade.getMaxScore().compareTo(BigDecimal.ZERO) > 0
                && grade.getScore().divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP)
                        .compareTo(LOW_SCORE_RATE) < 0;
    }

    private void validateGradeRequest(
            AssignmentSubmissionEntity submission,
            AssignmentEntity assignment,
            GradeRecordEntity grade,
            GradeSubmissionRequest request) {
        if (!(SubmissionStatus.SUBMITTED.name().equals(submission.getStatus())
                || SubmissionStatus.GRADED.name().equals(submission.getStatus()))) {
            throw new BusinessException(GradeErrorCode.SUBMISSION_STATE_CONFLICT);
        }
        if (grade != null && GradeStatus.PUBLISHED.name().equals(grade.getGradeStatus())) {
            throw new BusinessException(GradeErrorCode.GRADE_STATE_CONFLICT);
        }
        if (request.maxScore().compareTo(assignment.getMaxScore()) != 0) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "maxScore must match assignment maxScore");
        }
        if (request.score().compareTo(BigDecimal.ZERO) < 0 || request.score().compareTo(assignment.getMaxScore()) > 0) {
            throw new BusinessException(GradeErrorCode.GRADE_SCORE_OUT_OF_RANGE);
        }
    }

    private AssignmentEntity requireAssignment(Long assignmentId) {
        AssignmentEntity assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    private AssignmentSubmissionEntity requireSubmission(Long submissionId) {
        AssignmentSubmissionEntity submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(GradeErrorCode.SUBMISSION_NOT_FOUND);
        }
        return submission;
    }

    private GradeRecordEntity requireGrade(Long gradeId) {
        GradeRecordEntity grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(GradeErrorCode.GRADE_NOT_FOUND);
        }
        return grade;
    }

    private AssignmentSubmissionEntity requireSubmissionByGrade(GradeRecordEntity grade) {
        AssignmentSubmissionEntity submission = submissionMapper.selectOne(
                Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                        .eq(AssignmentSubmissionEntity::getAssignmentId, grade.getSourceId())
                        .eq(AssignmentSubmissionEntity::getStudentId, grade.getStudentId())
                        .orderByDesc(AssignmentSubmissionEntity::getAttemptNo)
                        .orderByDesc(AssignmentSubmissionEntity::getId)
                        .last("LIMIT 1"));
        if (submission == null) {
            throw new BusinessException(GradeErrorCode.SUBMISSION_NOT_FOUND);
        }
        return submission;
    }

    private GradeRecordEntity findGrade(Long assignmentId, Long studentId) {
        return gradeMapper.selectOne(Wrappers.<GradeRecordEntity>lambdaQuery()
                .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name())
                .eq(GradeRecordEntity::getSourceId, assignmentId)
                .eq(GradeRecordEntity::getStudentId, studentId));
    }

    private Map<Long, GradeRecordEntity> gradeMap(Long assignmentId) {
        return gradeMapper.selectList(Wrappers.<GradeRecordEntity>lambdaQuery()
                        .eq(GradeRecordEntity::getSourceType, GradeSourceType.ASSIGNMENT.name())
                        .eq(GradeRecordEntity::getSourceId, assignmentId))
                .stream()
                .collect(Collectors.toMap(GradeRecordEntity::getStudentId, Function.identity()));
    }

    private Map<Long, AssignmentEntity> assignments(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return assignmentMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(AssignmentEntity::getId, Function.identity()));
    }

    private Map<Long, String> studentNames(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName));
    }

    private String studentName(Long studentId) {
        UserEntity user = userMapper.selectById(studentId);
        return user == null ? null : user.getDisplayName();
    }

    private void updateSubmissionOrConflict(AssignmentSubmissionEntity submission) {
        if (submissionMapper.updateById(submission) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "Submission changed, refresh and retry");
        }
    }

    private void updateGradeOrConflict(GradeRecordEntity grade) {
        if (gradeMapper.updateById(grade) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "Grade changed, refresh and retry");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
