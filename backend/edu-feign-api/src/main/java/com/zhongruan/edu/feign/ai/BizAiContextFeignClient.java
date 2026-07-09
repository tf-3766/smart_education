package com.zhongruan.edu.feign.ai;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.constant.ServiceNames;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = ServiceNames.EDU_BIZ_SERVICE,
        contextId = "bizAiContextFeignClient",
        path = BizAiContextFeignClient.BASE_PATH)
public interface BizAiContextFeignClient {
    String BASE_PATH = "/_internal/v1/ai-context";

    @PostMapping("/course")
    ApiResponse<AiCourseContextResponse> getCourseContext(@Valid @RequestBody AiCourseContextRequest request);
}
