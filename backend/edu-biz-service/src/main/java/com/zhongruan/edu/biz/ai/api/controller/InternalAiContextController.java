package com.zhongruan.edu.biz.ai.api.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BizAiContextFeignClient.BASE_PATH)
public class InternalAiContextController implements BizAiContextFeignClient {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String ENROLLED = "ENROLLED";
    private static final String PUBLISHED = "PUBLISHED";

    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CourseLessonMapper courseLessonMapper;
    private final CourseMaterialMapper courseMaterialMapper;
    private final HttpServletRequest servletRequest;

    public InternalAiContextController(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            CourseLessonMapper courseLessonMapper,
            CourseMaterialMapper courseMaterialMapper,
            HttpServletRequest servletRequest) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.courseLessonMapper = courseLessonMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.servletRequest = servletRequest;
    }

    @Override
    public ApiResponse<AiCourseContextResponse> getCourseContext(
            @Valid @RequestBody AiCourseContextRequest request) {
        CourseEntity course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        boolean teacherMember = isTeacherMember(request.userId(), request.courseId());
        boolean enrolled = isEnrolled(request.userId(), request.courseId());
        boolean admin = ADMIN_ROLE.equals(request.roleCode());

        if (!admin && !teacherMember && !enrolled) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (STUDENT_ROLE.equals(request.roleCode()) && !PUBLISHED.equals(course.getStatus())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (TEACHER_ROLE.equals(request.roleCode()) && !teacherMember) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        boolean includeDraft = admin || teacherMember;
        AiCourseContextResponse response = new AiCourseContextResponse(
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                course.getStatus(),
                course.getReviewStatus(),
                course.getOwnerTeacherId(),
                teacherMember,
                enrolled,
                lessons(request.courseId(), includeDraft),
                materials(request.courseId(), includeDraft));

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
                .eq(CourseEnrollmentEntity::getStatus, ENROLLED));
        return count != null && count > 0;
    }

    private List<AiLessonRef> lessons(Long courseId, boolean includeDraft) {
        var query = Wrappers.<CourseLessonEntity>lambdaQuery()
                .eq(CourseLessonEntity::getCourseId, courseId)
                .orderByAsc(CourseLessonEntity::getChapterId)
                .orderByAsc(CourseLessonEntity::getSortOrder)
                .orderByAsc(CourseLessonEntity::getId);
        if (!includeDraft) {
            query.eq(CourseLessonEntity::getStatus, PUBLISHED);
        }
        return courseLessonMapper.selectList(query).stream()
                .map(lesson -> new AiLessonRef(
                        lesson.getId(),
                        lesson.getChapterId(),
                        lesson.getTitle(),
                        lesson.getStatus(),
                        lesson.getContentType(),
                        lesson.getEstimatedMinutes()))
                .toList();
    }

    private List<AiMaterialRef> materials(Long courseId, boolean includeDraft) {
        var query = Wrappers.<CourseMaterialEntity>lambdaQuery()
                .eq(CourseMaterialEntity::getCourseId, courseId)
                .orderByAsc(CourseMaterialEntity::getSortOrder)
                .orderByAsc(CourseMaterialEntity::getId);
        if (!includeDraft) {
            query.eq(CourseMaterialEntity::getStatus, PUBLISHED);
        }
        return courseMaterialMapper.selectList(query).stream()
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
}
