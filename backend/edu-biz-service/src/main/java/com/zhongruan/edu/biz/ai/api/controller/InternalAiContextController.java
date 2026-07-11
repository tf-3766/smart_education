package com.zhongruan.edu.biz.ai.api.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestTrace;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiCourseContextRequest;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
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
    private final HttpServletRequest servletRequest;

    public InternalAiContextController(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            CourseLessonMapper courseLessonMapper,
            CourseMaterialMapper courseMaterialMapper,
            CoursePermissionService coursePermissionService,
            HttpServletRequest servletRequest) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.courseLessonMapper = courseLessonMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.coursePermissionService = coursePermissionService;
        this.servletRequest = servletRequest;
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
                .map(material -> new AiMaterialRef(
                        material.getId(),
                        material.getChapterId(),
                        material.getLessonId(),
                        material.getName(),
                        material.getMaterialType(),
                        material.getFileKey(),
                        material.getFileUrl(),
                        material.getVisibility(),
                        material.getStatus()))
                .toList();
    }

    private AuthenticatedUser authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
