package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonUnlockType;
import com.zhongruan.edu.biz.course.domain.enums.MaterialStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialVisibility;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseChapterMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class CoursePermissionService {
    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseMaterialMapper materialMapper;
    private final Clock clock;

    public CoursePermissionService(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseEnrollmentMapper enrollmentMapper,
            CourseChapterMapper chapterMapper,
            CourseLessonMapper lessonMapper,
            CourseMaterialMapper materialMapper) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.materialMapper = materialMapper;
        this.clock = Clock.systemUTC();
    }

    public boolean canManageCourse(Long userId, Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        return course != null && userId.equals(course.getOwnerTeacherId());
    }

    public boolean canEditCourseContent(Long userId, Long courseId) {
        return courseTeacherMapper.selectCount(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getCourseId, courseId)
                        .eq(CourseTeacherEntity::getTeacherId, userId))
                > 0;
    }

    public boolean canViewCourseAsStudent(Long userId, Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        return course != null
                && CourseReviewStatus.APPROVED.name().equals(course.getReviewStatus())
                && CourseStatus.valueOf(course.getStatus()).isStudentVisible()
                && activeEnrollment(userId, courseId) != null;
    }

    public boolean canEnrollCourse(Long studentId, Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null
                || !CourseReviewStatus.APPROVED.name().equals(course.getReviewStatus())
                || !CourseStatus.valueOf(course.getStatus()).allowsNewEnrollment()) {
            return false;
        }
        LocalDateTime now = now();
        return (course.getEnrollmentOpenAt() == null || !now.isBefore(course.getEnrollmentOpenAt()))
                && (course.getEnrollmentCloseAt() == null || !now.isAfter(course.getEnrollmentCloseAt()));
    }

    public boolean canAccessLesson(Long studentId, Long lessonId) {
        CourseLessonEntity lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || !LessonStatus.PUBLISHED.name().equals(lesson.getStatus())) {
            return false;
        }
        CourseChapterEntity chapter = chapterMapper.selectById(lesson.getChapterId());
        if (chapter == null
                || !lesson.getCourseId().equals(chapter.getCourseId())
                || !ChapterStatus.PUBLISHED.name().equals(chapter.getStatus())) {
            return false;
        }
        return canViewCourseAsStudent(studentId, lesson.getCourseId()) && isUnlocked(lesson);
    }

    public boolean canAccessMaterial(Long userId, Long materialId) {
        CourseMaterialEntity material = materialMapper.selectById(materialId);
        if (material == null) {
            return false;
        }
        if (canEditCourseContent(userId, material.getCourseId())) {
            return true;
        }
        if (!MaterialStatus.PUBLISHED.name().equals(material.getStatus())) {
            return false;
        }
        if (!canViewCourseAsStudent(userId, material.getCourseId())) {
            return false;
        }
        MaterialVisibility visibility = MaterialVisibility.valueOf(material.getVisibility());
        if (visibility == MaterialVisibility.COURSE) {
            return true;
        }
        CourseChapterEntity chapter = material.getChapterId() == null ? null : chapterMapper.selectById(material.getChapterId());
        if (chapter == null
                || !material.getCourseId().equals(chapter.getCourseId())
                || !ChapterStatus.PUBLISHED.name().equals(chapter.getStatus())) {
            return false;
        }
        return visibility == MaterialVisibility.CHAPTER
                || (material.getLessonId() != null && canAccessLesson(userId, material.getLessonId()));
    }

    public CourseEnrollmentEntity activeEnrollment(Long studentId, Long courseId) {
        return enrollmentMapper.selectOne(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getStudentId, studentId)
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .in(CourseEnrollmentEntity::getStatus, EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()));
    }

    public boolean isUnlocked(CourseLessonEntity lesson) {
        return LessonUnlockType.IMMEDIATE.name().equals(lesson.getUnlockType())
                || (lesson.getUnlockAt() != null && !now().isBefore(lesson.getUnlockAt()));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
