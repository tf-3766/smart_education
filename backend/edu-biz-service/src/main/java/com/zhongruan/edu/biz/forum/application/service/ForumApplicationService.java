package com.zhongruan.edu.biz.forum.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.application.service.CourseManagementService;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.forum.api.dto.query.ForumListQuery;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumReplyCreateRequest;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumTopicCreateRequest;
import com.zhongruan.edu.biz.forum.api.dto.request.ForumVisibilityRequest;
import com.zhongruan.edu.biz.forum.api.vo.ForumReplyVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicDetailVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicListItemVO;
import com.zhongruan.edu.biz.forum.application.assembler.ForumAssembler;
import com.zhongruan.edu.biz.forum.domain.ForumErrorCode;
import com.zhongruan.edu.biz.forum.domain.enums.ForumContentStatus;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumReplyEntity;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumTopicEntity;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.mapper.ForumReplyMapper;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.mapper.ForumTopicMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForumApplicationService {
    private final ForumTopicMapper topicMapper;
    private final ForumReplyMapper replyMapper;
    private final UserMapper userMapper;
    private final CoursePermissionService permissionService;
    private final CourseManagementService courseManagementService;
    private final ForumAssembler assembler;

    public ForumApplicationService(
            ForumTopicMapper topicMapper,
            ForumReplyMapper replyMapper,
            UserMapper userMapper,
            CoursePermissionService permissionService,
            CourseManagementService courseManagementService,
            ForumAssembler assembler) {
        this.topicMapper = topicMapper;
        this.replyMapper = replyMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.courseManagementService = courseManagementService;
        this.assembler = assembler;
    }

    @Transactional
    public ForumTopicDetailVO createTopic(Long studentId, Long courseId, ForumTopicCreateRequest request) {
        requireStudentCourseAccess(studentId, courseId);
        return createTopicForMember(studentId, courseId, request);
    }

    @Transactional
    public ForumTopicDetailVO createTeacherTopic(Long teacherId, Long courseId, ForumTopicCreateRequest request) {
        courseManagementService.requireEditor(teacherId, courseId);
        return createTopicForMember(teacherId, courseId, request);
    }

    private ForumTopicDetailVO createTopicForMember(
            Long authorId, Long courseId, ForumTopicCreateRequest request) {
        ForumTopicEntity topic = new ForumTopicEntity();
        topic.setCourseId(courseId);
        topic.setAuthorId(authorId);
        topic.setTitle(request.title().trim());
        topic.setContent(request.content().trim());
        topic.setStatus(ForumContentStatus.VISIBLE.name());
        topic.setPinned(0);
        topic.setReplyCount(0);
        topicMapper.insert(topic);
        return assembler.toDetail(topic, authorProfile(authorId).name(), authorProfile(authorId).avatarFileId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumTopicListItemVO> listStudentTopics(Long studentId, Long courseId, ForumListQuery query) {
        requireStudentCourseAccess(studentId, courseId);
        var wrapper = Wrappers.<ForumTopicEntity>lambdaQuery()
                .eq(ForumTopicEntity::getCourseId, courseId)
                .eq(ForumTopicEntity::getStatus, ForumContentStatus.VISIBLE.name());
        if (query.getStatus() != null) {
            wrapper.eq(ForumTopicEntity::getStatus, query.getStatus().name());
        }
        applyTopicKeyword(wrapper, query.getKeyword());
        applyTopicOrder(wrapper);
        return topicPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public ForumTopicDetailVO studentTopic(Long studentId, Long topicId) {
        ForumTopicEntity topic = requireTopic(topicId);
        requireStudentCourseAccess(studentId, topic.getCourseId());
        if (!ForumContentStatus.VISIBLE.name().equals(topic.getStatus())) {
            throw new BusinessException(ForumErrorCode.FORUM_TOPIC_NOT_FOUND);
        }
        return assembler.toDetail(topic, authorProfile(topic.getAuthorId()).name(), authorProfile(topic.getAuthorId()).avatarFileId());
    }

    @Transactional(readOnly = true)
    public ForumTopicDetailVO teacherTopic(Long teacherId, Long topicId) {
        ForumTopicEntity topic = requireTopic(topicId);
        courseManagementService.requireEditor(teacherId, topic.getCourseId());
        return assembler.toDetail(topic, authorProfile(topic.getAuthorId()).name(), authorProfile(topic.getAuthorId()).avatarFileId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumReplyVO> studentReplies(Long studentId, Long topicId, ForumListQuery query) {
        ForumTopicEntity topic = requireTopic(topicId);
        requireStudentCourseAccess(studentId, topic.getCourseId());
        if (!ForumContentStatus.VISIBLE.name().equals(topic.getStatus())) {
            throw new BusinessException(ForumErrorCode.FORUM_TOPIC_NOT_FOUND);
        }
        var wrapper = Wrappers.<ForumReplyEntity>lambdaQuery()
                .eq(ForumReplyEntity::getTopicId, topicId)
                .eq(ForumReplyEntity::getStatus, ForumContentStatus.VISIBLE.name());
        if (query.getStatus() != null) {
            wrapper.eq(ForumReplyEntity::getStatus, query.getStatus().name());
        }
        wrapper.orderByAsc(ForumReplyEntity::getCreatedAt)
                .orderByAsc(ForumReplyEntity::getId);
        return replyPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumReplyVO> teacherReplies(Long teacherId, Long topicId, ForumListQuery query) {
        ForumTopicEntity topic = requireTopic(topicId);
        courseManagementService.requireEditor(teacherId, topic.getCourseId());
        var wrapper = Wrappers.<ForumReplyEntity>lambdaQuery()
                .eq(ForumReplyEntity::getTopicId, topicId);
        if (query.getStatus() != null) {
            wrapper.eq(ForumReplyEntity::getStatus, query.getStatus().name());
        }
        wrapper.orderByAsc(ForumReplyEntity::getCreatedAt)
                .orderByAsc(ForumReplyEntity::getId);
        return replyPage(query, wrapper);
    }

    @Transactional
    public ForumReplyVO createReply(Long studentId, Long topicId, ForumReplyCreateRequest request) {
        ForumTopicEntity topic = requireTopic(topicId);
        requireStudentCourseAccess(studentId, topic.getCourseId());
        return createReplyForMember(studentId, topic, request);
    }

    @Transactional
    public ForumReplyVO createTeacherReply(Long teacherId, Long topicId, ForumReplyCreateRequest request) {
        ForumTopicEntity topic = requireTopic(topicId);
        courseManagementService.requireEditor(teacherId, topic.getCourseId());
        return createReplyForMember(teacherId, topic, request);
    }

    private ForumReplyVO createReplyForMember(
            Long authorId, ForumTopicEntity topic, ForumReplyCreateRequest request) {
        if (!ForumContentStatus.VISIBLE.name().equals(topic.getStatus())) {
            throw new BusinessException(ForumErrorCode.FORUM_TOPIC_HIDDEN);
        }
        if (request.parentReplyId() != null) {
            ForumReplyEntity parent = replyMapper.selectById(request.parentReplyId());
            if (parent == null
                    || !topic.getId().equals(parent.getTopicId())
                    || !ForumContentStatus.VISIBLE.name().equals(parent.getStatus())) {
                throw new BusinessException(ForumErrorCode.FORUM_PARENT_REPLY_INVALID);
            }
        }
        ForumReplyEntity reply = new ForumReplyEntity();
        reply.setTopicId(topic.getId());
        reply.setCourseId(topic.getCourseId());
        reply.setAuthorId(authorId);
        reply.setParentReplyId(request.parentReplyId());
        reply.setContent(request.content().trim());
        reply.setStatus(ForumContentStatus.VISIBLE.name());
        replyMapper.insert(reply);
        recalculateTopicReplyStats(topic.getId());
        return assembler.toReply(reply, authorProfile(authorId).name(), authorProfile(authorId).avatarFileId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumTopicListItemVO> listTeacherTopics(Long teacherId, Long courseId, ForumListQuery query) {
        courseManagementService.requireEditor(teacherId, courseId);
        var wrapper = Wrappers.<ForumTopicEntity>lambdaQuery().eq(ForumTopicEntity::getCourseId, courseId);
        if (query.getStatus() != null) {
            wrapper.eq(ForumTopicEntity::getStatus, query.getStatus().name());
        }
        applyTopicKeyword(wrapper, query.getKeyword());
        applyTopicOrder(wrapper);
        return topicPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumTopicListItemVO> listAdminTopics(ForumListQuery query) {
        var wrapper = Wrappers.<ForumTopicEntity>lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(ForumTopicEntity::getStatus, query.getStatus().name());
        }
        applyTopicKeyword(wrapper, query.getKeyword());
        applyTopicOrder(wrapper);
        return topicPage(query, wrapper);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumReplyVO> listAdminReplies(ForumListQuery query) {
        var wrapper = Wrappers.<ForumReplyEntity>lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(ForumReplyEntity::getStatus, query.getStatus().name());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(ForumReplyEntity::getContent, query.getKeyword().trim());
        }
        wrapper.orderByDesc(ForumReplyEntity::getCreatedAt)
                .orderByDesc(ForumReplyEntity::getId);
        return replyPage(query, wrapper);
    }
    @Transactional
    public ForumTopicListItemVO setTopicPinnedByTeacher(
            Long teacherId, Long topicId, boolean pinned, Integer version) {
        ForumTopicEntity topic = requireTopic(topicId);
        courseManagementService.requireEditor(teacherId, topic.getCourseId());
        topic.setPinned(pinned ? 1 : 0);
        topic.setVersion(version);
        updateTopicOrConflict(topic);
        ForumTopicEntity fresh = requireTopic(topicId);
        return assembler.toListItem(fresh, authorProfile(fresh.getAuthorId()).name(), authorProfile(fresh.getAuthorId()).avatarFileId());
    }
    @Transactional
    public ForumTopicDetailVO setTopicVisibilityByTeacher(
            Long teacherId, Long topicId, ForumVisibilityRequest request) {
        ForumTopicEntity topic = requireTopic(topicId);
        courseManagementService.requireEditor(teacherId, topic.getCourseId());
        return setTopicVisibility(topic, request, teacherId);
    }

    @Transactional
    public ForumTopicDetailVO setTopicVisibilityByAdmin(
            Long adminId, Long topicId, ForumVisibilityRequest request) {
        return setTopicVisibility(requireTopic(topicId), request, adminId);
    }

    @Transactional
    public ForumReplyVO setReplyVisibilityByTeacher(
            Long teacherId, Long replyId, ForumVisibilityRequest request) {
        ForumReplyEntity reply = requireReply(replyId);
        courseManagementService.requireEditor(teacherId, reply.getCourseId());
        return setReplyVisibility(reply, request, teacherId);
    }

    @Transactional
    public ForumReplyVO setReplyVisibilityByAdmin(
            Long adminId, Long replyId, ForumVisibilityRequest request) {
        return setReplyVisibility(requireReply(replyId), request, adminId);
    }

    private ForumTopicDetailVO setTopicVisibility(
            ForumTopicEntity topic, ForumVisibilityRequest request, Long moderatorId) {
        topic.setVersion(request.version());
        topic.setStatus(Boolean.TRUE.equals(request.visible())
                ? ForumContentStatus.VISIBLE.name()
                : ForumContentStatus.HIDDEN.name());
        topic.setModerationReason(trimToNull(request.reason()));
        topic.setModeratedBy(moderatorId);
        topic.setModeratedAt(now());
        updateTopicOrConflict(topic);
        ForumTopicEntity fresh = requireTopic(topic.getId());
        return assembler.toDetail(fresh, authorProfile(fresh.getAuthorId()).name(), authorProfile(fresh.getAuthorId()).avatarFileId());
    }

    private ForumReplyVO setReplyVisibility(
            ForumReplyEntity reply, ForumVisibilityRequest request, Long moderatorId) {
        reply.setVersion(request.version());
        reply.setStatus(Boolean.TRUE.equals(request.visible())
                ? ForumContentStatus.VISIBLE.name()
                : ForumContentStatus.HIDDEN.name());
        reply.setModerationReason(trimToNull(request.reason()));
        reply.setModeratedBy(moderatorId);
        reply.setModeratedAt(now());
        updateReplyOrConflict(reply);
        recalculateTopicReplyStats(reply.getTopicId());
        ForumReplyEntity fresh = requireReply(reply.getId());
        return assembler.toReply(fresh, authorProfile(fresh.getAuthorId()).name(), authorProfile(fresh.getAuthorId()).avatarFileId());
    }

    private PageResponse<ForumTopicListItemVO> topicPage(
            ForumListQuery query,
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ForumTopicEntity> wrapper) {
        IPage<ForumTopicEntity> page = topicMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, AuthorProfile> authors = authorProfiles(page.getRecords().stream()
                .map(ForumTopicEntity::getAuthorId)
                .distinct()
                .toList());
        List<ForumTopicListItemVO> records = page.getRecords().stream()
                .map(topic -> assembler.toListItem(topic, authors.getOrDefault(topic.getAuthorId(), new AuthorProfile(null, null)).name(), authors.getOrDefault(topic.getAuthorId(), new AuthorProfile(null, null)).avatarFileId()))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private PageResponse<ForumReplyVO> replyPage(
            ForumListQuery query,
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ForumReplyEntity> wrapper) {
        IPage<ForumReplyEntity> page = replyMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, AuthorProfile> authors = authorProfiles(page.getRecords().stream()
                .map(ForumReplyEntity::getAuthorId)
                .distinct()
                .toList());
        List<ForumReplyVO> records = page.getRecords().stream()
                .map(reply -> assembler.toReply(reply, authors.getOrDefault(reply.getAuthorId(), new AuthorProfile(null, null)).name(), authors.getOrDefault(reply.getAuthorId(), new AuthorProfile(null, null)).avatarFileId()))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private void applyTopicKeyword(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ForumTopicEntity> wrapper,
            String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(group -> group.like(ForumTopicEntity::getTitle, value)
                    .or()
                    .like(ForumTopicEntity::getContent, value));
        }
    }

    private void applyTopicOrder(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ForumTopicEntity> wrapper) {
        wrapper.orderByDesc(ForumTopicEntity::getPinned)
                .orderByDesc(ForumTopicEntity::getLastRepliedAt)
                .orderByDesc(ForumTopicEntity::getId);
    }

    private void recalculateTopicReplyStats(Long topicId) {
        long count = replyMapper.selectCount(Wrappers.<ForumReplyEntity>lambdaQuery()
                .eq(ForumReplyEntity::getTopicId, topicId)
                .eq(ForumReplyEntity::getStatus, ForumContentStatus.VISIBLE.name()));
        ForumReplyEntity last = replyMapper.selectOne(Wrappers.<ForumReplyEntity>lambdaQuery()
                .eq(ForumReplyEntity::getTopicId, topicId)
                .eq(ForumReplyEntity::getStatus, ForumContentStatus.VISIBLE.name())
                .orderByDesc(ForumReplyEntity::getCreatedAt)
                .orderByDesc(ForumReplyEntity::getId)
                .last("LIMIT 1"));
        topicMapper.update(null, Wrappers.<ForumTopicEntity>lambdaUpdate()
                .eq(ForumTopicEntity::getId, topicId)
                .set(ForumTopicEntity::getReplyCount, (int) count)
                .set(ForumTopicEntity::getLastRepliedAt, last == null ? null : last.getCreatedAt()));
    }

    private ForumTopicEntity requireTopic(Long topicId) {
        ForumTopicEntity topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException(ForumErrorCode.FORUM_TOPIC_NOT_FOUND);
        }
        return topic;
    }

    private ForumReplyEntity requireReply(Long replyId) {
        ForumReplyEntity reply = replyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(ForumErrorCode.FORUM_REPLY_NOT_FOUND);
        }
        return reply;
    }

    private void requireStudentCourseAccess(Long studentId, Long courseId) {
        if (!permissionService.canViewCourseAsStudent(studentId, courseId)) {
            throw new BusinessException(ForumErrorCode.FORUM_FORBIDDEN);
        }
    }

    private Map<Long, AuthorProfile> authorProfiles(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, this::toAuthorProfile, (left, right) -> left));
    }

    private AuthorProfile authorProfile(Long id) {
        return toAuthorProfile(userMapper.selectById(id));
    }

    private AuthorProfile toAuthorProfile(UserEntity user) {
        return user == null
                ? new AuthorProfile(null, null)
                : new AuthorProfile(user.getDisplayName(), user.getAvatarFileId() == null ? null : String.valueOf(user.getAvatarFileId()));
    }

    private record AuthorProfile(String name, String avatarFileId) {}
    private void updateTopicOrConflict(ForumTopicEntity topic) {
        if (topicMapper.updateById(topic) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "Forum topic changed, refresh and retry");
        }
    }

    private void updateReplyOrConflict(ForumReplyEntity reply) {
        if (replyMapper.updateById(reply) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "Forum reply changed, refresh and retry");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
