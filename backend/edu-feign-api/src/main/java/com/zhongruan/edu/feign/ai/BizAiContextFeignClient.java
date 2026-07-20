package com.zhongruan.edu.feign.ai;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.constant.ServiceNames;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = ServiceNames.EDU_BIZ_SERVICE,
        contextId = "bizAiContextFeignClient",
        url = "${BIZ_SERVICE_URL:http://127.0.0.1:18081}",
        path = BizAiContextFeignClient.BASE_PATH)
public interface BizAiContextFeignClient {
    String BASE_PATH = "/_internal/v1/ai-context";

    @PostMapping("/assistant")
    ApiResponse<AiAssistantContextResponse> getAssistantContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAssistantContextRequest request);

    @PostMapping("/course")
    ApiResponse<AiCourseContextResponse> getCourseContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiCourseContextRequest request);

    @PostMapping("/submission")
    ApiResponse<AiSubmissionContextResponse> getSubmissionContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiResourceContextRequest request);

    @PostMapping("/warning")
    ApiResponse<AiWarningContextResponse> getWarningContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiResourceContextRequest request);

    @PostMapping("/paper")
    ApiResponse<AiPaperContextResponse> getPaperContext(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiPaperContextRequest request);
}
