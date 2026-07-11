package com.zhongruan.edu.biz.exam.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.exam.api.dto.request.ExamAnswerSubmitRequest;
import com.zhongruan.edu.biz.exam.api.dto.query.ExamAttemptListQuery;
import com.zhongruan.edu.biz.exam.api.dto.request.GradeExamAnswerRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.GradeExamAttemptRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.SubmitExamAttemptRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamAnswerVO;
import com.zhongruan.edu.biz.exam.api.vo.ExamAttemptVO;
import com.zhongruan.edu.biz.exam.api.vo.StudentExamOptionVO;
import com.zhongruan.edu.biz.exam.api.vo.StudentExamQuestionVO;
import com.zhongruan.edu.biz.exam.domain.enums.ExamAttemptStatus;
import com.zhongruan.edu.biz.exam.domain.enums.ExamPaperStatus;
import com.zhongruan.edu.biz.exam.domain.enums.ExamStatus;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionType;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamAnswerEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamAttemptEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperQuestionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionOptionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamAnswerMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamAttemptMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamPaperMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamPaperQuestionMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionOptionMapper;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamParticipationService {
    private final CoursePermissionService coursePermissionService;
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final ExamAttemptMapper attemptMapper;
    private final ExamAnswerMapper answerMapper;
    private final Clock clock = Clock.systemUTC();

    public ExamParticipationService(
            CoursePermissionService coursePermissionService,
            ExamMapper examMapper,
            ExamPaperMapper paperMapper,
            ExamPaperQuestionMapper paperQuestionMapper,
            QuestionMapper questionMapper,
            QuestionOptionMapper optionMapper,
            ExamAttemptMapper attemptMapper,
            ExamAnswerMapper answerMapper) {
        this.coursePermissionService = coursePermissionService;
        this.examMapper = examMapper;
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
    }

    @Transactional
    public ExamAttemptVO start(Long studentId, Long examId) {
        ExamEntity exam = requireAvailableExam(studentId, examId);
        ExamPaperEntity paper = requirePublishedPaper(examId);
        ExamAttemptEntity existing = findAttempt(studentId, examId);
        if (existing != null) {
            if (ExamAttemptStatus.IN_PROGRESS.name().equals(existing.getStatus())) {
                return toVO(existing, exam, paper);
            }
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该考试已经提交，不能重复开始");
        }

        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setExamId(examId);
        attempt.setPaperId(paper.getId());
        attempt.setStudentId(studentId);
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS.name());
        attempt.setStartedAt(LocalDateTime.now(clock));
        try {
            attemptMapper.insert(attempt);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "考试已在其他请求中开始");
        }
        return toVO(attempt, exam, paper);
    }

    @Transactional(readOnly = true)
    public ExamAttemptVO get(Long studentId, Long attemptId) {
        ExamAttemptEntity attempt = requireOwnedAttempt(studentId, attemptId);
        return toVO(attempt, requireExam(attempt.getExamId()), requirePaper(attempt.getPaperId()));
    }

    @Transactional
    public ExamAttemptVO submit(Long studentId, Long attemptId, SubmitExamAttemptRequest request) {
        ExamAttemptEntity attempt = requireOwnedAttempt(studentId, attemptId);
        if (!ExamAttemptStatus.IN_PROGRESS.name().equals(attempt.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试答卷已经提交");
        }
        ExamEntity exam = requireExam(attempt.getExamId());
        ExamPaperEntity paper = requirePaper(attempt.getPaperId());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = deadline(attempt, exam);
        if (deadline != null && now.isAfter(deadline)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试作答时间已经结束");
        }

        List<ExamPaperQuestionEntity> paperQuestions = paperQuestions(paper.getId());
        Map<Long, ExamAnswerSubmitRequest> submitted = uniqueAnswers(request.answers());
        Set<Long> allowedQuestionIds = paperQuestions.stream()
                .map(ExamPaperQuestionEntity::getQuestionId)
                .collect(java.util.stream.Collectors.toSet());
        if (!allowedQuestionIds.containsAll(submitted.keySet())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "答卷包含不属于当前试卷的题目");
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        boolean fullyAutoGraded = true;
        for (ExamPaperQuestionEntity item : paperQuestions) {
            QuestionEntity question = requireQuestion(item.getQuestionId());
            QuestionType type = QuestionType.valueOf(question.getQuestionType());
            ExamAnswerSubmitRequest value = submitted.get(item.getQuestionId());
            BigDecimal answerScore = null;
            if (type == QuestionType.SHORT_ANSWER) {
                fullyAutoGraded = false;
            } else {
                answerScore = value != null && isCorrect(item.getQuestionId(), type, value.answerContent())
                        ? item.getScore()
                        : BigDecimal.ZERO;
                totalScore = totalScore.add(answerScore);
            }
            if (value != null) {
                ExamAnswerEntity answer = new ExamAnswerEntity();
                answer.setAttemptId(attemptId);
                answer.setQuestionId(item.getQuestionId());
                answer.setAnswerContent(value.answerContent().trim());
                answer.setScore(answerScore);
                answerMapper.insert(answer);
            }
        }

        attempt.setStatus((fullyAutoGraded ? ExamAttemptStatus.GRADED : ExamAttemptStatus.SUBMITTED).name());
        attempt.setSubmittedAt(now);
        attempt.setScore(fullyAutoGraded ? totalScore : null);
        attempt.setGradedAt(fullyAutoGraded ? now : null);
        attempt.setVersion(request.version());
        if (attemptMapper.updateById(attempt) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "答卷已被其他请求修改，请刷新后重试");
        }
        return toVO(attempt, exam, paper);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamAttemptVO> listForTeacher(
            Long teacherId, Long examId, ExamAttemptListQuery query) {
        ExamEntity exam = requireExam(examId);
        requireTeacherCourse(teacherId, exam.getCourseId());
        var wrapper = Wrappers.<ExamAttemptEntity>lambdaQuery()
                .eq(ExamAttemptEntity::getExamId, examId);
        if (query.getStatus() != null) {
            wrapper.eq(ExamAttemptEntity::getStatus, query.getStatus().name());
        }
        var page = attemptMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                wrapper.orderByDesc(ExamAttemptEntity::getSubmittedAt)
                        .orderByDesc(ExamAttemptEntity::getId));
        List<ExamAttemptVO> records = page.getRecords().stream()
                .map(attempt -> toVO(attempt, exam, requirePaper(attempt.getPaperId())))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public ExamAttemptVO grade(
            Long teacherId, Long attemptId, GradeExamAttemptRequest request) {
        ExamAttemptEntity attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "考试答卷不存在");
        }
        ExamEntity exam = requireExam(attempt.getExamId());
        requireTeacherCourse(teacherId, exam.getCourseId());
        if (!ExamAttemptStatus.SUBMITTED.name().equals(attempt.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "只有待批阅答卷可以评分");
        }
        ExamPaperEntity paper = requirePaper(attempt.getPaperId());
        Map<Long, ExamPaperQuestionEntity> paperItems = new HashMap<>();
        paperQuestions(paper.getId()).forEach(item -> paperItems.put(item.getQuestionId(), item));
        Map<Long, GradeExamAnswerRequest> grades = new HashMap<>();
        for (GradeExamAnswerRequest grade : request.answers()) {
            if (grades.putIfAbsent(grade.questionId(), grade) != null) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "同一道题目不能重复评分");
            }
            ExamPaperQuestionEntity item = paperItems.get(grade.questionId());
            QuestionEntity question = item == null ? null : requireQuestion(item.getQuestionId());
            if (item == null || !QuestionType.SHORT_ANSWER.name().equals(question.getQuestionType())) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "只能人工评分当前试卷中的简答题");
            }
            if (grade.score().compareTo(item.getScore()) > 0) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "题目得分不能超过题目分值");
            }
            ExamAnswerEntity answer = answerMapper.selectOne(Wrappers.<ExamAnswerEntity>lambdaQuery()
                    .eq(ExamAnswerEntity::getAttemptId, attemptId)
                    .eq(ExamAnswerEntity::getQuestionId, grade.questionId()));
            if (answer == null) {
                throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "待评分答案不存在");
            }
            answer.setScore(grade.score());
            answer.setTeacherComment(trim(grade.teacherComment()));
            if (answerMapper.updateById(answer) != 1) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ExamPaperQuestionEntity item : paperItems.values()) {
            QuestionEntity question = requireQuestion(item.getQuestionId());
            ExamAnswerEntity answer = answerMapper.selectOne(Wrappers.<ExamAnswerEntity>lambdaQuery()
                    .eq(ExamAnswerEntity::getAttemptId, attemptId)
                    .eq(ExamAnswerEntity::getQuestionId, item.getQuestionId()));
            if (QuestionType.SHORT_ANSWER.name().equals(question.getQuestionType())
                    && answer != null && answer.getScore() == null) {
                throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "仍有简答题尚未评分");
            }
            if (answer != null && answer.getScore() != null) {
                total = total.add(answer.getScore());
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        attempt.setStatus(ExamAttemptStatus.GRADED.name());
        attempt.setScore(total);
        attempt.setGradedAt(now);
        attempt.setVersion(request.version());
        if (attemptMapper.updateById(attempt) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        return toVO(attempt, exam, paper);
    }

    private ExamEntity requireAvailableExam(Long studentId, Long examId) {
        ExamEntity exam = requireExam(examId);
        if (!coursePermissionService.canViewCourseAsStudent(studentId, exam.getCourseId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你没有参加该课程考试的权限");
        }
        if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试尚未发布");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (exam.getStartAt() != null && now.isBefore(exam.getStartAt())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试尚未开始");
        }
        if (exam.getEndAt() != null && !now.isBefore(exam.getEndAt())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试已经结束");
        }
        return exam;
    }

    private ExamPaperEntity requirePublishedPaper(Long examId) {
        ExamPaperEntity paper = paperMapper.selectOne(Wrappers.<ExamPaperEntity>lambdaQuery()
                .eq(ExamPaperEntity::getExamId, examId)
                .eq(ExamPaperEntity::getStatus, ExamPaperStatus.PUBLISHED.name()));
        if (paper == null) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "考试没有可用的已发布试卷");
        }
        return paper;
    }

    private ExamAttemptEntity findAttempt(Long studentId, Long examId) {
        return attemptMapper.selectOne(Wrappers.<ExamAttemptEntity>lambdaQuery()
                .eq(ExamAttemptEntity::getStudentId, studentId)
                .eq(ExamAttemptEntity::getExamId, examId));
    }

    private ExamAttemptEntity requireOwnedAttempt(Long studentId, Long attemptId) {
        ExamAttemptEntity attempt = attemptMapper.selectOne(Wrappers.<ExamAttemptEntity>lambdaQuery()
                .eq(ExamAttemptEntity::getId, attemptId)
                .eq(ExamAttemptEntity::getStudentId, studentId));
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "考试答卷不存在");
        }
        return attempt;
    }

    private void requireTeacherCourse(Long teacherId, Long courseId) {
        if (!coursePermissionService.canEditCourseContent(teacherId, courseId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你不属于该课程教师团队");
        }
    }

    private ExamEntity requireExam(Long examId) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private ExamPaperEntity requirePaper(Long paperId) {
        ExamPaperEntity paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "试卷不存在");
        }
        return paper;
    }

    private QuestionEntity requireQuestion(Long questionId) {
        QuestionEntity question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private List<ExamPaperQuestionEntity> paperQuestions(Long paperId) {
        return paperQuestionMapper.selectList(Wrappers.<ExamPaperQuestionEntity>lambdaQuery()
                .eq(ExamPaperQuestionEntity::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestionEntity::getQuestionOrder));
    }

    private Map<Long, ExamAnswerSubmitRequest> uniqueAnswers(List<ExamAnswerSubmitRequest> answers) {
        Map<Long, ExamAnswerSubmitRequest> values = new HashMap<>();
        for (ExamAnswerSubmitRequest answer : answers) {
            if (values.putIfAbsent(answer.questionId(), answer) != null) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "同一道题目不能重复提交答案");
            }
        }
        return values;
    }

    private boolean isCorrect(Long questionId, QuestionType type, String answerContent) {
        Set<String> expected = optionMapper.selectList(Wrappers.<QuestionOptionEntity>lambdaQuery()
                        .eq(QuestionOptionEntity::getQuestionId, questionId)
                        .eq(QuestionOptionEntity::getIsCorrect, 1))
                .stream()
                .map(option -> normalizeLabel(option.getOptionLabel()))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> actual = new HashSet<>();
        for (String label : answerContent.split("[,;\\s]+")) {
            if (!label.isBlank()) {
                actual.add(normalizeLabel(label));
            }
        }
        return !expected.isEmpty() && expected.equals(actual)
                && (type == QuestionType.MULTIPLE_CHOICE || actual.size() == 1);
    }

    private String normalizeLabel(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private ExamAttemptVO toVO(ExamAttemptEntity attempt, ExamEntity exam, ExamPaperEntity paper) {
        List<StudentExamQuestionVO> questions = paperQuestions(paper.getId()).stream().map(item -> {
            QuestionEntity question = requireQuestion(item.getQuestionId());
            List<StudentExamOptionVO> options = optionMapper.selectList(Wrappers.<QuestionOptionEntity>lambdaQuery()
                            .eq(QuestionOptionEntity::getQuestionId, question.getId())
                            .orderByAsc(QuestionOptionEntity::getSortOrder))
                    .stream()
                    .map(option -> new StudentExamOptionVO(
                            option.getOptionLabel(), option.getOptionContent(), option.getSortOrder()))
                    .toList();
            return new StudentExamQuestionVO(
                    String.valueOf(question.getId()),
                    item.getQuestionOrder(),
                    item.getScore(),
                    question.getQuestionType(),
                    question.getStem(),
                    options);
        }).toList();
        List<ExamAnswerVO> answers = answerMapper.selectList(Wrappers.<ExamAnswerEntity>lambdaQuery()
                        .eq(ExamAnswerEntity::getAttemptId, attempt.getId())
                        .orderByAsc(ExamAnswerEntity::getId))
                .stream()
                .map(answer -> new ExamAnswerVO(
                        String.valueOf(answer.getQuestionId()), answer.getAnswerContent(), answer.getScore()))
                .toList();
        return new ExamAttemptVO(
                String.valueOf(attempt.getId()),
                String.valueOf(attempt.getExamId()),
                String.valueOf(attempt.getPaperId()),
                String.valueOf(attempt.getStudentId()),
                attempt.getStatus(),
                offset(attempt.getStartedAt()),
                offset(deadline(attempt, exam)),
                offset(attempt.getSubmittedAt()),
                attempt.getScore(),
                questions,
                answers,
                attempt.getVersion());
    }

    private LocalDateTime deadline(ExamAttemptEntity attempt, ExamEntity exam) {
        LocalDateTime durationDeadline = exam.getDurationMinutes() == null || attempt.getStartedAt() == null
                ? null
                : attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());
        if (durationDeadline == null) {
            return exam.getEndAt();
        }
        if (exam.getEndAt() == null) {
            return durationDeadline;
        }
        return durationDeadline.isBefore(exam.getEndAt()) ? durationDeadline : exam.getEndAt();
    }

    private OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
