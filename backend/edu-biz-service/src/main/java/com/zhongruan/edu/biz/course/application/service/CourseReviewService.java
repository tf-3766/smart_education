package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.api.dto.query.CourseListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.RejectCourseRequest;
import com.zhongruan.edu.biz.course.api.dto.request.ReviewCourseRequest;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewListItemVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewVO;
import com.zhongruan.edu.biz.course.application.assembler.CourseAssembler;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseReviewEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseReviewMapper;
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
public class CourseReviewService {
    private final CourseMapper courseMapper;
    private final CourseReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final CourseManagementService courseManagementService;
    private final CourseAssembler assembler;
    private final Clock clock = Clock.systemUTC();

    public CourseReviewService(
            CourseMapper courseMapper,
            CourseReviewMapper reviewMapper,
            UserMapper userMapper,
            CourseManagementService courseManagementService,
            CourseAssembler assembler) {
        this.courseMapper = courseMapper;
        this.reviewMapper = reviewMapper;
        this.userMapper = userMapper;
        this.courseManagementService = courseManagementService;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseReviewListItemVO> list(CourseListQuery query) {
        var wrapper = Wrappers.<CourseEntity>lambdaQuery();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(group -> group.like(CourseEntity::getName, keyword)
                    .or()
                    .like(CourseEntity::getCourseCode, keyword));
        }
        if (query.getReviewStatus() != null) {
            wrapper.eq(CourseEntity::getReviewStatus, query.getReviewStatus().name());
        }
        if (query.getTerm() != null && !query.getTerm().isBlank()) {
            wrapper.eq(CourseEntity::getTerm, query.getTerm().trim());
        }
        wrapper.orderByDesc(CourseEntity::getUpdatedAt).orderByDesc(CourseEntity::getId);
        IPage<CourseEntity> page = courseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<CourseReviewListItemVO> records = page.getRecords().stream()
                .map(course -> assembler.toReviewListItem(course, userName(course.getOwnerTeacherId())))
                .toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional(readOnly = true)
    public CourseReviewDetailVO detail(Long courseId) {
        CourseEntity course = courseManagementService.requireCourse(courseId);
        List<CourseReviewVO> history = reviewMapper.selectList(Wrappers.<CourseReviewEntity>lambdaQuery()
                        .eq(CourseReviewEntity::getCourseId, courseId)
                        .orderByDesc(CourseReviewEntity::getReviewedAt)
                        .orderByDesc(CourseReviewEntity::getId))
                .stream()
                .map(review -> assembler.toReview(review, userName(review.getReviewerId())))
                .toList();
        String reason = history.isEmpty() ? null : history.getFirst().reason();
        return new CourseReviewDetailVO(
                assembler.toDetail(course, userName(course.getOwnerTeacherId()), reason, true), history);
    }

    @Transactional
    public CourseReviewVO approve(Long adminId, Long courseId, ReviewCourseRequest request) {
        CourseEntity course = requirePending(courseId);
        courseManagementService.alignWithApprovedDefinition(course);
        course.setReviewStatus(CourseReviewStatus.APPROVED.name());
        updateCourse(course);
        return record(courseId, adminId, CourseReviewStatus.APPROVED, null, request == null ? null : trim(request.remark()));
    }

    @Transactional
    public CourseReviewVO reject(Long adminId, Long courseId, RejectCourseRequest request) {
        CourseEntity course = requirePending(courseId);
        course.setStatus(CourseStatus.DRAFT.name());
        course.setReviewStatus(CourseReviewStatus.REJECTED.name());
        updateCourse(course);
        return record(courseId, adminId, CourseReviewStatus.REJECTED, request.reason().trim(), null);
    }

    private CourseEntity requirePending(Long courseId) {
        CourseEntity course = courseManagementService.requireCourse(courseId);
        if (!CourseStatus.PENDING_REVIEW.name().equals(course.getStatus())
                || !CourseReviewStatus.PENDING.name().equals(course.getReviewStatus())) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课程不在待审核状态");
        }
        return course;
    }

    private CourseReviewVO record(
            Long courseId,
            Long adminId,
            CourseReviewStatus status,
            String reason,
            String remark) {
        CourseReviewEntity review = new CourseReviewEntity();
        review.setCourseId(courseId);
        review.setReviewStatus(status.name());
        review.setReviewerId(adminId);
        review.setReason(reason);
        review.setRemark(remark);
        review.setReviewedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        reviewMapper.insert(review);
        return assembler.toReview(review, userName(adminId));
    }

    private void updateCourse(CourseEntity course) {
        if (courseMapper.updateById(course) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课程审核状态已变化，请刷新后重试");
        }
    }

    private String userName(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
