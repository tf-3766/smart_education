package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiAuthoringResultResponse;
import com.zhongruan.edu.feign.ai.AiQuestionBankDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionDraft;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 教师/管理员自动流写工具：把 LLM 依据课程资料生成的题目落库为待确认草稿（source=AI）。
 * 仅在教师/管理员的课程上下文中注入；学生不可用。工具本身不生成题目——
 * 由 LLM 先调用 searchCourseKnowledge 取材、再生成题目并作为参数传入本工具。
 */
public class CourseAuthoringTools {
    private static final Logger log = LoggerFactory.getLogger(CourseAuthoringTools.class);

    private final BizAiAuthoringFeignClient authoringClient;
    private final String authorization;
    private final Long userId;
    private final String role;
    private final Long courseId;

    public CourseAuthoringTools(
            BizAiAuthoringFeignClient authoringClient,
            String authorization,
            Long userId,
            String role,
            Long courseId) {
        this.authoringClient = authoringClient;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.courseId = courseId;
    }

    @Tool(name = "generateQuestionBank",
            description = """
                    把已生成的题目落库为当前课程的“AI 草稿题库”，供教师确认后发布。
                    调用前必须先用 searchCourseKnowledge 检索课程资料正文，据此出题，不得凭空捏造。
                    questions 为完整题目列表：questionType 取 SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/FILL_BLANK/SHORT_ANSWER 之一；
                    difficulty 取 EASY/MEDIUM/HARD；score 为该题分值(>0)；选择题必须给出 options（label、content、correct、sortOrder），
                    并至少标记一个 correct=true。落库后返回题库 ID 与题量，须提示教师到题库页面确认，不得声称已正式发布。""")
    public String generateQuestionBank(String bankName, String description, List<AiQuestionDraft> questions) {
        if (questions == null || questions.isEmpty()) {
            return "未提供任何题目，题库草稿未创建。请先检索资料并生成题目后再调用。";
        }
        String safeName = bankName == null || bankName.isBlank() ? "AI 生成题库草稿" : bankName.trim();
        AiQuestionBankDraftRequest request = new AiQuestionBankDraftRequest(
                userId, role, courseId, safeName, description, questions, null);
        try {
            ApiResponse<AiAuthoringResultResponse> response =
                    authoringClient.createQuestionBank(authorization, request);
            AiAuthoringResultResponse result = response == null ? null : response.data();
            if (result == null) {
                return "题库草稿创建请求已发送，但未返回结果，请稍后到题库页面核对。";
            }
            return "已创建 AI 草稿题库《%s》，题库 ID=%s，含 %d 道题，状态为待确认（AI 草稿）。请提示教师到该课程的题库页面确认后发布。"
                    .formatted(result.title(), result.resourceId(), result.itemCount());
        } catch (RuntimeException exception) {
            log.warn("AI 题库草稿落库失败 courseId={} reason={}", courseId, exception.toString());
            return "题库草稿落库失败：" + exception.getMessage()
                    + "。可能是无该课程权限或题目字段不合法，请核对后重试，切勿声称已成功。";
        }
    }
}
