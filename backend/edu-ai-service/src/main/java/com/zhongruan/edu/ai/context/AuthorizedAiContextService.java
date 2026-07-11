package com.zhongruan.edu.ai.context;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiContextPurpose;
import com.zhongruan.edu.feign.ai.AiCourseContextRequest;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import org.springframework.stereotype.Service;

@Service
public class AuthorizedAiContextService {
    private final BizAiContextFeignClient contextClient;

    public AuthorizedAiContextService(BizAiContextFeignClient contextClient) {
        this.contextClient = contextClient;
    }

    public AiCourseContextResponse courseContext(
            String authorization,
            Long userId,
            String role,
            Long courseId,
            Long lessonId,
            AiContextPurpose purpose,
            String traceId) {
        ApiResponse<AiCourseContextResponse> response = contextClient.getCourseContext(
                authorization,
                new AiCourseContextRequest(userId, role, courseId, lessonId, null, purpose, traceId));
        if (response == null || response.data() == null || !"SUCCESS".equals(response.code())) {
            throw new BusinessException(CommonErrorCode.AI_SERVICE_UNAVAILABLE, "无法获取授权课程上下文");
        }
        return response.data();
    }
}
