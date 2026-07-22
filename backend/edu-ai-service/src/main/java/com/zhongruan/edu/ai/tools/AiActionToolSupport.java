package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiActionPlanRequest;
import com.zhongruan.edu.feign.ai.AiActionResponse;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;

final class AiActionToolSupport {
    private final BizAiActionFeignClient client;
    private final String authorization;
    private final Long userId;
    private final String role;
    private final String requestId;
    private final ObjectMapper objectMapper;
    private final Consumer<AiActionVO> observer;

    AiActionToolSupport(
            BizAiActionFeignClient client,
            String authorization,
            Long userId,
            String role,
            String requestId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> observer) {
        this.client = client;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.requestId = requestId;
        this.objectMapper = objectMapper;
        this.observer = observer;
    }

    String plan(String capabilityId, Map<String, ?> parameters) {
        try {
            String parametersJson = objectMapper.writeValueAsString(parameters);
            String idempotencyKey = idempotencyKey(capabilityId, parametersJson);
            ApiResponse<AiActionResponse> response = client.plan(
                    authorization,
                    new AiActionPlanRequest(userId, role, capabilityId, idempotencyKey, parametersJson));
            AiActionResponse action = response == null ? null : response.data();
            if (action == null) {
                return "动作计划创建失败：业务服务未返回计划。";
            }
            observer.accept(AiActionVO.from(action));
            return "已创建待确认动作计划，actionId=%s。必须由当前用户在正式确认卡中确认后才会执行。"
                    .formatted(action.actionId());
        } catch (JsonProcessingException exception) {
            return "动作计划创建失败：参数无法序列化。";
        } catch (RuntimeException exception) {
            return "动作计划创建失败：" + safeMessage(exception);
        }
    }

    private String idempotencyKey(String capabilityId, String parametersJson) {
        String material = requestId + "|" + capabilityId + "|" + parametersJson;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return "ai:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "业务服务暂不可用" : message;
    }
}
