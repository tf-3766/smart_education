package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AdminGovernanceDraftVO;
import com.zhongruan.edu.ai.api.vo.AiDraftVO;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Administrator draft tools backed by the same governance service used by the management pages. */
public class AdminGovernanceTools {
    private final AiApplicationService service;
    private final String authorization;
    private final Long userId;
    private final String role;
    private final String traceId;
    private final ObjectMapper objectMapper;
    private final Consumer<AiActionVO> observer;

    public AdminGovernanceTools(
            AiApplicationService service,
            String authorization,
            Long userId,
            String role,
            String traceId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> observer) {
        this.service = service;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.traceId = traceId;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.observer = observer;
    }

    @Tool(
            name = "draftAdminGovernanceReview",
            description = "批量预审待审核教师或检查课程合规性，返回逐项结构化草稿；不会批准教师、驳回教师、发布课程或修改课程。调用前必须先用授权查询工具取得真实 ID。")
    public String draftAdminGovernanceReview(
            @ToolParam(description = "待预审教师用户 ID，多个 ID 用英文逗号分隔；没有则传空字符串")
                    String teacherUserIds,
            @ToolParam(description = "待检查课程 ID，多个 ID 用英文逗号分隔；没有则传空字符串")
                    String courseIds,
            @ToolParam(description = "管理员补充的人工复核或合规标准；没有则传空字符串")
                    String criteria) {
        try {
            if (!"SUPER_ADMIN".equals(role) && teacherUserIds != null && !teacherUserIds.isBlank()) {
                return "治理预审失败：教师注册批量预审仅限超级管理员；普通管理员可继续检查课程合规性。";
            }
            AdminGovernanceDraftVO draft = service.adminGovernanceDraft(
                            authorization, userId, role, ids(teacherUserIds), ids(courseIds), criteria, traceId)
                    .block();
            if (draft == null) return "治理预审失败：未生成结果";
            String capability = draft.courseCompliance().isEmpty()
                    ? "admin.teacher-registration.batch-precheck"
                    : "admin.course.compliance-check";
            observer.accept(new AiActionVO(
                    draft.requestId(), capability, "DRAFT_CREATED", "ADMIN_GOVERNANCE_DRAFT",
                    draft.requestId(), "管理端治理预审草稿",
                    "已检查 %s 项，成功 %s 项，失败 %s 项，需人工复核 %s 项"
                            .formatted(draft.totalCount(), draft.successCount(), draft.failureCount(), draft.reviewCount()),
                    draft.courseCompliance().isEmpty() ? "/admin/users" : "/admin/course-reviews", false));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("requestId", draft.requestId());
            result.put("status", draft.status());
            result.put("totalCount", draft.totalCount());
            result.put("successCount", draft.successCount());
            result.put("failureCount", draft.failureCount());
            result.put("reviewCount", draft.reviewCount());
            result.put("teacherReviews", draft.teacherReviews());
            result.put("courseCompliance", draft.courseCompliance());
            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            return "治理预审失败：" + exception.getMessage();
        }
    }

    @Tool(name = "draftAdminOperationsBrief", description = "根据当前授权平台指标生成每日运营简报、异常信号和可确认处置建议草稿；不会执行任何处置。")
    public String draftOperationsBrief(
            @ToolParam(description = "管理员补充关注重点；没有则传空字符串") String instruction) {
        AiDraftVO draft = service.adminOperationsBrief(authorization, userId, role, instruction, traceId).block();
        if (draft == null) return "运营简报未生成结果，请稍后重试。";
        observer.accept(new AiActionVO(
                draft.requestId(), "admin.operations-brief.generate", "DRAFT_CREATED",
                draft.draftType(), draft.businessId(), "每日运营简报", "已生成运营简报草稿，未执行处置。",
                "/admin/dashboard", false));
        return draft.content();
    }

    private List<Long> ids(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<Long> values = java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    if (!value.matches("\\d+")) throw new IllegalArgumentException("ID 必须为数字");
                    return Long.valueOf(value);
                })
                .distinct()
                .toList();
        if (values.size() > 50) throw new IllegalArgumentException("一次最多处理 50 项");
        return values;
    }
}
