package com.zhongruan.edu.biz.platform.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import com.zhongruan.edu.biz.notification.application.service.NotificationApplicationService;
import com.zhongruan.edu.biz.platform.api.dto.query.AnnouncementListQuery;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.WithdrawAnnouncementRequest;
import com.zhongruan.edu.biz.platform.api.vo.AnnouncementVO;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementAudience;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementScope;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementStatus;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.AnnouncementEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.AnnouncementMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementApplicationService {
    private final AnnouncementMapper announcementMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CoursePermissionService coursePermissionService;
    private final NotificationApplicationService notificationService;
    private final Clock clock = Clock.systemUTC();

    public AnnouncementApplicationService(
            AnnouncementMapper announcementMapper,
            CourseEnrollmentMapper enrollmentMapper,
            CourseTeacherMapper courseTeacherMapper,
            CoursePermissionService coursePermissionService,
            NotificationApplicationService notificationService) {
        this.announcementMapper = announcementMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.coursePermissionService = coursePermissionService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementVO createCourseAnnouncement(
            Long teacherId, Long courseId, CreateAnnouncementRequest request) {
        requireTeacherCourse(teacherId, courseId);
        if (request.audience() == AnnouncementAudience.TEACHER) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "课程公告受众只能是学生或全部成员");
        }
        return create(teacherId, AnnouncementScope.COURSE, courseId, request);
    }

    @Transactional
    public AnnouncementVO createSystemAnnouncement(Long administratorId, CreateAnnouncementRequest request) {
        return create(administratorId, AnnouncementScope.SYSTEM, null, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementVO> listCourse(
            Long teacherId, Long courseId, AnnouncementListQuery query) {
        requireTeacherCourse(teacherId, courseId);
        return page(Wrappers.<AnnouncementEntity>lambdaQuery()
                .eq(AnnouncementEntity::getScopeType, AnnouncementScope.COURSE.name())
                .eq(AnnouncementEntity::getCourseId, courseId), query);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementVO> listForAdministration(AnnouncementListQuery query) {
        return page(Wrappers.lambdaQuery(AnnouncementEntity.class), query);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementVO> listForStudent(Long studentId, AnnouncementListQuery query) {
        List<Long> courseIds = enrollmentMapper.selectList(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                        .eq(CourseEnrollmentEntity::getStudentId, studentId)
                        .in(CourseEnrollmentEntity::getStatus,
                                EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()))
                .stream()
                .map(CourseEnrollmentEntity::getCourseId)
                .toList();
        LambdaQueryWrapper<AnnouncementEntity> wrapper = Wrappers.<AnnouncementEntity>lambdaQuery()
                .eq(AnnouncementEntity::getStatus, AnnouncementStatus.PUBLISHED.name())
                .in(AnnouncementEntity::getAudience,
                        AnnouncementAudience.ALL.name(), AnnouncementAudience.STUDENT.name());
        wrapper.and(scope -> {
            scope.eq(AnnouncementEntity::getScopeType, AnnouncementScope.SYSTEM.name());
            if (!courseIds.isEmpty()) {
                scope.or(course -> course
                        .eq(AnnouncementEntity::getScopeType, AnnouncementScope.COURSE.name())
                        .in(AnnouncementEntity::getCourseId, courseIds));
            }
        });
        return page(wrapper, query);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementVO> listForTeacher(Long teacherId, AnnouncementListQuery query) {
        List<Long> courseIds = courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getTeacherId, teacherId))
                .stream()
                .map(CourseTeacherEntity::getCourseId)
                .toList();
        LambdaQueryWrapper<AnnouncementEntity> wrapper = Wrappers.<AnnouncementEntity>lambdaQuery()
                .eq(AnnouncementEntity::getStatus, AnnouncementStatus.PUBLISHED.name())
                .in(AnnouncementEntity::getAudience,
                        AnnouncementAudience.ALL.name(), AnnouncementAudience.TEACHER.name());
        wrapper.and(scope -> {
            scope.eq(AnnouncementEntity::getScopeType, AnnouncementScope.SYSTEM.name());
            if (!courseIds.isEmpty()) {
                scope.or(course -> course
                        .eq(AnnouncementEntity::getScopeType, AnnouncementScope.COURSE.name())
                        .in(AnnouncementEntity::getCourseId, courseIds));
            }
        });
        return page(wrapper, query);
    }

    @Transactional
    public AnnouncementVO withdrawCourseAnnouncement(
            Long teacherId, Long announcementId, WithdrawAnnouncementRequest request) {
        AnnouncementEntity announcement = requireAnnouncement(announcementId);
        if (!AnnouncementScope.COURSE.name().equals(announcement.getScopeType())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该公告不是课程公告");
        }
        requireTeacherCourse(teacherId, announcement.getCourseId());
        return withdraw(announcement, request.version());
    }

    @Transactional
    public AnnouncementVO withdrawSystemAnnouncement(Long announcementId, WithdrawAnnouncementRequest request) {
        AnnouncementEntity announcement = requireAnnouncement(announcementId);
        if (!AnnouncementScope.SYSTEM.name().equals(announcement.getScopeType())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该公告不是系统公告");
        }
        return withdraw(announcement, request.version());
    }

    private AnnouncementVO create(
            Long publisherId,
            AnnouncementScope scope,
            Long courseId,
            CreateAnnouncementRequest request) {
        AnnouncementEntity announcement = new AnnouncementEntity();
        announcement.setScopeType(scope.name());
        announcement.setCourseId(courseId);
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setAudience(request.audience().name());
        announcement.setStatus(AnnouncementStatus.PUBLISHED.name());
        announcement.setPublishedAt(LocalDateTime.now(clock));
        announcement.setCreatedBy(publisherId);
        announcementMapper.insert(announcement);
        notificationService.publishAnnouncement(announcement);
        return toVO(announcement);
    }

    private AnnouncementVO withdraw(AnnouncementEntity announcement, Integer version) {
        if (AnnouncementStatus.WITHDRAWN.name().equals(announcement.getStatus())) {
            return toVO(announcement);
        }
        announcement.setStatus(AnnouncementStatus.WITHDRAWN.name());
        announcement.setWithdrawnAt(LocalDateTime.now(clock));
        announcement.setVersion(version);
        if (announcementMapper.updateById(announcement) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        notificationService.withdrawAnnouncement(announcement.getId());
        return toVO(announcement);
    }

    private PageResponse<AnnouncementVO> page(
            LambdaQueryWrapper<AnnouncementEntity> wrapper, AnnouncementListQuery query) {
        var page = announcementMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                wrapper.orderByDesc(AnnouncementEntity::getPublishedAt)
                        .orderByDesc(AnnouncementEntity::getId));
        return PageResponse.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    private AnnouncementEntity requireAnnouncement(Long announcementId) {
        AnnouncementEntity announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "公告不存在");
        }
        return announcement;
    }

    private void requireTeacherCourse(Long teacherId, Long courseId) {
        if (!coursePermissionService.canEditCourseContent(teacherId, courseId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你不属于该课程教师团队");
        }
    }

    private AnnouncementVO toVO(AnnouncementEntity announcement) {
        return new AnnouncementVO(
                String.valueOf(announcement.getId()),
                announcement.getScopeType(),
                announcement.getCourseId() == null ? null : String.valueOf(announcement.getCourseId()),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAudience(),
                announcement.getStatus(),
                announcement.getPublishedAt() == null ? null : announcement.getPublishedAt().atOffset(ZoneOffset.UTC),
                announcement.getWithdrawnAt() == null ? null : announcement.getWithdrawnAt().atOffset(ZoneOffset.UTC),
                announcement.getCreatedBy() == null ? null : String.valueOf(announcement.getCreatedBy()),
                announcement.getVersion());
    }
}
