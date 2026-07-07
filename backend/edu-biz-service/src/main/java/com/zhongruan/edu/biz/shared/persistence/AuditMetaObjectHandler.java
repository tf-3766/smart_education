package com.zhongruan.edu.biz.shared.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    private static final long SYSTEM_ACTOR_ID = 0L;
    private final Clock clock;

    public AuditMetaObjectHandler() {
        this(Clock.systemUTC());
    }

    AuditMetaObjectHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Long actorId = currentActorId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, actorId);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedBy", Long.class, actorId);
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
        strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updatedBy", Long.class, currentActorId());
    }

    private Long currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return SYSTEM_ACTOR_ID;
    }
}

