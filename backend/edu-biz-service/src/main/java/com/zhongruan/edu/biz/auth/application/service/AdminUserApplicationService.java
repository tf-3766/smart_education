package com.zhongruan.edu.biz.auth.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.auth.api.dto.query.AdminUserQuery;
import com.zhongruan.edu.biz.auth.api.vo.AdminUserVO;
import com.zhongruan.edu.biz.auth.domain.AuthErrorCode;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserRoleMapper;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserApplicationService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserApplicationService(
            UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserVO> listUsers(Long operatorId, AdminUserQuery query) {
        requireSuperAdministrator(operatorId);
        var wrapper = Wrappers.<UserEntity>lambdaQuery();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(group -> group.like(UserEntity::getUsername, keyword)
                    .or()
                    .like(UserEntity::getDisplayName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(UserEntity::getUserStatus, query.getStatus().name());
        }
        wrapper.orderByDesc(UserEntity::getCreatedAt).orderByDesc(UserEntity::getId);
        IPage<UserEntity> page = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<AdminUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public AdminUserVO grantAdministrator(Long operatorId, Long userId) {
        requireSuperAdministrator(operatorId);
        UserEntity user = requireUser(userId);
        Set<String> roles = roleMapper.findRoleCodesByUserId(userId);
        if (!UserStatus.ENABLED.name().equals(user.getUserStatus())
                || !(roles.contains(RoleCode.STUDENT.name()) || roles.contains(RoleCode.TEACHER.name()))) {
            throw new BusinessException(
                    CommonErrorCode.OPERATION_NOT_ALLOWED, "只能将已启用的学生或教师设为管理员");
        }
        RoleEntity adminRole = requireRole(RoleCode.ADMIN);
        if (userRoleMapper.countActiveRole(userId, adminRole.getId()) == 0) {
            int restored = userRoleMapper.restoreRole(userId, adminRole.getId(), operatorId);
            if (restored == 0) {
                UserRoleEntity relation = new UserRoleEntity();
                relation.setUserId(userId);
                relation.setRoleId(adminRole.getId());
                try {
                    userRoleMapper.insert(relation);
                } catch (DuplicateKeyException exception) {
                    if (userRoleMapper.countActiveRole(userId, adminRole.getId()) == 0) {
                        throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
                    }
                }
            }
        }
        return toVO(user);
    }

    @Transactional
    public AdminUserVO revokeAdministrator(Long operatorId, Long userId) {
        requireSuperAdministrator(operatorId);
        UserEntity user = requireUser(userId);
        Set<String> roles = roleMapper.findRoleCodesByUserId(userId);
        if (roles.contains(RoleCode.SUPER_ADMIN.name())) {
            throw new BusinessException(AuthErrorCode.SUPER_ADMIN_PROTECTED);
        }
        RoleEntity adminRole = requireRole(RoleCode.ADMIN);
        userRoleMapper.revokeRole(userId, adminRole.getId(), operatorId);
        return toVO(user);
    }

    @Transactional
    public AdminUserVO approveTeacherRegistration(Long operatorId, Long userId) {
        requireSuperAdministrator(operatorId);
        UserEntity user = requirePendingTeacher(userId);
        user.setUserStatus(UserStatus.ENABLED.name());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        return toVO(user);
    }

    @Transactional
    public AdminUserVO rejectTeacherRegistration(Long operatorId, Long userId) {
        requireSuperAdministrator(operatorId);
        UserEntity user = requirePendingTeacher(userId);
        user.setUserStatus(UserStatus.REJECTED.name());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        return toVO(user);
    }

    private UserEntity requirePendingTeacher(Long userId) {
        UserEntity user = requireUser(userId);
        Set<String> roles = roleMapper.findRoleCodesByUserId(userId);
        if (!UserStatus.PENDING.name().equals(user.getUserStatus()) || !roles.contains(RoleCode.TEACHER.name())) {
            throw new BusinessException(AuthErrorCode.TEACHER_REGISTRATION_NOT_PENDING);
        }
        return user;
    }

    private void requireSuperAdministrator(Long userId) {
        Set<String> roles = roleMapper.findRoleCodesByUserId(userId);
        if (!roles.contains(RoleCode.SUPER_ADMIN.name())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private RoleEntity requireRole(RoleCode roleCode) {
        RoleEntity role = roleMapper.selectOne(Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getRoleCode, roleCode.name())
                .eq(RoleEntity::getEnabled, 1));
        if (role == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "系统角色配置不存在");
        }
        return role;
    }

    private AdminUserVO toVO(UserEntity user) {
        Set<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        OffsetDateTime createdAt = user.getCreatedAt() == null
                ? null
                : user.getCreatedAt().atOffset(ZoneOffset.UTC);
        return new AdminUserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getUserStatus(),
                roles,
                roles.contains(RoleCode.SUPER_ADMIN.name()),
                createdAt,
                user.getVersion());
    }
}
