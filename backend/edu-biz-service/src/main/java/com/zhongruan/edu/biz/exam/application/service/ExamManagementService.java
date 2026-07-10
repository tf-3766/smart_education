package com.zhongruan.edu.biz.exam.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.exam.api.dto.query.ExamListQuery;
import com.zhongruan.edu.biz.exam.api.dto.query.QuestionBankListQuery;
import com.zhongruan.edu.biz.exam.api.dto.query.QuestionListQuery;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateExamPaperRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateExamRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionBankRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.ExamPaperQuestionRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.QuestionOptionRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateExamPaperRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateExamRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateQuestionBankRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateQuestionRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamPaperQuestionVO;
import com.zhongruan.edu.biz.exam.api.vo.ExamPaperVO;
import com.zhongruan.edu.biz.exam.api.vo.ExamVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionBankVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionOptionVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionVO;
import com.zhongruan.edu.biz.exam.api.vo.StudentExamListItemVO;
import com.zhongruan.edu.biz.exam.domain.enums.ExamPaperStatus;
import com.zhongruan.edu.biz.exam.domain.enums.ExamStatus;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionBankStatus;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionStatus;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionType;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperQuestionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionBankEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionOptionEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamPaperMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamPaperQuestionMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionBankMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.QuestionOptionMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamManagementService {
    private final CourseMapper courseMapper;
    private final CoursePermissionService coursePermissionService;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ExamMapper examMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;

    public ExamManagementService(
            CourseMapper courseMapper,
            CoursePermissionService coursePermissionService,
            QuestionBankMapper questionBankMapper,
            QuestionMapper questionMapper,
            QuestionOptionMapper questionOptionMapper,
            ExamMapper examMapper,
            ExamPaperMapper examPaperMapper,
            ExamPaperQuestionMapper examPaperQuestionMapper) {
        this.courseMapper = courseMapper;
        this.coursePermissionService = coursePermissionService;
        this.questionBankMapper = questionBankMapper;
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.examMapper = examMapper;
        this.examPaperMapper = examPaperMapper;
        this.examPaperQuestionMapper = examPaperQuestionMapper;
    }

    @Transactional
    public QuestionBankVO createQuestionBank(Long teacherId, Long courseId, CreateQuestionBankRequest request) {
        requireTeacherCourse(teacherId, courseId);
        QuestionBankEntity bank = new QuestionBankEntity();
        bank.setCourseId(courseId);
        bank.setName(request.name().trim());
        bank.setDescription(trim(request.description()));
        bank.setStatus(QuestionBankStatus.ACTIVE.name());
        questionBankMapper.insert(bank);
        return toQuestionBank(bank);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionBankVO> listQuestionBanks(Long teacherId, Long courseId, QuestionBankListQuery query) {
        requireTeacherCourse(teacherId, courseId);
        LambdaQueryWrapper<QuestionBankEntity> wrapper = Wrappers.<QuestionBankEntity>lambdaQuery()
                .eq(QuestionBankEntity::getCourseId, courseId);
        if (hasText(query.getKeyword())) {
            wrapper.and(group -> group.like(QuestionBankEntity::getName, query.getKeyword().trim())
                    .or()
                    .like(QuestionBankEntity::getDescription, query.getKeyword().trim()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(QuestionBankEntity::getStatus, query.getStatus().name());
        }
        var page = questionBankMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper.orderByDesc(QuestionBankEntity::getId));
        return PageResponse.of(page.getRecords().stream().map(this::toQuestionBank).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public QuestionBankVO getQuestionBank(Long teacherId, Long bankId) {
        QuestionBankEntity bank = requireQuestionBank(bankId);
        requireTeacherCourse(teacherId, bank.getCourseId());
        return toQuestionBank(bank);
    }

    @Transactional
    public QuestionBankVO updateQuestionBank(Long teacherId, Long bankId, UpdateQuestionBankRequest request) {
        QuestionBankEntity bank = requireQuestionBank(bankId);
        requireTeacherCourse(teacherId, bank.getCourseId());
        bank.setName(request.name().trim());
        bank.setDescription(trim(request.description()));
        bank.setStatus(request.status().name());
        bank.setVersion(request.version());
        updateOrConflict(questionBankMapper.updateById(bank), "题库已被其他请求修改，请刷新后重试");
        return toQuestionBank(bank);
    }

    @Transactional
    public void deleteQuestionBank(Long teacherId, Long bankId) {
        QuestionBankEntity bank = requireQuestionBank(bankId);
        requireTeacherCourse(teacherId, bank.getCourseId());
        if (questionMapper.selectCount(Wrappers.<QuestionEntity>lambdaQuery().eq(QuestionEntity::getBankId, bankId)) > 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "题库仍包含题目，不能删除");
        }
        questionBankMapper.deleteById(bankId);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionVO> listQuestions(Long teacherId, Long bankId, QuestionListQuery query) {
        QuestionBankEntity bank = requireQuestionBank(bankId);
        requireTeacherCourse(teacherId, bank.getCourseId());
        LambdaQueryWrapper<QuestionEntity> wrapper = Wrappers.<QuestionEntity>lambdaQuery().eq(QuestionEntity::getBankId, bankId);
        if (hasText(query.getKeyword())) {
            wrapper.like(QuestionEntity::getStem, query.getKeyword().trim());
        }
        if (query.getQuestionType() != null) {
            wrapper.eq(QuestionEntity::getQuestionType, query.getQuestionType().name());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(QuestionEntity::getDifficulty, query.getDifficulty().name());
        }
        if (query.getStatus() != null) {
            wrapper.eq(QuestionEntity::getStatus, query.getStatus().name());
        }
        var page = questionMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper.orderByDesc(QuestionEntity::getId));
        return PageResponse.of(page.getRecords().stream().map(this::toQuestion).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public QuestionVO createQuestion(Long teacherId, Long bankId, CreateQuestionRequest request) {
        QuestionBankEntity bank = requireQuestionBank(bankId);
        requireTeacherCourse(teacherId, bank.getCourseId());
        requireActiveBank(bank);
        validateOptions(request.questionType(), request.options());
        QuestionEntity question = new QuestionEntity();
        question.setBankId(bankId);
        question.setCourseId(bank.getCourseId());
        question.setQuestionType(request.questionType().name());
        question.setStem(request.stem().trim());
        question.setAnalysis(trim(request.analysis()));
        question.setDifficulty(request.difficulty().name());
        question.setScore(request.score());
        question.setStatus(QuestionStatus.ACTIVE.name());
        questionMapper.insert(question);
        replaceQuestionOptions(question.getId(), request.options());
        return toQuestion(question);
    }

    @Transactional(readOnly = true)
    public QuestionVO getQuestion(Long teacherId, Long questionId) {
        QuestionEntity question = requireQuestion(questionId);
        requireTeacherCourse(teacherId, question.getCourseId());
        return toQuestion(question);
    }

    @Transactional
    public QuestionVO updateQuestion(Long teacherId, Long questionId, UpdateQuestionRequest request) {
        QuestionEntity question = requireQuestion(questionId);
        requireTeacherCourse(teacherId, question.getCourseId());
        if (isQuestionInPublishedPaper(questionId)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已发布试卷中的题目不能修改");
        }
        validateOptions(request.questionType(), request.options());
        question.setQuestionType(request.questionType().name());
        question.setStem(request.stem().trim());
        question.setAnalysis(trim(request.analysis()));
        question.setDifficulty(request.difficulty().name());
        question.setScore(request.score());
        question.setStatus(request.status() == null ? question.getStatus() : request.status().name());
        question.setVersion(request.version());
        updateOrConflict(questionMapper.updateById(question), "题目已被其他请求修改，请刷新后重试");
        replaceQuestionOptions(questionId, request.options());
        return toQuestion(question);
    }

    @Transactional
    public void deleteQuestion(Long teacherId, Long questionId) {
        QuestionEntity question = requireQuestion(questionId);
        requireTeacherCourse(teacherId, question.getCourseId());
        if (isQuestionReferencedByAnyPaper(questionId)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已被试卷引用的题目不能删除");
        }
        questionMapper.deleteById(questionId);
    }

    @Transactional
    public ExamVO createExam(Long teacherId, Long courseId, CreateExamRequest request) {
        requireTeacherCourse(teacherId, courseId);
        validateExamTimes(request.startAt(), request.endAt(), request.durationMinutes());
        ExamEntity exam = new ExamEntity();
        exam.setCourseId(courseId);
        exam.setTitle(request.title().trim());
        exam.setDescription(trim(request.description()));
        exam.setStatus(ExamStatus.DRAFT.name());
        exam.setStartAt(utc(request.startAt()));
        exam.setEndAt(utc(request.endAt()));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTotalScore(request.totalScore());
        examMapper.insert(exam);
        return toExam(exam);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamVO> listTeacherExams(Long teacherId, Long courseId, ExamListQuery query) {
        requireTeacherCourse(teacherId, courseId);
        LambdaQueryWrapper<ExamEntity> wrapper = Wrappers.<ExamEntity>lambdaQuery().eq(ExamEntity::getCourseId, courseId);
        if (hasText(query.getKeyword())) {
            wrapper.like(ExamEntity::getTitle, query.getKeyword().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ExamEntity::getStatus, query.getStatus().name());
        }
        var page = examMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper.orderByDesc(ExamEntity::getId));
        return PageResponse.of(page.getRecords().stream().map(this::toExam).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public ExamVO getExam(Long teacherId, Long examId) {
        ExamEntity exam = requireExam(examId);
        requireTeacherCourse(teacherId, exam.getCourseId());
        return toExam(exam);
    }

    @Transactional
    public ExamVO updateExam(Long teacherId, Long examId, UpdateExamRequest request) {
        ExamEntity exam = requireExam(examId);
        requireTeacherCourse(teacherId, exam.getCourseId());
        requireDraftExam(exam);
        validateExamTimes(request.startAt(), request.endAt(), request.durationMinutes());
        if (examPaperMapper.selectCount(Wrappers.<ExamPaperEntity>lambdaQuery().eq(ExamPaperEntity::getExamId, examId)) > 0
                && exam.getTotalScore().compareTo(request.totalScore()) != 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已有试卷草稿时不能修改考试总分");
        }
        exam.setTitle(request.title().trim());
        exam.setDescription(trim(request.description()));
        exam.setStartAt(utc(request.startAt()));
        exam.setEndAt(utc(request.endAt()));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTotalScore(request.totalScore());
        exam.setVersion(request.version());
        updateOrConflict(examMapper.updateById(exam), "考试已被其他请求修改，请刷新后重试");
        return toExam(exam);
    }

    @Transactional
    public void deleteExam(Long teacherId, Long examId) {
        ExamEntity exam = requireExam(examId);
        requireTeacherCourse(teacherId, exam.getCourseId());
        requireDraftExam(exam);
        if (examPaperMapper.selectCount(Wrappers.<ExamPaperEntity>lambdaQuery().eq(ExamPaperEntity::getExamId, examId)) > 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "请先删除考试下的试卷草稿");
        }
        examMapper.deleteById(examId);
    }

    @Transactional
    public ExamPaperVO createPaper(Long teacherId, Long examId, CreateExamPaperRequest request) {
        ExamEntity exam = requireExam(examId);
        requireTeacherCourse(teacherId, exam.getCourseId());
        requireDraftExam(exam);
        validatePaperQuestions(exam.getCourseId(), request.questions());
        ExamPaperEntity paper = new ExamPaperEntity();
        paper.setExamId(examId);
        paper.setCourseId(exam.getCourseId());
        paper.setTitle(request.title().trim());
        paper.setTotalScore(totalScore(request.questions()));
        paper.setStatus(ExamPaperStatus.DRAFT.name());
        paper.setAiGenerationRecordId(null);
        examPaperMapper.insert(paper);
        replacePaperQuestions(paper.getId(), request.questions());
        return toExamPaper(paper);
    }

    @Transactional(readOnly = true)
    public ExamPaperVO getPaper(Long teacherId, Long paperId) {
        ExamPaperEntity paper = requirePaper(paperId);
        requireTeacherCourse(teacherId, paper.getCourseId());
        return toExamPaper(paper);
    }

    @Transactional
    public ExamPaperVO updatePaper(Long teacherId, Long paperId, UpdateExamPaperRequest request) {
        ExamPaperEntity paper = requirePaper(paperId);
        requireTeacherCourse(teacherId, paper.getCourseId());
        requireDraftPaper(paper);
        validatePaperQuestions(paper.getCourseId(), request.questions());
        paper.setTitle(request.title().trim());
        paper.setTotalScore(totalScore(request.questions()));
        paper.setVersion(request.version());
        updateOrConflict(examPaperMapper.updateById(paper), "试卷草稿已被其他请求修改，请刷新后重试");
        replacePaperQuestions(paperId, request.questions());
        return toExamPaper(paper);
    }

    @Transactional
    public ExamPaperVO publishPaper(Long teacherId, Long paperId) {
        ExamPaperEntity paper = requirePaper(paperId);
        requireTeacherCourse(teacherId, paper.getCourseId());
        if (ExamPaperStatus.PUBLISHED.name().equals(paper.getStatus())) {
            return toExamPaper(paper);
        }
        requireDraftPaper(paper);
        ExamEntity exam = requireExam(paper.getExamId());
        requireDraftExam(exam);
        if (paper.getTotalScore().compareTo(exam.getTotalScore()) != 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "试卷题目总分必须与考试总分一致");
        }
        if (examPaperQuestionMapper.selectCount(Wrappers.<ExamPaperQuestionEntity>lambdaQuery()
                        .eq(ExamPaperQuestionEntity::getPaperId, paperId)) == 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "试卷至少需要一道题目");
        }
        if (examPaperMapper.selectCount(Wrappers.<ExamPaperEntity>lambdaQuery()
                        .eq(ExamPaperEntity::getExamId, paper.getExamId())
                        .eq(ExamPaperEntity::getStatus, ExamPaperStatus.PUBLISHED.name())
                        .ne(ExamPaperEntity::getId, paperId)) > 0) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "一个考试只能发布一份试卷");
        }
        paper.setStatus(ExamPaperStatus.PUBLISHED.name());
        updateOrConflict(examPaperMapper.updateById(paper), "试卷草稿已被其他请求修改，请刷新后重试");
        exam.setStatus(ExamStatus.PUBLISHED.name());
        updateOrConflict(examMapper.updateById(exam), "考试已被其他请求修改，请刷新后重试");
        return toExamPaper(paper);
    }

    @Transactional
    public void deletePaper(Long teacherId, Long paperId) {
        ExamPaperEntity paper = requirePaper(paperId);
        requireTeacherCourse(teacherId, paper.getCourseId());
        requireDraftPaper(paper);
        examPaperQuestionMapper.deletePhysicalByPaperId(paperId);
        examPaperMapper.deleteById(paperId);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentExamListItemVO> listStudentExams(Long studentId, Long courseId, ExamListQuery query) {
        requireStudentCourse(studentId, courseId);
        LambdaQueryWrapper<ExamEntity> wrapper = Wrappers.<ExamEntity>lambdaQuery()
                .eq(ExamEntity::getCourseId, courseId)
                .eq(ExamEntity::getStatus, ExamStatus.PUBLISHED.name());
        if (hasText(query.getKeyword())) {
            wrapper.like(ExamEntity::getTitle, query.getKeyword().trim());
        }
        var page = examMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper
                .orderByAsc(ExamEntity::getStartAt)
                .orderByDesc(ExamEntity::getId));
        List<StudentExamListItemVO> records = page.getRecords().stream()
                .map(this::toStudentExam)
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private void replaceQuestionOptions(Long questionId, List<QuestionOptionRequest> options) {
        questionOptionMapper.deletePhysicalByQuestionId(questionId);
        if (options == null) {
            return;
        }
        for (QuestionOptionRequest option : options) {
            QuestionOptionEntity entity = new QuestionOptionEntity();
            entity.setQuestionId(questionId);
            entity.setOptionLabel(option.label().trim());
            entity.setOptionContent(option.content().trim());
            entity.setIsCorrect(Boolean.TRUE.equals(option.correct()) ? 1 : 0);
            entity.setSortOrder(option.sortOrder());
            questionOptionMapper.insert(entity);
        }
    }

    private void replacePaperQuestions(Long paperId, List<ExamPaperQuestionRequest> requests) {
        examPaperQuestionMapper.deletePhysicalByPaperId(paperId);
        for (ExamPaperQuestionRequest request : requests) {
            ExamPaperQuestionEntity entity = new ExamPaperQuestionEntity();
            entity.setPaperId(paperId);
            entity.setQuestionId(request.questionId());
            entity.setQuestionOrder(request.questionOrder());
            entity.setScore(request.score());
            examPaperQuestionMapper.insert(entity);
        }
    }

    private void validateOptions(QuestionType type, List<QuestionOptionRequest> options) {
        List<QuestionOptionRequest> values = options == null ? List.of() : options;
        Set<String> labels = new HashSet<>();
        for (QuestionOptionRequest option : values) {
            if (!labels.add(option.label().trim().toUpperCase())) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "同一道题目的选项标签不能重复");
            }
        }
        long correctCount = values.stream().filter(option -> Boolean.TRUE.equals(option.correct())).count();
        boolean valid = switch (type) {
            case SINGLE_CHOICE -> values.size() >= 2 && correctCount == 1;
            case MULTIPLE_CHOICE -> values.size() >= 2 && correctCount >= 2;
            case TRUE_FALSE -> values.size() == 2 && correctCount == 1;
            case SHORT_ANSWER -> values.isEmpty();
        };
        if (!valid) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "题型与选项、正确答案数量不匹配");
        }
    }

    private void validatePaperQuestions(Long courseId, List<ExamPaperQuestionRequest> requests) {
        Set<Long> questionIds = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (ExamPaperQuestionRequest request : requests) {
            if (!questionIds.add(request.questionId()) || !orders.add(request.questionOrder())) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "试卷题目和题目顺序不能重复");
            }
            QuestionEntity question = requireQuestion(request.questionId());
            if (!courseId.equals(question.getCourseId())) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "试卷题目必须属于同一门课程");
            }
            if (!QuestionStatus.ACTIVE.name().equals(question.getStatus())) {
                throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "试卷只能使用可用题目");
            }
        }
    }

    private void validateExamTimes(OffsetDateTime startAt, OffsetDateTime endAt, Integer durationMinutes) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "考试结束时间必须晚于开始时间");
        }
        if (startAt != null && endAt != null && durationMinutes != null
                && endAt.toInstant().minusSeconds(durationMinutes.longValue() * 60).isBefore(startAt.toInstant())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "考试时长不能超过考试时间窗口");
        }
    }

    private boolean isQuestionInPublishedPaper(Long questionId) {
        List<Long> paperIds = examPaperQuestionMapper.selectList(Wrappers.<ExamPaperQuestionEntity>lambdaQuery()
                        .eq(ExamPaperQuestionEntity::getQuestionId, questionId))
                .stream()
                .map(ExamPaperQuestionEntity::getPaperId)
                .toList();
        return !paperIds.isEmpty()
                && examPaperMapper.selectCount(Wrappers.<ExamPaperEntity>lambdaQuery()
                                .in(ExamPaperEntity::getId, paperIds)
                                .eq(ExamPaperEntity::getStatus, ExamPaperStatus.PUBLISHED.name()))
                        > 0;
    }

    private boolean isQuestionReferencedByAnyPaper(Long questionId) {
        return examPaperQuestionMapper.selectCount(Wrappers.<ExamPaperQuestionEntity>lambdaQuery()
                        .eq(ExamPaperQuestionEntity::getQuestionId, questionId))
                > 0;
    }

    private QuestionBankEntity requireQuestionBank(Long bankId) {
        QuestionBankEntity bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "题库不存在");
        }
        return bank;
    }

    private QuestionEntity requireQuestion(Long questionId) {
        QuestionEntity question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private ExamEntity requireExam(Long examId) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private ExamPaperEntity requirePaper(Long paperId) {
        ExamPaperEntity paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "试卷不存在");
        }
        return paper;
    }

    private void requireTeacherCourse(Long teacherId, Long courseId) {
        if (coursePermissionService.canEditCourseContent(teacherId, courseId)) {
            return;
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN, "你不属于该课程教师团队");
    }

    private void requireStudentCourse(Long studentId, Long courseId) {
        if (coursePermissionService.canViewCourseAsStudent(studentId, courseId)) {
            return;
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN, "你尚未选修该课程或课程不可学习");
    }

    private void requireActiveBank(QuestionBankEntity bank) {
        if (!QuestionBankStatus.ACTIVE.name().equals(bank.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已归档题库不能新增题目");
        }
    }

    private void requireDraftExam(ExamEntity exam) {
        if (!ExamStatus.DRAFT.name().equals(exam.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已发布或已关闭考试不能修改");
        }
    }

    private void requireDraftPaper(ExamPaperEntity paper) {
        if (!ExamPaperStatus.DRAFT.name().equals(paper.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已发布试卷不能修改");
        }
    }

    private void updateOrConflict(int updated, String message) {
        if (updated != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, message);
        }
    }

    private QuestionBankVO toQuestionBank(QuestionBankEntity bank) {
        return new QuestionBankVO(
                String.valueOf(bank.getId()),
                String.valueOf(bank.getCourseId()),
                bank.getName(),
                bank.getDescription(),
                bank.getStatus(),
                bank.getVersion());
    }

    private QuestionVO toQuestion(QuestionEntity question) {
        List<QuestionOptionVO> options = questionOptionMapper.selectList(Wrappers.<QuestionOptionEntity>lambdaQuery()
                        .eq(QuestionOptionEntity::getQuestionId, question.getId())
                        .orderByAsc(QuestionOptionEntity::getSortOrder)
                        .orderByAsc(QuestionOptionEntity::getId))
                .stream()
                .map(option -> new QuestionOptionVO(
                        String.valueOf(option.getId()),
                        option.getOptionLabel(),
                        option.getOptionContent(),
                        option.getIsCorrect() != null && option.getIsCorrect() == 1,
                        option.getSortOrder()))
                .toList();
        return new QuestionVO(
                String.valueOf(question.getId()),
                String.valueOf(question.getBankId()),
                String.valueOf(question.getCourseId()),
                question.getQuestionType(),
                question.getStem(),
                question.getAnalysis(),
                question.getDifficulty(),
                question.getScore(),
                question.getStatus(),
                options,
                question.getVersion());
    }

    private ExamVO toExam(ExamEntity exam) {
        return new ExamVO(
                String.valueOf(exam.getId()),
                String.valueOf(exam.getCourseId()),
                exam.getTitle(),
                exam.getDescription(),
                exam.getStatus(),
                offset(exam.getStartAt()),
                offset(exam.getEndAt()),
                exam.getDurationMinutes(),
                exam.getTotalScore(),
                exam.getVersion());
    }

    private ExamPaperVO toExamPaper(ExamPaperEntity paper) {
        List<ExamPaperQuestionVO> questions = examPaperQuestionMapper.selectList(Wrappers.<ExamPaperQuestionEntity>lambdaQuery()
                        .eq(ExamPaperQuestionEntity::getPaperId, paper.getId())
                        .orderByAsc(ExamPaperQuestionEntity::getQuestionOrder)
                        .orderByAsc(ExamPaperQuestionEntity::getId))
                .stream()
                .map(item -> {
                    QuestionEntity question = requireQuestion(item.getQuestionId());
                    return new ExamPaperQuestionVO(
                            String.valueOf(item.getQuestionId()),
                            item.getQuestionOrder(),
                            item.getScore(),
                            question.getQuestionType(),
                            question.getStem());
                })
                .toList();
        return new ExamPaperVO(
                String.valueOf(paper.getId()),
                String.valueOf(paper.getExamId()),
                String.valueOf(paper.getCourseId()),
                paper.getTitle(),
                paper.getTotalScore(),
                paper.getStatus(),
                questions,
                paper.getVersion());
    }

    private StudentExamListItemVO toStudentExam(ExamEntity exam) {
        return new StudentExamListItemVO(
                String.valueOf(exam.getId()),
                String.valueOf(exam.getCourseId()),
                exam.getTitle(),
                exam.getDescription(),
                offset(exam.getStartAt()),
                offset(exam.getEndAt()),
                exam.getDurationMinutes(),
                exam.getTotalScore());
    }

    private BigDecimal totalScore(List<ExamPaperQuestionRequest> questions) {
        return questions.stream().map(ExamPaperQuestionRequest::score).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private LocalDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
