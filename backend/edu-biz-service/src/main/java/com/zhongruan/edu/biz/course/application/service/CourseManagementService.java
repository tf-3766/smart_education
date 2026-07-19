package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.AddCourseTeacherRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateCourseRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateCourseRequest;
import com.zhongruan.edu.biz.course.api.vo.CollabInvitationVO;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseTemplateVO;
import com.zhongruan.edu.biz.course.api.vo.CourseTeacherVO;
import com.zhongruan.edu.biz.course.api.vo.TeacherCourseListItemVO;
import com.zhongruan.edu.biz.course.api.vo.TeacherOptionVO;
import com.zhongruan.edu.biz.course.application.assembler.CourseAssembler;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseTeacherRole;
import com.zhongruan.edu.biz.course.domain.enums.CourseTeacherStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseReviewEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseReviewMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import com.zhongruan.edu.biz.platform.application.service.CourseCategoryService;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManagementService {
    private final CourseMapper courseMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseReviewMapper courseReviewMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final CoursePermissionService permissionService;
    private final CourseAssembler assembler;
    private final CourseCategoryService categoryService;

    public CourseManagementService(
            CourseMapper courseMapper,
            CourseTeacherMapper courseTeacherMapper,
            CourseReviewMapper courseReviewMapper,
            UserMapper userMapper,
            RoleMapper roleMapper,
            CoursePermissionService permissionService,
            CourseAssembler assembler,
            CourseCategoryService categoryService) {
        this.courseMapper = courseMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseReviewMapper = courseReviewMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionService = permissionService;
        this.assembler = assembler;
        this.categoryService = categoryService;
    }

    @Transactional
    public CourseDetailVO create(Long teacherId, CreateCourseRequest request) {
        validateTimes(request.enrollmentOpenAt(), request.enrollmentCloseAt(), request.startAt(), request.endAt());
        requireCategory(request.categoryId());
        CourseEntity course = new CourseEntity();
        course.setCourseCode(request.courseCode().trim());
        course.setName(request.name().trim());
        course.setSummary(trim(request.summary()));
        course.setCoverUrl(trim(request.coverUrl()));
        course.setCategoryId(request.categoryId());
        course.setTerm(trim(request.term()));
        course.setDepartment(trim(request.department()));
        course.setCredit(request.credit());
        course.setOwnerTeacherId(teacherId);
        course.setStatus(CourseStatus.DRAFT.name());
        course.setReviewStatus(CourseReviewStatus.NOT_SUBMITTED.name());
        course.setEnrollmentOpenAt(utc(request.enrollmentOpenAt()));
        course.setEnrollmentCloseAt(utc(request.enrollmentCloseAt()));
        course.setStartAt(utc(request.startAt()));
        course.setEndAt(utc(request.endAt()));

        try {
            courseMapper.insert(course);
            CourseTeacherEntity owner = new CourseTeacherEntity();
            owner.setCourseId(course.getId());
            owner.setTeacherId(teacherId);
            owner.setRole(CourseTeacherRole.OWNER.name());
            owner.setStatus(CourseTeacherStatus.ACTIVE.name());
            courseTeacherMapper.insert(owner);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课程编号已存在");
        }
        return detail(course, null, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherCourseListItemVO> listForTeacher(Long teacherId, CourseListQuery query) {
        List<Long> courseIds = courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getTeacherId, teacherId)
                        .eq(CourseTeacherEntity::getStatus, CourseTeacherStatus.ACTIVE.name()))
                .stream()
                .map(CourseTeacherEntity::getCourseId)
                .toList();
        if (courseIds.isEmpty()) {
            return PageResponse.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        var wrapper = Wrappers.<CourseEntity>lambdaQuery().in(CourseEntity::getId, courseIds);
        applyFilters(wrapper, query);
        applySort(wrapper, query.getSort());
        IPage<CourseEntity> page = courseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<TeacherCourseListItemVO> records = page.getRecords().stream()
                .map(course -> assembler.toTeacherListItem(course, userName(course.getOwnerTeacherId())))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public CourseDetailVO getForTeacher(Long teacherId, Long courseId) {
        requireEditor(teacherId, courseId);
        return detail(requireCourse(courseId), latestReviewReason(courseId), true);
    }

    @Transactional(readOnly = true)
    public CourseDetailVO getForAdmin(Long courseId) {
        return detail(requireCourse(courseId), latestReviewReason(courseId), true);
    }

    @Transactional
    public CourseDetailVO update(Long teacherId, Long courseId, UpdateCourseRequest request) {
        requireOwner(teacherId, courseId);
        CourseEntity course = requireCourse(courseId);
        CourseStatus status = CourseStatus.valueOf(course.getStatus());
        CourseReviewStatus reviewStatus = CourseReviewStatus.valueOf(course.getReviewStatus());
        if (status != CourseStatus.DRAFT
                || (reviewStatus != CourseReviewStatus.NOT_SUBMITTED && reviewStatus != CourseReviewStatus.REJECTED)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "当前课程状态不允许修改关键字段");
        }
        applyUpdate(course, request);
        updateOrConflict(course);
        return detail(course, latestReviewReason(courseId), true);
    }

    @Transactional
    public CourseDetailVO updateAsAdmin(Long courseId, UpdateCourseRequest request) {
        CourseEntity course = requireCourse(courseId);
        if (CourseStatus.OFFLINE.name().equals(course.getStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "已下线课程不允许继续编辑");
        }
        applyUpdate(course, request);
        updateOrConflict(course);
        return detail(course, latestReviewReason(courseId), true);
    }

    private void applyUpdate(CourseEntity course, UpdateCourseRequest request) {
        validateTimes(request.enrollmentOpenAt(), request.enrollmentCloseAt(), request.startAt(), request.endAt());
        requireCategory(request.categoryId());
        course.setName(request.name().trim());
        course.setSummary(trim(request.summary()));
        course.setCoverUrl(trim(request.coverUrl()));
        course.setCategoryId(request.categoryId());
        course.setTerm(trim(request.term()));
        course.setDepartment(trim(request.department()));
        course.setCredit(request.credit());
        course.setEnrollmentOpenAt(utc(request.enrollmentOpenAt()));
        course.setEnrollmentCloseAt(utc(request.enrollmentCloseAt()));
        course.setStartAt(utc(request.startAt()));
        course.setEndAt(utc(request.endAt()));
        course.setVersion(request.version());
    }

    @Transactional
    public CourseDetailVO submitReview(Long teacherId, Long courseId) {
        requireOwner(teacherId, courseId);
        CourseEntity course = requireCourse(courseId);
        CourseStatus status = CourseStatus.valueOf(course.getStatus());
        CourseReviewStatus review = CourseReviewStatus.valueOf(course.getReviewStatus());
        if (status != CourseStatus.DRAFT || !review.canTransitionTo(CourseReviewStatus.PENDING)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "当前课程不能提交审核");
        }
        course.setStatus(CourseStatus.PENDING_REVIEW.name());
        course.setReviewStatus(CourseReviewStatus.PENDING.name());
        updateOrConflict(course);
        return detail(course, null, true);
    }

    @Transactional
    public CourseDetailVO publish(Long teacherId, Long courseId) {
        requireOwner(teacherId, courseId);
        CourseEntity course = requireCourse(courseId);
        if (!CourseStatus.PENDING_REVIEW.name().equals(course.getStatus())
                || !CourseReviewStatus.APPROVED.name().equals(course.getReviewStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课程审核通过后才能发布");
        }
        course.setStatus(CourseStatus.PUBLISHED.name());
        updateOrConflict(course);
        return detail(course, latestReviewReason(courseId), true);
    }

    @Transactional
    public CourseDetailVO start(Long teacherId, Long courseId) {
        requireOwner(teacherId, courseId);
        return transition(courseId, CourseStatus.ONGOING, "当前课程状态不允许开课");
    }

    @Transactional
    public CourseDetailVO finish(Long teacherId, Long courseId) {
        requireOwner(teacherId, courseId);
        return transition(courseId, CourseStatus.FINISHED, "当前课程状态不允许结课");
    }

    @Transactional
    public CourseDetailVO offline(Long teacherId, Long courseId) {
        requireOwner(teacherId, courseId);
        return transition(courseId, CourseStatus.OFFLINE, "课程已下线");
    }

    @Transactional
    public CourseDetailVO offlineAsAdmin(Long courseId) {
        return transition(courseId, CourseStatus.OFFLINE, "课程已下线");
    }

    @Transactional(readOnly = true)
    public List<CourseTeacherVO> listTeachers(Long teacherId, Long courseId) {
        requireEditor(teacherId, courseId);
        return courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getCourseId, courseId)
                        .orderByAsc(CourseTeacherEntity::getRole)
                        .orderByAsc(CourseTeacherEntity::getId))
                .stream()
                .map(relation -> assembler.toTeacher(relation, userName(relation.getTeacherId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseTemplateVO> listTemplates() {
        LinkedHashMap<String, CourseTemplateVO> templates = new LinkedHashMap<>();
        courseMapper.selectList(Wrappers.<CourseEntity>lambdaQuery()
                        .orderByAsc(CourseEntity::getName)
                        .orderByAsc(CourseEntity::getId))
                .forEach(course -> templates.putIfAbsent(
                        course.getCourseCode(),
                        new CourseTemplateVO(
                                String.valueOf(course.getId()),
                                course.getCourseCode(),
                                course.getName(),
                                course.getSummary())));
        return templates.values().stream().limit(100).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherOptionVO> listTeacherDirectory() {
        return userMapper.findEnabledTeachers().stream()
                .map(teacher -> new TeacherOptionVO(String.valueOf(teacher.getId()), teacher.getDisplayName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CollabInvitationVO> listInvitations(Long teacherId) {
        return courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getTeacherId, teacherId)
                        .eq(CourseTeacherEntity::getRole, CourseTeacherRole.COLLABORATOR.name())
                        .eq(CourseTeacherEntity::getStatus, CourseTeacherStatus.PENDING.name())
                        .orderByDesc(CourseTeacherEntity::getCreatedAt))
                .stream()
                .map(relation -> {
                    CourseEntity course = requireCourse(relation.getCourseId());
                    return new CollabInvitationVO(
                            String.valueOf(course.getId()),
                            course.getName(),
                            String.valueOf(course.getOwnerTeacherId()),
                            userName(course.getOwnerTeacherId()),
                            assembler.time(relation.getCreatedAt()));
                })
                .toList();
    }

    @Transactional
    public void acceptInvitation(Long teacherId, Long courseId) {
        CourseTeacherEntity relation = pendingInvitation(teacherId, courseId);
        relation.setStatus(CourseTeacherStatus.ACTIVE.name());
        if (courseTeacherMapper.updateById(relation) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "邀请已被其他请求处理，请刷新后重试");
        }
    }

    @Transactional
    public void rejectInvitation(Long teacherId, Long courseId) {
        CourseTeacherEntity relation = pendingInvitation(teacherId, courseId);
        if (courseTeacherMapper.deleteById(relation.getId()) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "邀请已被其他请求处理，请刷新后重试");
        }
    }

    @Transactional
    public CourseTeacherVO addTeacher(Long ownerId, Long courseId, AddCourseTeacherRequest request) {
        requireOwner(ownerId, courseId);
        if (ownerId.equals(request.teacherId())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "无需邀请课程负责人本人");
        }
        UserEntity teacher = userMapper.selectById(request.teacherId());
        Set<String> roles = teacher == null ? Set.of() : roleMapper.findRoleCodesByUserId(request.teacherId());
        if (teacher == null
                || !UserStatus.ENABLED.name().equals(teacher.getUserStatus())
                || !roles.contains(RoleCode.TEACHER.name())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "教师账号不存在或不可用");
        }
        CourseTeacherEntity existing = courseTeacherMapper.findIncludingDeleted(courseId, request.teacherId());
        if (existing != null && Integer.valueOf(0).equals(existing.getDeleted())) {
            String message = CourseTeacherStatus.PENDING.name().equals(existing.getStatus())
                    ? "已向该教师发出邀请，等待对方确认"
                    : "该教师已加入课程";
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, message);
        }
        CourseTeacherEntity relation;
        if (existing != null) {
            if (courseTeacherMapper.restoreInvitation(existing.getId(), ownerId) != 1) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "邀请恢复失败，请刷新后重试");
            }
            relation = courseTeacherMapper.selectById(existing.getId());
        } else {
            relation = new CourseTeacherEntity();
            relation.setCourseId(courseId);
            relation.setTeacherId(request.teacherId());
            relation.setRole(CourseTeacherRole.COLLABORATOR.name());
            relation.setStatus(CourseTeacherStatus.PENDING.name());
            try {
                courseTeacherMapper.insert(relation);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "该教师已收到邀请或已加入课程");
            }
        }
        return assembler.toTeacher(relation, teacher.getDisplayName());
    }
    @Transactional
    public void removeTeacher(Long ownerId, Long courseId, Long teacherId) {
        requireOwner(ownerId, courseId);
        CourseEntity course = requireCourse(courseId);
        if (teacherId.equals(course.getOwnerTeacherId())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "不能删除课程负责人");
        }
        CourseTeacherEntity relation = courseTeacherMapper.selectOne(Wrappers.<CourseTeacherEntity>lambdaQuery()
                .eq(CourseTeacherEntity::getCourseId, courseId)
                .eq(CourseTeacherEntity::getTeacherId, teacherId));
        if (relation == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程教师关系不存在");
        }
        courseTeacherMapper.deleteById(relation.getId());
    }

    private CourseTeacherEntity pendingInvitation(Long teacherId, Long courseId) {
        CourseTeacherEntity relation = courseTeacherMapper.selectOne(Wrappers.<CourseTeacherEntity>lambdaQuery()
                .eq(CourseTeacherEntity::getCourseId, courseId)
                .eq(CourseTeacherEntity::getTeacherId, teacherId)
                .eq(CourseTeacherEntity::getRole, CourseTeacherRole.COLLABORATOR.name())
                .eq(CourseTeacherEntity::getStatus, CourseTeacherStatus.PENDING.name()));
        if (relation == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "协作邀请不存在或已处理");
        }
        return relation;
    }
    public CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
        }
        return course;
    }

    public void requireOwner(Long userId, Long courseId) {
        if (!permissionService.canManageCourse(userId, courseId)) {
            if (courseMapper.selectById(courseId) == null) {
                throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
            }
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "只有课程负责人可以执行该操作");
        }
    }

    public void requireEditor(Long userId, Long courseId) {
        if (!permissionService.canEditCourseContent(userId, courseId)) {
            if (courseMapper.selectById(courseId) == null) {
                throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程不存在");
            }
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你不属于该课程教师团队");
        }
    }

    private CourseDetailVO detail(CourseEntity course, String reason, boolean internalView) {
        return assembler.toDetail(course, userName(course.getOwnerTeacherId()), reason, internalView);
    }

    private String latestReviewReason(Long courseId) {
        CourseReviewEntity review = courseReviewMapper.selectOne(Wrappers.<CourseReviewEntity>lambdaQuery()
                .eq(CourseReviewEntity::getCourseId, courseId)
                .orderByDesc(CourseReviewEntity::getReviewedAt)
                .orderByDesc(CourseReviewEntity::getId)
                .last("LIMIT 1"));
        return review == null ? null : review.getReason();
    }

    private String userName(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private void updateOrConflict(CourseEntity course) {
        if (courseMapper.updateById(course) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课程已被其他请求修改，请刷新后重试");
        }
    }

    private CourseDetailVO transition(Long courseId, CourseStatus target, String message) {
        CourseEntity course = requireCourse(courseId);
        CourseStatus current = CourseStatus.valueOf(course.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, message);
        }
        course.setStatus(target.name());
        updateOrConflict(course);
        return detail(course, latestReviewReason(courseId), true);
    }

    private void validateTimes(
            OffsetDateTime enrollmentOpenAt,
            OffsetDateTime enrollmentCloseAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt) {
        if (enrollmentOpenAt != null && enrollmentCloseAt != null && !enrollmentCloseAt.isAfter(enrollmentOpenAt)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "选课结束时间必须晚于开始时间");
        }
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "课程结束时间必须晚于开始时间");
        }
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
        if (query.getReviewStatus() != null) {
            wrapper.eq(CourseEntity::getReviewStatus, query.getReviewStatus().name());
        }
        if (query.getTerm() != null && !query.getTerm().isBlank()) {
            wrapper.eq(CourseEntity::getTerm, query.getTerm().trim());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(CourseEntity::getCategoryId, query.getCategoryId());
        }
    }

    private void applySort(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseEntity> wrapper, String sort) {
        String value = sort == null ? "createdAt,desc" : sort;
        boolean asc = value.endsWith(",asc");
        if (value.startsWith("name,")) {
            wrapper.orderBy(true, asc, CourseEntity::getName);
        } else if (value.startsWith("startAt,")) {
            wrapper.orderBy(true, asc, CourseEntity::getStartAt);
        } else if (value.startsWith("createdAt,")) {
            wrapper.orderBy(true, asc, CourseEntity::getCreatedAt);
        } else {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "不支持的课程排序字段");
        }
        wrapper.orderByDesc(CourseEntity::getId);
    }

    private LocalDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void requireCategory(Long categoryId) {
        if (categoryId != null) {
            categoryService.requireEnabled(categoryId);
        }
    }
}
