package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.api.vo.EnrollmentVO;
import com.zhongruan.edu.biz.course.api.vo.StudentCourseListItemVO;
import com.zhongruan.edu.biz.course.application.assembler.CourseAssembler;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCourseService {
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final CoursePermissionService permissionService;
    private final CourseManagementService courseManagementService;
    private final CourseAssembler assembler;
    private final Clock clock = Clock.systemUTC();

    public StudentCourseService(
            CourseMapper courseMapper,
            CourseEnrollmentMapper enrollmentMapper,
            UserMapper userMapper,
            CoursePermissionService permissionService,
            CourseManagementService courseManagementService,
            CourseAssembler assembler) {
        this.courseMapper = courseMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.courseManagementService = courseManagementService;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCourseListItemVO> catalog(Long studentId, CourseListQuery query) {
        LocalDateTime now = now();
        var wrapper = Wrappers.<CourseEntity>lambdaQuery()
                .eq(CourseEntity::getReviewStatus, CourseReviewStatus.APPROVED.name())
                .in(CourseEntity::getStatus, CourseStatus.PUBLISHED.name(), CourseStatus.ONGOING.name())
                .and(group -> group.isNull(CourseEntity::getEnrollmentOpenAt)
                        .or()
                        .le(CourseEntity::getEnrollmentOpenAt, now))
                .and(group -> group.isNull(CourseEntity::getEnrollmentCloseAt)
                        .or()
                        .ge(CourseEntity::getEnrollmentCloseAt, now));
        applyFilters(wrapper, query);
        wrapper.orderByAsc(CourseEntity::getStartAt).orderByDesc(CourseEntity::getId);
        IPage<CourseEntity> page = courseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, CourseEnrollmentEntity> enrollments = enrollmentMap(studentId, page.getRecords());
        List<StudentCourseListItemVO> records = page.getRecords().stream()
                .map(course -> {
                    CourseEnrollmentEntity enrollment = enrollments.get(course.getId());
                    boolean enrollable = enrollment == null && permissionService.canEnrollCourse(studentId, course.getId());
                    return assembler.toStudentListItem(
                            course, userName(course.getOwnerTeacherId()), enrollment, enrollable);
                })
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCourseListItemVO> myCourses(Long studentId, CourseListQuery query) {
        var enrollmentQuery = Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getStudentId, studentId);
        if (query.getEnrollmentStatus() != null) {
            enrollmentQuery.eq(CourseEnrollmentEntity::getStatus, query.getEnrollmentStatus().name());
        }
        List<CourseEnrollmentEntity> enrollments = enrollmentMapper.selectList(enrollmentQuery);
        if (enrollments.isEmpty()) {
            return PageResponse.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        Map<Long, CourseEnrollmentEntity> byCourse = enrollments.stream()
                .collect(Collectors.toMap(CourseEnrollmentEntity::getCourseId, Function.identity()));
        var wrapper = Wrappers.<CourseEntity>lambdaQuery().in(CourseEntity::getId, byCourse.keySet());
        applyFilters(wrapper, query);
        wrapper.orderByDesc(CourseEntity::getUpdatedAt).orderByDesc(CourseEntity::getId);
        IPage<CourseEntity> page = courseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<StudentCourseListItemVO> records = page.getRecords().stream()
                .map(course -> assembler.toStudentListItem(
                        course, userName(course.getOwnerTeacherId()), byCourse.get(course.getId()), false))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public CourseDetailVO detail(Long studentId, Long courseId) {
        if (!permissionService.canViewCourseAsStudent(studentId, courseId)) {
            if (courseMapper.selectById(courseId) == null) {
                throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
            }
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "选课后才能查看课程内容");
        }
        CourseEntity course = courseManagementService.requireCourse(courseId);
        return assembler.toDetail(course, userName(course.getOwnerTeacherId()), null, false);
    }

    @Transactional
    public EnrollmentVO enroll(Long studentId, Long courseId) {
        CourseEnrollmentEntity existing = enrollmentMapper.selectOne(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getStudentId, studentId));
        if (existing != null) {
            if (EnrollmentStatus.ENROLLED.name().equals(existing.getStatus())) {
                return assembler.toEnrollment(existing);
            }
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "该课程已有不可重复的历史选课记录");
        }
        courseManagementService.requireCourse(courseId);
        if (!permissionService.canEnrollCourse(studentId, courseId)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课程当前不可选");
        }
        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED.name());
        enrollment.setEnrolledAt(now());
        try {
            enrollmentMapper.insert(enrollment);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "请勿重复选课");
        }
        return assembler.toEnrollment(enrollment);
    }

    @Transactional
    public EnrollmentVO withdraw(Long studentId, Long courseId) {
        CourseEnrollmentEntity enrollment = enrollmentMapper.selectOne(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getStudentId, studentId));
        if (enrollment == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "选课记录不存在");
        }
        EnrollmentStatus status = EnrollmentStatus.valueOf(enrollment.getStatus());
        if (!status.canTransitionTo(EnrollmentStatus.WITHDRAWN)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "当前选课状态不能退选");
        }
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN.name());
        enrollment.setWithdrawnAt(now());
        if (enrollmentMapper.updateById(enrollment) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "选课状态已变化，请刷新后重试");
        }
        return assembler.toEnrollment(enrollment);
    }

    private Map<Long, CourseEnrollmentEntity> enrollmentMap(Long studentId, List<CourseEntity> courses) {
        if (courses.isEmpty()) {
            return Map.of();
        }
        List<Long> courseIds = courses.stream().map(CourseEntity::getId).toList();
        return enrollmentMapper.selectList(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                        .eq(CourseEnrollmentEntity::getStudentId, studentId)
                        .in(CourseEnrollmentEntity::getCourseId, courseIds))
                .stream()
                .collect(Collectors.toMap(CourseEnrollmentEntity::getCourseId, Function.identity()));
    }

    private void applyFilters(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseEntity> wrapper,
            CourseListQuery query) {
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(group -> group.like(CourseEntity::getName, keyword)
                    .or()
                    .like(CourseEntity::getCourseCode, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(CourseEntity::getStatus, query.getStatus().name());
        }
        if (query.getTerm() != null && !query.getTerm().isBlank()) {
            wrapper.eq(CourseEntity::getTerm, query.getTerm().trim());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(CourseEntity::getCategoryId, query.getCategoryId());
        }
    }

    private String userName(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
