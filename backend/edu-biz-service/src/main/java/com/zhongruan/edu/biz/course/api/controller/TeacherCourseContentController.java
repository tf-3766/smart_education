package com.zhongruan.edu.biz.course.api.controller;

import com.zhongruan.edu.biz.course.api.dto.query.CourseMaterialListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.CreateChapterRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateCourseMaterialRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateLessonRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateChapterRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateCourseMaterialRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateLessonRequest;
import com.zhongruan.edu.biz.course.api.vo.ChapterDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseMaterialVO;
import com.zhongruan.edu.biz.course.api.vo.LessonDetailVO;
import com.zhongruan.edu.biz.course.application.service.CourseContentService;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
public class TeacherCourseContentController {
    private final CourseContentService service;
    private final RequestContextFactory contextFactory;

    public TeacherCourseContentController(CourseContentService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ApiResponse<List<ChapterDetailVO>> chapters(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long courseId, HttpServletRequest request) {
        return ApiResponse.success(service.listChapters(user.userId(), courseId), trace(request));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<ChapterDetailVO>> createChapter(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateChapterRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createChapter(user.userId(), courseId, body), trace(request)));
    }

    @PutMapping("/chapters/{chapterId}")
    public ApiResponse<ChapterDetailVO> updateChapter(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long chapterId,
            @Valid @RequestBody UpdateChapterRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateChapter(user.userId(), chapterId, body), trace(request));
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ApiResponse<Void> deleteChapter(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long chapterId, HttpServletRequest request) {
        service.deleteChapter(user.userId(), chapterId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/chapters/{chapterId}/publish")
    public ApiResponse<ChapterDetailVO> publishChapter(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long chapterId, HttpServletRequest request) {
        return ApiResponse.success(service.publishChapter(user.userId(), chapterId), trace(request));
    }

    @PostMapping("/chapters/{chapterId}/offline")
    public ApiResponse<ChapterDetailVO> offlineChapter(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long chapterId, HttpServletRequest request) {
        return ApiResponse.success(service.offlineChapter(user.userId(), chapterId), trace(request));
    }

    @GetMapping("/chapters/{chapterId}/lessons")
    public ApiResponse<List<LessonDetailVO>> lessons(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long chapterId, HttpServletRequest request) {
        return ApiResponse.success(service.listLessons(user.userId(), chapterId), trace(request));
    }

    @PostMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<ApiResponse<LessonDetailVO>> createLesson(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long chapterId,
            @Valid @RequestBody CreateLessonRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createLesson(user.userId(), chapterId, body), trace(request)));
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<LessonDetailVO> lesson(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.getLesson(user.userId(), lessonId), trace(request));
    }

    @PutMapping("/lessons/{lessonId}")
    public ApiResponse<LessonDetailVO> updateLesson(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateLesson(user.userId(), lessonId, body), trace(request));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ApiResponse<Void> deleteLesson(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        service.deleteLesson(user.userId(), lessonId);
        return ApiResponse.success(trace(request));
    }

    @PostMapping("/lessons/{lessonId}/publish")
    public ApiResponse<LessonDetailVO> publishLesson(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.publishLesson(user.userId(), lessonId), trace(request));
    }

    @PostMapping("/lessons/{lessonId}/offline")
    public ApiResponse<LessonDetailVO> offlineLesson(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId, HttpServletRequest request) {
        return ApiResponse.success(service.offlineLesson(user.userId(), lessonId), trace(request));
    }

    @GetMapping("/courses/{courseId}/materials")
    public ApiResponse<PageResponse<CourseMaterialVO>> materials(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid CourseMaterialListQuery query,
            HttpServletRequest request) {
        return ApiResponse.success(service.listMaterials(user.userId(), courseId, query), trace(request));
    }

    @PostMapping("/courses/{courseId}/materials")
    public ResponseEntity<ApiResponse<CourseMaterialVO>> createMaterial(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseMaterialRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createMaterial(user.userId(), courseId, body), trace(request)));
    }

    @PutMapping("/materials/{materialId}")
    public ApiResponse<CourseMaterialVO> updateMaterial(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long materialId,
            @Valid @RequestBody UpdateCourseMaterialRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateMaterial(user.userId(), materialId, body), trace(request));
    }

    @DeleteMapping("/materials/{materialId}")
    public ApiResponse<Void> deleteMaterial(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long materialId, HttpServletRequest request) {
        service.deleteMaterial(user.userId(), materialId);
        return ApiResponse.success(trace(request));
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
