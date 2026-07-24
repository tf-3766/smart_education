package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Exact write/draft tool set for ordinary administrators; user governance is deliberately absent. */
public class AdminPlatformTools {
    private final AdminActionTools actionTools;
    private final AdminGovernanceTools governanceTools;

    public AdminPlatformTools(
            AiApplicationService service,
            BizAiActionFeignClient actionClient,
            String authorization,
            Long userId,
            String requestId,
            String traceId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> observer) {
        this.actionTools = new AdminActionTools(
                actionClient, authorization, userId, "ADMIN", requestId, objectMapper, observer);
        this.governanceTools = new AdminGovernanceTools(
                service, authorization, userId, "ADMIN", traceId, objectMapper, observer);
    }

    @Tool(name = "planTermEnrollmentWindow", description = "为普通管理员创建学期统一选课时间的待强确认动作计划；不会立即修改。时间必须为带时区的 ISO-8601。")
    public String planTermEnrollmentWindow(
            @ToolParam(description = "学期，如 2027 春季 或 2027 秋季") String term,
            @ToolParam(description = "选课开放时间，带时区 ISO-8601；不限时传空字符串") String openAt,
            @ToolParam(description = "选课截止时间，带时区 ISO-8601；不限时传空字符串") String closeAt) {
        return actionTools.planTermEnrollmentWindow(term, openAt, closeAt);
    }

    @Tool(name = "draftCourseComplianceReview", description = "普通管理员批量检查授权课程的审核状态、课时和资料完整度并生成逐项草稿；不会发布或修改课程。课程 ID 必须来自授权查询。")
    public String draftCourseComplianceReview(
            @ToolParam(description = "课程 ID，多个 ID 用英文逗号分隔，一次最多 50 项") String courseIds,
            @ToolParam(description = "人工合规标准；没有则传空字符串") String criteria) {
        return governanceTools.draftAdminGovernanceReview("", courseIds, criteria);
    }

    @Tool(name = "draftAdminOperationsBrief", description = "根据当前授权平台指标生成每日运营简报、异常信号和可确认处置建议草稿；不会执行任何处置。")
    public String draftOperationsBrief(
            @ToolParam(description = "管理员补充关注重点；没有则传空字符串") String instruction) {
        return governanceTools.draftOperationsBrief(instruction);
    }
}
