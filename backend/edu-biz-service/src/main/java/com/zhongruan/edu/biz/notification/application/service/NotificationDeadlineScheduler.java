package com.zhongruan.edu.biz.notification.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeadlineScheduler {
    private final AssignmentMapper assignmentMapper;
    private final NotificationApplicationService notificationService;
    private final Clock clock = Clock.systemUTC();

    public NotificationDeadlineScheduler(
            AssignmentMapper assignmentMapper, NotificationApplicationService notificationService) {
        this.assignmentMapper = assignmentMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${edu.notifications.deadline-scan-ms:60000}")
    @Transactional
    public void publishDueAssignmentNotifications() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getStatus, AssignmentStatus.PUBLISHED.name())
                        .isNotNull(AssignmentEntity::getDueAt)
                        .le(AssignmentEntity::getDueAt, now))
                .forEach(notificationService::publishAssignmentDeadline);
    }
}
