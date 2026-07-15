package com.zhongruan.edu.biz.notification.application.service;

import com.zhongruan.edu.biz.notification.api.vo.NotificationStreamEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationStreamService {
    private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ignored -> remove(userId, emitter));
        send(userId, emitter, new NotificationStreamEvent("connected", null, OffsetDateTime.now(ZoneOffset.UTC)));
        return emitter;
    }

    public void publish(Long userId, NotificationStreamEvent event) {
        for (SseEmitter emitter : emitters.getOrDefault(userId, Set.of())) {
            send(userId, emitter, event);
        }
    }

    @Scheduled(fixedDelayString = "${edu.notifications.heartbeat-ms:25000}")
    public void heartbeat() {
        NotificationStreamEvent event = new NotificationStreamEvent("heartbeat", null, OffsetDateTime.now(ZoneOffset.UTC));
        emitters.forEach((userId, userEmitters) -> userEmitters.forEach(emitter -> send(userId, emitter, event)));
    }

    private void send(Long userId, SseEmitter emitter, NotificationStreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .reconnectTime(3000)
                    .data(event));
        } catch (IOException | IllegalStateException exception) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId, userEmitters);
        }
    }
}
