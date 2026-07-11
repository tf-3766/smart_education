package com.zhongruan.edu.biz.platform.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserRoleMapper;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseEnrollmentMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.exam.domain.enums.ExamStatus;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamEntity;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper.ExamMapper;
import com.zhongruan.edu.biz.platform.api.vo.AdminStatisticsVO;
import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementStatus;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.AnnouncementEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.AnnouncementMapper;
import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.LearningWarningEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.mapper.LearningWarningMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatisticsService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final ExamMapper examMapper;
    private final LearningWarningMapper warningMapper;
    private final AnnouncementMapper announcementMapper;

    public AdminStatisticsService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            CourseMapper courseMapper,
            CourseEnrollmentMapper enrollmentMapper,
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            ExamMapper examMapper,
            LearningWarningMapper warningMapper,
            AnnouncementMapper announcementMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.courseMapper = courseMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.examMapper = examMapper;
        this.warningMapper = warningMapper;
        this.announcementMapper = announcementMapper;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsVO overview() {
        return new AdminStatisticsVO(
                userMapper.selectCount(null),
                userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUserStatus, UserStatus.ENABLED.name())),
                roleCount(RoleCode.STUDENT),
                roleCount(RoleCode.TEACHER),
                roleCount(RoleCode.ADMIN),
                courseMapper.selectCount(null),
                courseMapper.selectCount(Wrappers.<CourseEntity>lambdaQuery()
                        .eq(CourseEntity::getStatus, CourseStatus.PUBLISHED.name())),
                courseMapper.selectCount(Wrappers.<CourseEntity>lambdaQuery()
                        .eq(CourseEntity::getReviewStatus, CourseReviewStatus.PENDING.name())),
                enrollmentMapper.selectCount(Wrappers.<CourseEnrollmentEntity>lambdaQuery()
                        .eq(CourseEnrollmentEntity::getStatus, EnrollmentStatus.ENROLLED.name())),
                assignmentMapper.selectCount(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getStatus, AssignmentStatus.PUBLISHED.name())),
                submissionMapper.selectCount(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                        .in(AssignmentSubmissionEntity::getStatus,
                                SubmissionStatus.SUBMITTED.name(), SubmissionStatus.GRADED.name())),
                examMapper.selectCount(Wrappers.<ExamEntity>lambdaQuery()
                        .eq(ExamEntity::getStatus, ExamStatus.PUBLISHED.name())),
                warningMapper.selectCount(Wrappers.<LearningWarningEntity>lambdaQuery()
                        .eq(LearningWarningEntity::getWarningStatus, WarningStatus.OPEN.name())),
                announcementMapper.selectCount(Wrappers.<AnnouncementEntity>lambdaQuery()
                        .eq(AnnouncementEntity::getStatus, AnnouncementStatus.PUBLISHED.name())));
    }

    private long roleCount(RoleCode roleCode) {
        RoleEntity role = roleMapper.selectOne(Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getRoleCode, roleCode.name()));
        return role == null ? 0 : userRoleMapper.selectCount(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getRoleId, role.getId()));
    }
}
