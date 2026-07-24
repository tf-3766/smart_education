package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.ai.api.vo.AiServiceStatusVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import org.springframework.ai.tool.annotation.Tool;

/** Read-only live AI runtime status for administrators. */
public class AdminAiStatusTools {
    private final AiApplicationService service;

    public AdminAiStatusTools(AiApplicationService service) {
        this.service = service;
    }

    @Tool(name = "getAiServiceStatus", description = "查询当前 AI 服务、模型和向量库的实时配置状态；回答 AI 是否可用时必须调用")
    public String status() {
        AiServiceStatusVO status = service.status();
        return "服务=%s，框架=%s %s，provider=%s，model=%s，模型已配置=%s，向量库已配置=%s，检查时间=%s"
                .formatted(status.serviceStatus(), status.framework(), status.frameworkVersion(), status.provider(),
                        status.model(), status.modelConfigured(), status.vectorStoreConfigured(), status.checkedAt());
    }
}
