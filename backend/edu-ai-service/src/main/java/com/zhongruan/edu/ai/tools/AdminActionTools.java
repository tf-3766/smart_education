package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class AdminActionTools {
    private static final String TERM_WINDOW_CAPABILITY = "platform.term-enrollment-window.upsert";
    private static final String TEACHER_REVIEW_CAPABILITY = "admin.teacher-registration.review";
    private final AiActionToolSupport support;
    private final boolean superAdmin;

    public AdminActionTools(
            BizAiActionFeignClient client,
            String authorization,
            Long userId,
            String role,
            String requestId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> observer) {
        this.support = new AiActionToolSupport(
                client, authorization, userId, role, requestId, objectMapper, observer);
        this.superAdmin = "SUPER_ADMIN".equals(role);
    }

    @Tool(
            name = "planTermEnrollmentWindow",
            description = "为管理员创建学期统一选课时间的待确认动作计划；不会立即修改。"
                    + "仅当用户明确要求设置或修改某学期选课窗口时调用。时间必须为带时区的 ISO-8601。")
    public String planTermEnrollmentWindow(
            @ToolParam(description = "学期，严格格式如 2027 春季 或 2027 秋季") String term,
            @ToolParam(description = "选课开放时间，带时区 ISO-8601，如 2027-02-20T09:00:00+08:00；不限时传空字符串")
                    String enrollmentOpenAt,
            @ToolParam(description = "选课截止时间，带时区 ISO-8601，如 2027-03-05T17:00:00+08:00；不限时传空字符串")
                    String enrollmentCloseAt) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("term", term == null ? null : term.trim());
        parameters.put("enrollmentOpenAt", blankToNull(enrollmentOpenAt));
        parameters.put("enrollmentCloseAt", blankToNull(enrollmentCloseAt));
        return support.plan(TERM_WINDOW_CAPABILITY, parameters);
    }

    @Tool(
            name = "planTeacherRegistrationReview",
            description = "为超级管理员创建待审核教师注册的通过或驳回计划；不会立即改变账号状态。"
                    + "调用前必须从授权上下文得到准确用户 ID。")
    public String planTeacherRegistrationReview(
            @ToolParam(description = "待审核教师的用户 ID，必须来自授权上下文") String userId,
            @ToolParam(description = "审核决定，只能是 APPROVE 或 REJECT") String decision) {
        if (!superAdmin) {
            return "动作计划创建失败：教师注册审核仅限超级管理员。";
        }
        if (userId == null || !userId.matches("\\d+")) {
            return "动作计划创建失败：缺少有效用户 ID，请先查询待审核教师。";
        }
        if (decision == null || !decision.trim().matches("(?i)APPROVE|REJECT")) {
            return "动作计划创建失败：审核决定只能是 APPROVE 或 REJECT。";
        }
        return support.plan(TEACHER_REVIEW_CAPABILITY, Map.of(
                "userId", userId,
                "decision", decision.trim().toUpperCase(java.util.Locale.ROOT)));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
