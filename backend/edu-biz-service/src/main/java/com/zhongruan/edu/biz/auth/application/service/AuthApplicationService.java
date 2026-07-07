package com.zhongruan.edu.biz.auth.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.auth.api.dto.request.LoginRequest;
import com.zhongruan.edu.biz.auth.api.vo.CurrentUserVO;
import com.zhongruan.edu.biz.auth.api.vo.LoginVO;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.PermissionMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.security.JwtProperties;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.common.security.JwtTokenService;
import java.util.Comparator;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthApplicationService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional(readOnly = true)
    public LoginVO login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, request.username().trim())
                .eq(UserEntity::getUserStatus, UserStatus.ENABLED.name()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        Set<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        Set<String> permissions = permissionMapper.findPermissionCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "当前账号没有可用角色");
        }
        String activeRole = selectActiveRole(roles);
        JwtTokenService.IssuedToken token =
                jwtTokenService.issue(user.getId(), user.getUsername(), activeRole, roles, permissions);
        return new LoginVO(
                token.value(),
                "Bearer",
                jwtProperties.ttl().toSeconds(),
                token.expiresAt(),
                toCurrentUser(user, activeRole, roles, permissions),
                roles,
                permissions);
    }

    @Transactional(readOnly = true)
    public CurrentUserVO currentUser(AuthenticatedUser principal) {
        UserEntity user = userMapper.selectById(principal.userId());
        if (user == null || !UserStatus.ENABLED.name().equals(user.getUserStatus())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        Set<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        Set<String> permissions = permissionMapper.findPermissionCodesByUserId(user.getId());
        String activeRole = roles.contains(principal.activeRole()) ? principal.activeRole() : selectActiveRole(roles);
        return toCurrentUser(user, activeRole, roles, permissions);
    }

    private CurrentUserVO toCurrentUser(
            UserEntity user, String activeRole, Set<String> roles, Set<String> permissions) {
        return new CurrentUserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                activeRole,
                roles,
                permissions);
    }

    private String selectActiveRole(Set<String> roles) {
        return roles.stream()
                .sorted(Comparator.comparingInt(this::rolePriority))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    }

    private int rolePriority(String role) {
        if (RoleCode.STUDENT.name().equals(role)) {
            return 0;
        }
        if (RoleCode.TEACHER.name().equals(role)) {
            return 1;
        }
        if (RoleCode.ADMIN.name().equals(role)) {
            return 2;
        }
        return Integer.MAX_VALUE;
    }
}
