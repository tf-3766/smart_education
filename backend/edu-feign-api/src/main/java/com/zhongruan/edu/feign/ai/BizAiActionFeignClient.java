package com.zhongruan.edu.feign.ai;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.constant.ServiceNames;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** AI 服务到 Biz 的动作计划入口。只创建计划，不直接执行正式业务写操作。 */
@FeignClient(
        name = ServiceNames.EDU_BIZ_SERVICE,
        contextId = "bizAiActionFeignClient",
        url = "${BIZ_SERVICE_URL:http://127.0.0.1:18081}",
        path = BizAiActionFeignClient.BASE_PATH)
public interface BizAiActionFeignClient {
    String BASE_PATH = "/_internal/v1/ai-actions";

    @PostMapping
    ApiResponse<AiActionResponse> plan(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiActionPlanRequest request);
}
