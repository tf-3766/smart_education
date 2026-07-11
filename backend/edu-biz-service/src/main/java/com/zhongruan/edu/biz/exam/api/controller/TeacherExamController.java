package com.zhongruan.edu.biz.exam.api.controller;

import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.SystemPermission;
import com.zhongruan.edu.biz.exam.api.dto.query.ExamListQuery;
import com.zhongruan.edu.biz.exam.api.dto.query.ExamAttemptListQuery;
import com.zhongruan.edu.biz.exam.api.dto.query.QuestionBankListQuery;
import com.zhongruan.edu.biz.exam.api.dto.query.QuestionListQuery;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateExamPaperRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateExamRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionBankRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.CreateQuestionRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.GradeExamAttemptRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamAttemptVO;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateExamPaperRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateExamRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateQuestionBankRequest;
import com.zhongruan.edu.biz.exam.api.dto.request.UpdateQuestionRequest;
import com.zhongruan.edu.biz.exam.api.vo.ExamPaperVO;
import com.zhongruan.edu.biz.exam.api.vo.ExamVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionBankVO;
import com.zhongruan.edu.biz.exam.api.vo.QuestionVO;
import com.zhongruan.edu.biz.exam.application.service.ExamManagementService;
import com.zhongruan.edu.biz.exam.application.service.ExamParticipationService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole(T(com.zhongruan.edu.biz.auth.domain.enums.RoleCode).TEACHER.name())"
        + " and hasAuthority(T(com.zhongruan.edu.biz.auth.domain.enums.SystemPermission).TEACHER_ACCESS.code())")
public class TeacherExamController {
    private final ExamManagementService service;
    private final ExamParticipationService participationService;
    private final RequestContextFactory contextFactory;

    public TeacherExamController(
            ExamManagementService service,
            ExamParticipationService participationService,
            RequestContextFactory contextFactory) {
        this.service = service;
        this.participationService = participationService;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/question-banks")
    public ApiResponse<PageResponse<QuestionBankVO>> listQuestionBanks(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid QuestionBankListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listQuestionBanks(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/question-banks")
    public ResponseEntity<ApiResponse<QuestionBankVO>> createQuestionBank(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateQuestionBankRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createQuestionBank(user.userId(), courseId, body), trace(request)));
    }

    @GetMapping("/question-banks/{bankId}")
    public ApiResponse<QuestionBankVO> getQuestionBank(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bankId, HttpServletRequest request) {
        return ApiResponse.success(service.getQuestionBank(user.userId(), bankId), trace(request));
    }

    @PutMapping("/question-banks/{bankId}")
    public ApiResponse<QuestionBankVO> updateQuestionBank(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bankId,
            @Valid @RequestBody UpdateQuestionBankRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateQuestionBank(user.userId(), bankId, body), trace(request));
    }

    @DeleteMapping("/question-banks/{bankId}")
    public ApiResponse<Void> deleteQuestionBank(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bankId, HttpServletRequest request) {
        service.deleteQuestionBank(user.userId(), bankId);
        return ApiResponse.success(trace(request));
    }

    @GetMapping("/question-banks/{bankId}/questions")
    public ApiResponse<PageResponse<QuestionVO>> listQuestions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bankId,
            @Valid QuestionListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listQuestions(user.userId(), bankId, query), trace(request));
    }

    @PostMapping("/question-banks/{bankId}/questions")
    public ResponseEntity<ApiResponse<QuestionVO>> createQuestion(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bankId,
            @Valid @RequestBody CreateQuestionRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createQuestion(user.userId(), bankId, body), trace(request)));
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<QuestionVO> getQuestion(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long questionId, HttpServletRequest request) {
        return ApiResponse.success(service.getQuestion(user.userId(), questionId), trace(request));
    }

    @PutMapping("/questions/{questionId}")
    public ApiResponse<QuestionVO> updateQuestion(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateQuestion(user.userId(), questionId, body), trace(request));
    }

    @DeleteMapping("/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long questionId, HttpServletRequest request) {
        service.deleteQuestion(user.userId(), questionId);
        return ApiResponse.success(trace(request));
    }

    @GetMapping("/courses/{courseId}/exams")
    public ApiResponse<PageResponse<ExamVO>> listExams(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid ExamListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listTeacherExams(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/exams")
    public ResponseEntity<ApiResponse<ExamVO>> createExam(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateExamRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createExam(user.userId(), courseId, body), trace(request)));
    }

    @GetMapping("/exams/{examId}")
    public ApiResponse<ExamVO> getExam(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId, HttpServletRequest request) {
        return ApiResponse.success(service.getExam(user.userId(), examId), trace(request));
    }

    @PutMapping("/exams/{examId}")
    public ApiResponse<ExamVO> updateExam(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long examId,
            @Valid @RequestBody UpdateExamRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateExam(user.userId(), examId, body), trace(request));
    }

    @DeleteMapping("/exams/{examId}")
    public ApiResponse<Void> deleteExam(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId, HttpServletRequest request) {
        service.deleteExam(user.userId(), examId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/exams/{examId}/papers")
    public ResponseEntity<ApiResponse<ExamPaperVO>> createPaper(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long examId,
            @Valid @RequestBody CreateExamPaperRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createPaper(user.userId(), examId, body), trace(request)));
    }

    @GetMapping("/exam-papers/{paperId}")
    public ApiResponse<ExamPaperVO> getPaper(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long paperId, HttpServletRequest request) {
        return ApiResponse.success(service.getPaper(user.userId(), paperId), trace(request));
    }

    @PutMapping("/exam-papers/{paperId}")
    public ApiResponse<ExamPaperVO> updatePaper(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long paperId,
            @Valid @RequestBody UpdateExamPaperRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updatePaper(user.userId(), paperId, body), trace(request));
    }

    @PostMapping("/exam-papers/{paperId}/publish")
    public ApiResponse<ExamPaperVO> publishPaper(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long paperId, HttpServletRequest request) {
        return ApiResponse.success(service.publishPaper(user.userId(), paperId), trace(request));
    }

    @DeleteMapping("/exam-papers/{paperId}")
    public ApiResponse<Void> deletePaper(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long paperId, HttpServletRequest request) {
        service.deletePaper(user.userId(), paperId);
        return ApiResponse.success(trace(request));
    }

    @GetMapping("/exams/{examId}/attempts")
    public ApiResponse<PageResponse<ExamAttemptVO>> listAttempts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long examId,
            @Valid ExamAttemptListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(
                participationService.listForTeacher(user.userId(), examId, query), trace(request));
    }

    @PostMapping("/exam-attempts/{attemptId}/grade")
    public ApiResponse<ExamAttemptVO> gradeAttempt(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long attemptId,
            @Valid @RequestBody GradeExamAttemptRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(
                participationService.grade(user.userId(), attemptId, body), trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
