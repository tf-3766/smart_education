package com.zhongruan.edu.biz.auth.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.auth.api.dto.request.LoginRequest;
import com.zhongruan.edu.biz.auth.api.dto.request.RegisterRequest;
import com.zhongruan.edu.biz.auth.api.dto.request.UpdateAvatarRequest;
import com.zhongruan.edu.biz.auth.api.vo.CurrentUserVO;
import com.zhongruan.edu.biz.auth.api.vo.LoginVO;
import com.zhongruan.edu.biz.auth.api.vo.RegistrationVO;
import com.zhongruan.edu.biz.auth.domain.AuthErrorCode;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.domain.enums.RegistrationRole;
import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.PermissionMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.RoleMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserRoleMapper;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.security.JwtProperties;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService;
import com.zhongruan.edu.biz.storage.domain.FilePurpose;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.common.security.JwtTokenService;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final FileStorageService fileStorageService;

    public AuthApplicationService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            FileStorageService fileStorageService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public LoginVO login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, normalizeUsername(request.username()))
                .eq(UserEntity::getUserStatus, UserStatus.ENABLED.name()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        return issueLogin(user, null);
    }

    @Transactional
    public RegistrationVO register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username)) > 0) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        boolean approvalRequired = request.role() == RegistrationRole.TEACHER;
        user.setUserStatus((approvalRequired ? UserStatus.PENDING : UserStatus.ENABLED).name());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        RoleEntity role = roleMapper.selectOne(Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getRoleCode, request.role().name())
                .eq(RoleEntity::getEnabled, 1));
        if (role == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "注册角色配置不存在");
        }
        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(user.getId());
        relation.setRoleId(role.getId());
        userRoleMapper.insert(relation);
        LoginVO login = approvalRequired ? null : issueLogin(user, request.role().name());
        return new RegistrationVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                request.role().name(),
                user.getUserStatus(),
                approvalRequired,
                login);
    }

    private LoginVO issueLogin(UserEntity user, String preferredRole) {

        Set<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        Set<String> permissions = permissionMapper.findPermissionCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "当前账号没有可用角色");
        }
        String activeRole = preferredRole != null && roles.contains(preferredRole)
                ? preferredRole
                : selectActiveRole(roles);
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

    @Transactional
    public CurrentUserVO updateAvatar(AuthenticatedUser principal, UpdateAvatarRequest request) {
        UserEntity user = userMapper.selectById(principal.userId());
        if (user == null || !UserStatus.ENABLED.name().equals(user.getUserStatus())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (request.fileId() != null) {
            fileStorageService.requireOwnedFile(principal.userId(), request.fileId(), FilePurpose.AVATAR);
        }
        user.setAvatarFileId(request.fileId());
        user.setVersion(request.version());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
        }
        return currentUser(principal);
    }

    private CurrentUserVO toCurrentUser(
            UserEntity user, String activeRole, Set<String> roles, Set<String> permissions) {
        return new CurrentUserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarFileId() == null ? null : String.valueOf(user.getAvatarFileId()),
                user.getAvatarFileId() == null ? null : fileStorageService.accessUrl(user.getAvatarFileId()),
                activeRole,
                roles,
                permissions,
                user.getVersion());
    }

    private String selectActiveRole(Set<String> roles) {
        return roles.stream()
                .sorted(Comparator.comparingInt(this::rolePriority))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    }

    private int rolePriority(String role) {
        if (RoleCode.SUPER_ADMIN.name().equals(role)) {
            return 0;
        }
        if (RoleCode.ADMIN.name().equals(role)) {
            return 1;
        }
        if (RoleCode.TEACHER.name().equals(role)) {
            return 2;
        }
        if (RoleCode.STUDENT.name().equals(role)) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
