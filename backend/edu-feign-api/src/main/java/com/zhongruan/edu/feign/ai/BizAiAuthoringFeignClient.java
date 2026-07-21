package com.zhongruan.edu.feign.ai;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.common.constant.ServiceNames;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** AI 服务 → biz 的内部写授权契约。与 ai-context 一样透传用户 JWT，由 biz 重鉴权+归属校验。 */
@FeignClient(
        name = ServiceNames.EDU_BIZ_SERVICE,
        contextId = "bizAiAuthoringFeignClient",
        url = "${BIZ_SERVICE_URL:http://127.0.0.1:18081}",
        path = BizAiAuthoringFeignClient.BASE_PATH)
public interface BizAiAuthoringFeignClient {
    String BASE_PATH = "/_internal/v1/ai-authoring";

    @PostMapping("/question-banks")
    ApiResponse<AiAuthoringResultResponse> createQuestionBank(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiQuestionBankDraftRequest request);

    @PostMapping("/assignments")
    ApiResponse<AiAuthoringResultResponse> createAssignment(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAssignmentDraftRequest request);

    @PostMapping("/exams")
    ApiResponse<AiAuthoringResultResponse> createExam(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiExamDraftRequest request);

    @PostMapping("/announcements")
    ApiResponse<AiAuthoringResultResponse> createAnnouncement(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AiAnnouncementDraftRequest request);
}
