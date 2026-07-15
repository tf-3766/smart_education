package com.zhongruan.edu.biz.notification.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserRoleMapper;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseTeacherMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamEntity;
import com.zhongruan.edu.biz.grade.infrastructure.persistence.entity.GradeRecordEntity;
import com.zhongruan.edu.biz.notification.api.dto.query.NotificationListQuery;
import com.zhongruan.edu.biz.notification.api.dto.request.UpdateNotificationPreferencesRequest;
import com.zhongruan.edu.biz.notification.api.vo.NotificationPreferencesVO;
import com.zhongruan.edu.biz.notification.api.vo.NotificationVO;
import com.zhongruan.edu.biz.notification.domain.enums.NotificationCategory;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.entity.NotificationEntity;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.entity.NotificationReadEntity;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.mapper.NotificationMapper;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.mapper.NotificationPreferenceMapper;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.mapper.NotificationReadMapper;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementAudience;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementScope;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementStatus;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.AnnouncementEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.LearningWarningEntity;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationApplicationService {
    private static final String PUBLISHED = AnnouncementStatus.PUBLISHED.name();
    private static final String ARCHIVED = "ARCHIVED";
    private static final String SUPPRESSED = "SUPPRESSED";

    private final NotificationMapper notificationMapper;
    private final NotificationReadMapper notificationReadMapper;
    private final NotificationPreferenceMapper preferenceMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final NotificationRealtimePublisher realtimePublisher;
    private final Clock clock = Clock.systemUTC();

    public NotificationApplicationService(
            NotificationMapper notificationMapper,
            NotificationReadMapper notificationReadMapper,
            NotificationPreferenceMapper preferenceMapper,
            CourseEnrollmentMapper enrollmentMapper,
            CourseTeacherMapper courseTeacherMapper,
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            NotificationRealtimePublisher realtimePublisher) {
        this.notificationMapper = notificationMapper;
        this.notificationReadMapper = notificationReadMapper;
        this.preferenceMapper = preferenceMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationVO> list(Long userId, NotificationListQuery query) {
        Map<Long, LocalDateTime> reads = readsByNotification(userId);
        LambdaQueryWrapper<NotificationEntity> wrapper = visibleNotifications(userId);
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(NotificationEntity::getCategory, query.getCategory().trim().toUpperCase());
        }
        if (Boolean.TRUE.equals(query.getUnread()) && !reads.isEmpty()) {
            wrapper.notIn(NotificationEntity::getId, reads.keySet());
        }
        var page = notificationMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                wrapper.orderByDesc(NotificationEntity::getCreatedAt)
                        .orderByDesc(NotificationEntity::getId));
        return PageResponse.of(
                page.getRecords().stream().map(item -> toVO(item, reads.get(item.getId()))).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        Set<Long> readIds = readsByNotification(userId).keySet();
        LambdaQueryWrapper<NotificationEntity> wrapper = visibleNotifications(userId);
        if (!readIds.isEmpty()) {
            wrapper.notIn(NotificationEntity::getId, readIds);
        }
        return notificationMapper.selectCount(wrapper);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        requireOwnedVisibleNotification(userId, notificationId);
        insertReadReceipt(userId, notificationId);
        realtimePublisher.publishAfterCommit(userId, "state-changed", notificationId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        Set<Long> existing = readsByNotification(userId).keySet();
        notificationMapper.selectList(visibleNotifications(userId)).stream()
                .map(NotificationEntity::getId)
                .filter(notificationId -> !existing.contains(notificationId))
                .forEach(notificationId -> insertReadReceipt(userId, notificationId));
        realtimePublisher.publishAfterCommit(userId, "state-changed", null);
    }

    @Transactional
    public void archive(Long userId, Long notificationId) {
        NotificationEntity notification = requireOwnedVisibleNotification(userId, notificationId);
        notification.setStatus(ARCHIVED);
        if (notificationMapper.updateById(notification) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        realtimePublisher.publishAfterCommit(userId, "state-changed", notificationId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesVO preferences(Long userId) {
        Map<String, Integer> stored = new LinkedHashMap<>();
        preferenceMapper.selectList(Wrappers.<NotificationPreferenceEntity>lambdaQuery()
                        .eq(NotificationPreferenceEntity::getUserId, userId))
                .forEach(preference -> stored.put(preference.getCategory(), preference.getEnabled()));
        Set<String> enabled = new LinkedHashSet<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            if (stored.getOrDefault(category.name(), 1) == 1) {
                enabled.add(category.name());
            }
        }
        return new NotificationPreferencesVO(enabled);
    }

    @Transactional
    public NotificationPreferencesVO updatePreferences(
            Long userId, UpdateNotificationPreferencesRequest request) {
        for (NotificationCategory category : NotificationCategory.values()) {
            NotificationPreferenceEntity preference = preferenceMapper.selectOne(
                    Wrappers.<NotificationPreferenceEntity>lambdaQuery()
                            .eq(NotificationPreferenceEntity::getUserId, userId)
                            .eq(NotificationPreferenceEntity::getCategory, category.name()));
            int enabled = request.enabledCategories().contains(category) ? 1 : 0;
            if (preference == null) {
                preference = new NotificationPreferenceEntity();
                preference.setUserId(userId);
                preference.setCategory(category.name());
                preference.setEnabled(enabled);
                try {
                    preferenceMapper.insert(preference);
                } catch (DuplicateKeyException ignored) {
                    updatePreferenceAfterConcurrentInsert(userId, category, enabled);
                }
            } else if (!Integer.valueOf(enabled).equals(preference.getEnabled())) {
                preference.setEnabled(enabled);
                preferenceMapper.updateById(preference);
            }
        }
        realtimePublisher.publishAfterCommit(userId, "preferences-changed", null);
        return preferences(userId);
    }

    @Transactional
    public void publishAnnouncement(AnnouncementEntity announcement) {
        if (!PUBLISHED.equals(announcement.getStatus())) {
            return;
        }
        NotificationCategory category = AnnouncementScope.COURSE.name().equals(announcement.getScopeType())
                ? NotificationCategory.COURSE : NotificationCategory.SYSTEM;
        for (Long recipientId : announcementRecipients(announcement)) {
            createNotification(
                    recipientId, announcement.getTitle(), announcement.getContent(), category,
                    "ANNOUNCEMENT", "announcement:" + announcement.getId(),
                    announcement.getId(), announcement.getCourseId(), null, null, null);
        }
    }

    @Transactional
    public void withdrawAnnouncement(Long announcementId) {
        List<NotificationEntity> notifications = notificationMapper.selectList(
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getAnnouncementId, announcementId)
                        .eq(NotificationEntity::getStatus, PUBLISHED));
        NotificationEntity update = new NotificationEntity();
        update.setStatus(AnnouncementStatus.WITHDRAWN.name());
        notificationMapper.update(update, Wrappers.<NotificationEntity>lambdaUpdate()
                .eq(NotificationEntity::getAnnouncementId, announcementId)
                .eq(NotificationEntity::getStatus, PUBLISHED));
        notifications.forEach(notification -> realtimePublisher.publishAfterCommit(
                notification.getRecipientUserId(), "state-changed", notification.getId()));
    }

    @Transactional
    public void publishAssignment(AssignmentEntity assignment) {
        String content = "请按时完成作业，截止时间：" + time(assignment.getDueAt());
        for (Long studentId : courseStudentIds(assignment.getCourseId())) {
            createNotification(
                    studentId, "作业已发布：" + assignment.getTitle(), content,
                    NotificationCategory.ASSIGNMENT, "ASSIGNMENT_PUBLISHED",
                    "assignment:" + assignment.getId() + ":published",
                    null, assignment.getCourseId(), assignment.getId(), null, null);
        }
    }

    @Transactional
    public void publishAssignmentSubmission(
            AssignmentEntity assignment, AssignmentSubmissionEntity submission) {
        for (Long teacherId : courseTeacherIds(assignment.getCourseId())) {
            createNotification(
                    teacherId, "收到作业提交：" + assignment.getTitle(), "有新的学生作业等待批改。",
                    NotificationCategory.ASSIGNMENT, "ASSIGNMENT_SUBMITTED",
                    "submission:" + submission.getId() + ":submitted",
                    null, assignment.getCourseId(), assignment.getId(), null, null);
        }
    }

    @Transactional
    public void publishAssignmentDeadline(AssignmentEntity assignment) {
        for (Long studentId : courseStudentIds(assignment.getCourseId())) {
            createNotification(
                    studentId, "作业已截止：" + assignment.getTitle(), "该作业已到截止时间。",
                    NotificationCategory.ASSIGNMENT, "ASSIGNMENT_DEADLINE",
                    "assignment:" + assignment.getId() + ":deadline",
                    null, assignment.getCourseId(), assignment.getId(), null, null);
        }
        for (Long teacherId : courseTeacherIds(assignment.getCourseId())) {
            createNotification(
                    teacherId, "作业已截止：" + assignment.getTitle(), "作业已截止，可以进入批改工作台处理提交。",
                    NotificationCategory.ASSIGNMENT, "ASSIGNMENT_DEADLINE",
                    "assignment:" + assignment.getId() + ":deadline",
                    null, assignment.getCourseId(), assignment.getId(), null, null);
        }
    }

    @Transactional
    public void publishExam(ExamEntity exam) {
        String content = "考试时间：" + time(exam.getStartAt()) + " 至 " + time(exam.getEndAt());
        for (Long studentId : courseStudentIds(exam.getCourseId())) {
            createNotification(
                    studentId, "考试安排已发布：" + exam.getTitle(), content,
                    NotificationCategory.EXAM, "EXAM_PUBLISHED", "exam:" + exam.getId() + ":published",
                    null, exam.getCourseId(), null, exam.getId(), null);
        }
    }

    @Transactional
    public void publishWarning(LearningWarningEntity warning) {
        createNotification(
                warning.getStudentId(), "学习预警：" + warning.getSummary(), warning.getSuggestion(),
                NotificationCategory.WARNING, "WARNING_CREATED", "warning:" + warning.getId() + ":created",
                null, warning.getCourseId(), null, null, warning.getId());
        for (Long teacherId : courseTeacherIds(warning.getCourseId())) {
            createNotification(
                    teacherId, "学生学习预警：" + warning.getSummary(), "请进入预警中心查看并跟进。",
                    NotificationCategory.WARNING, "WARNING_CREATED", "warning:" + warning.getId() + ":created",
                    null, warning.getCourseId(), null, null, warning.getId());
        }
    }

    @Transactional
    public void publishGrade(GradeRecordEntity grade, AssignmentEntity assignment) {
        createNotification(
                grade.getStudentId(), "作业批改已完成：" + assignment.getTitle(),
                "成绩已发布，请进入成绩页面查看。",
                NotificationCategory.ASSIGNMENT, "GRADE_PUBLISHED", "grade:" + grade.getId() + ":published",
                null, grade.getCourseId(), assignment.getId(), null, null);
    }

    private NotificationEntity requireOwnedVisibleNotification(Long userId, Long notificationId) {
        NotificationEntity notification = notificationMapper.selectOne(visibleNotifications(userId)
                .eq(NotificationEntity::getId, notificationId));
        if (notification == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "消息不存在或已撤回");
        }
        return notification;
    }

    private LambdaQueryWrapper<NotificationEntity> visibleNotifications(Long userId) {
        return Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getRecipientUserId, userId)
                .eq(NotificationEntity::getStatus, PUBLISHED);
    }

    private Map<Long, LocalDateTime> readsByNotification(Long userId) {
        Map<Long, LocalDateTime> reads = new LinkedHashMap<>();
        notificationReadMapper.selectList(Wrappers.<NotificationReadEntity>lambdaQuery()
                        .eq(NotificationReadEntity::getUserId, userId))
                .forEach(read -> reads.put(read.getNotificationId(), read.getReadAt()));
        return reads;
    }

    private void insertReadReceipt(Long userId, Long notificationId) {
        if (notificationReadMapper.selectCount(Wrappers.<NotificationReadEntity>lambdaQuery()
                        .eq(NotificationReadEntity::getUserId, userId)
                        .eq(NotificationReadEntity::getNotificationId, notificationId)) > 0) {
            return;
        }
        NotificationReadEntity receipt = new NotificationReadEntity();
        receipt.setUserId(userId);
        receipt.setNotificationId(notificationId);
        receipt.setReadAt(LocalDateTime.now(clock));
        try {
            notificationReadMapper.insert(receipt);
        } catch (DuplicateKeyException ignored) {
            // Concurrent read operations are idempotent by the database unique key.
        }
    }

    private void createNotification(
            Long recipientId,
            String title,
            String content,
            NotificationCategory category,
            String sourceType,
            String idempotencyKeyBase,
            Long announcementId,
            Long courseId,
            Long assignmentId,
            Long examId,
            Long warningId) {
        if (recipientId == null) {
            return;
        }
        String idempotencyKey = idempotencyKeyBase + ":user:" + recipientId;
        if (notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getIdempotencyKey, idempotencyKey)) > 0) {
            return;
        }
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientUserId(recipientId);
        notification.setTitle(title);
        notification.setContent(content == null ? "" : content);
        notification.setCategory(category.name());
        boolean enabled = notificationEnabled(recipientId, category);
        notification.setStatus(enabled ? PUBLISHED : SUPPRESSED);
        notification.setSourceType(sourceType);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setAnnouncementId(announcementId);
        notification.setCourseId(courseId);
        notification.setAssignmentId(assignmentId);
        notification.setExamId(examId);
        notification.setWarningId(warningId);
        try {
            notificationMapper.insert(notification);
            if (enabled) {
                realtimePublisher.publishAfterCommit(recipientId, "created", notification.getId());
            }
        } catch (DuplicateKeyException ignored) {
            // Retried business events produce the same key and do not duplicate messages.
        }
    }

    private boolean notificationEnabled(Long userId, NotificationCategory category) {
        NotificationPreferenceEntity preference = preferenceMapper.selectOne(
                Wrappers.<NotificationPreferenceEntity>lambdaQuery()
                        .eq(NotificationPreferenceEntity::getUserId, userId)
                        .eq(NotificationPreferenceEntity::getCategory, category.name()));
        return preference == null || Integer.valueOf(1).equals(preference.getEnabled());
    }

    private void updatePreferenceAfterConcurrentInsert(
            Long userId, NotificationCategory category, int enabled) {
        NotificationPreferenceEntity update = new NotificationPreferenceEntity();
        update.setEnabled(enabled);
        preferenceMapper.update(update, Wrappers.<NotificationPreferenceEntity>lambdaUpdate()
                .eq(NotificationPreferenceEntity::getUserId, userId)
                .eq(NotificationPreferenceEntity::getCategory, category.name()));
    }

    private Set<Long> announcementRecipients(AnnouncementEntity announcement) {
        if (AnnouncementScope.COURSE.name().equals(announcement.getScopeType())) {
            return courseRecipients(announcement.getCourseId(), announcement.getAudience());
        }
        return systemRecipients(announcement.getAudience());
    }

    private Set<Long> courseRecipients(Long courseId, String audience) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (AnnouncementAudience.STUDENT.name().equals(audience)
                || AnnouncementAudience.ALL.name().equals(audience)) {
            recipients.addAll(courseStudentIds(courseId));
        }
        if (AnnouncementAudience.TEACHER.name().equals(audience)
                || AnnouncementAudience.ALL.name().equals(audience)) {
            recipients.addAll(courseTeacherIds(courseId));
        }
        return recipients;
    }

    private List<Long> courseStudentIds(Long courseId) {
        return enrollmentMapper.selectList(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                        .eq(CourseEnrollmentEntity::getCourseId, courseId)
                        .in(CourseEnrollmentEntity::getStatus,
                                EnrollmentStatus.ENROLLED.name(), EnrollmentStatus.COMPLETED.name()))
                .stream().map(CourseEnrollmentEntity::getStudentId).distinct().toList();
    }

    private List<Long> courseTeacherIds(Long courseId) {
        return courseTeacherMapper.selectList(Wrappers.<CourseTeacherEntity>lambdaQuery()
                        .eq(CourseTeacherEntity::getCourseId, courseId))
                .stream().map(CourseTeacherEntity::getTeacherId).distinct().toList();
    }

    private Set<Long> systemRecipients(String audience) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (AnnouncementAudience.STUDENT.name().equals(audience)
                || AnnouncementAudience.ALL.name().equals(audience)) {
            recipients.addAll(enabledUserIdsForRole(RoleCode.STUDENT));
        }
        if (AnnouncementAudience.TEACHER.name().equals(audience)
                || AnnouncementAudience.ALL.name().equals(audience)) {
            recipients.addAll(enabledUserIdsForRole(RoleCode.TEACHER));
        }
        recipients.addAll(enabledUserIdsForRole(RoleCode.ADMIN));
        recipients.addAll(enabledUserIdsForRole(RoleCode.SUPER_ADMIN));
        return recipients;
    }

    private List<Long> enabledUserIdsForRole(RoleCode roleCode) {
        RoleEntity role = roleMapper.selectOne(Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getRoleCode, roleCode.name())
                .eq(RoleEntity::getEnabled, 1));
        if (role == null) {
            return List.of();
        }
        List<Long> userIds = userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .eq(UserRoleEntity::getRoleId, role.getId()))
                .stream().map(UserRoleEntity::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .in(UserEntity::getId, userIds)
                        .eq(UserEntity::getUserStatus, UserStatus.ENABLED.name()))
                .stream().map(UserEntity::getId).toList();
    }

    private NotificationVO toVO(NotificationEntity notification, LocalDateTime readAt) {
        NotificationCategory category = NotificationCategory.valueOf(notification.getCategory());
        return new NotificationVO(
                String.valueOf(notification.getId()), notification.getTitle(), notification.getContent(),
                category.name(), category.label(), notification.getStatus(), notification.getSourceType(),
                stringId(notification.getAnnouncementId()), stringId(notification.getCourseId()),
                stringId(notification.getAssignmentId()), stringId(notification.getExamId()),
                stringId(notification.getWarningId()),
                notification.getCreatedAt().atOffset(ZoneOffset.UTC), readAt != null,
                readAt == null ? null : readAt.atOffset(ZoneOffset.UTC));
    }

    private String time(LocalDateTime value) {
        return value == null ? "未设置" : value.atOffset(ZoneOffset.UTC).toString();
    }

    private String stringId(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
