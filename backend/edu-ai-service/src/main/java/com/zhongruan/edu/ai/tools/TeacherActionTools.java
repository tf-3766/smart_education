package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TeacherActionTools {
    private static final String ASSIGNMENT_PUBLISH_CAPABILITY = "course.assignment.publish";
    private static final String SUBMISSION_GRADE_CAPABILITY = "course.submission.grade";
    private final AiActionToolSupport support;

    public TeacherActionTools(
            BizAiActionFeignClient client,
            String authorization,
            Long userId,
            String role,
            String requestId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> observer) {
        this.support = new AiActionToolSupport(
                client, authorization, userId, role, requestId, objectMapper, observer);
    }

    @Tool(
            name = "planAssignmentPublication",
            description = "为教师创建发布已有草稿作业的待确认动作计划；不会立即发布。"
                    + "仅当用户明确要求发布某个作业且已有准确作业 ID 时调用。")
    public String planAssignmentPublication(
            @ToolParam(description = "要发布的作业 ID，必须来自当前授权业务上下文") String assignmentId) {
        if (assignmentId == null || !assignmentId.matches("\\d+")) {
            return "动作计划创建失败：缺少有效作业 ID，请先查询授权作业。";
        }
        return support.plan(ASSIGNMENT_PUBLISH_CAPABILITY, Map.of("assignmentId", assignmentId));
    }

    @Tool(
            name = "planSubmissionGrade",
            description = "为教师创建单个作业提交的评分待确认计划；不会立即保存或发布成绩。"
                    + "必须使用授权上下文中的提交 ID，分数不得超过满分。")
    public String planSubmissionGrade(
            @ToolParam(description = "作业提交 ID，必须来自当前教师授权上下文") String submissionId,
            @ToolParam(description = "建议分数，十进制数字") java.math.BigDecimal score,
            @ToolParam(description = "给学生的评语，不超过 1000 字") String teacherComment,
            @ToolParam(description = "true 表示确认后立即发布正式成绩；false 表示仅保存评分草稿") boolean publishNow) {
        if (submissionId == null || !submissionId.matches("\\d+")) {
            return "动作计划创建失败：缺少有效提交 ID，请先查询授权提交。";
        }
        if (score == null) {
            return "动作计划创建失败：缺少建议分数。";
        }
        Map<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("submissionId", submissionId);
        parameters.put("score", score);
        parameters.put("teacherComment", teacherComment == null ? null : teacherComment.trim());
        parameters.put("aiCommentDraftId", null);
        parameters.put("publishNow", publishNow);
        return support.plan(SUBMISSION_GRADE_CAPABILITY, parameters);
    }
}
