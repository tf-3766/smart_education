package com.zhongruan.edu.biz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zhongruan.edu.biz.auth.domain.enums.UserStatus;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditPersistenceIntegrationTest {
    @Autowired
    private UserMapper userMapper;

    @Test
    void insertAndUpdateApplyAuditFieldsAndOptimisticVersion() {
        UserEntity user = new UserEntity();
        user.setUsername("audit-test-user");
        user.setPasswordHash("not-a-login-fixture");
        user.setDisplayName("审计测试用户");
        user.setUserStatus(UserStatus.ENABLED.name());

        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(0L, user.getCreatedBy());
        assertEquals(0L, user.getUpdatedBy());
        assertEquals(0, user.getDeleted());
        assertEquals(0, user.getVersion());

        user.setDisplayName("已更新的审计测试用户");
        assertEquals(1, userMapper.updateById(user));
        assertEquals(1, user.getVersion());
    }
}

