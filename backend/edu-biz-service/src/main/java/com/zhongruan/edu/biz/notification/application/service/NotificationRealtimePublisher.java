package com.zhongruan.edu.biz.notification.application.service;

import com.zhongruan.edu.biz.notification.api.vo.NotificationStreamEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationRealtimePublisher {
    private final NotificationStreamService streamService;

    public NotificationRealtimePublisher(NotificationStreamService streamService) {
        this.streamService = streamService;
    }

    public void publishAfterCommit(Long userId, String type, Long notificationId) {
        Runnable publish = () -> streamService.publish(
                userId,
                new NotificationStreamEvent(
                        type,
                        notificationId == null ? null : String.valueOf(notificationId),
                        OffsetDateTime.now(ZoneOffset.UTC)));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
