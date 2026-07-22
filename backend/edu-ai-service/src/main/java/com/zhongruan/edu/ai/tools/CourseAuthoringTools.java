package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiAnnouncementDraftRequest;
import com.zhongruan.edu.feign.ai.AiAssignmentDraftRequest;
import com.zhongruan.edu.feign.ai.AiAuthoringResultResponse;
import com.zhongruan.edu.feign.ai.AiExamDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionBankDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionDraft;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
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
    private final ObjectMapper objectMapper;
    private final Consumer<AiActionVO> actionObserver;

    public CourseAuthoringTools(
            BizAiAuthoringFeignClient authoringClient,
            String authorization,
            Long userId,
            String role,
            Long courseId) {
        this(authoringClient, authorization, userId, role, courseId, new ObjectMapper(), ignored -> {});
    }

    public CourseAuthoringTools(
            BizAiAuthoringFeignClient authoringClient,
            String authorization,
            Long userId,
            String role,
            Long courseId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> actionObserver) {
        this.authoringClient = authoringClient;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.courseId = courseId;
        this.objectMapper = objectMapper;
        this.actionObserver = actionObserver == null ? ignored -> {} : actionObserver;
    }

    @Tool(name = "generateQuestionBank",
            description = """
                    把已生成的题目落库为当前课程的“AI 草稿题库”，供教师确认后发布。
                    调用前必须先用 searchCourseKnowledge 检索课程资料正文，据此出题，不得凭空捏造。
                    questionsJson 是完整题目 JSON 数组文本。不要把数组作为嵌套工具参数传入；数组元素的 questionType
                    取 SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/FILL_BLANK/SHORT_ANSWER 之一；
                    difficulty 取 EASY/MEDIUM/HARD；score 为该题分值(>0)；选择题必须给出 options（label、content、correct、sortOrder），
                    题干字段只能叫 stem，解析字段只能叫 analysis，不接受 question/text/content 作为题干。
                    questionsJson 示例：[{"questionType":"SINGLE_CHOICE","stem":"Java 的入口方法是？","analysis":"main 是入口方法",
                    "difficulty":"EASY","score":2,"options":[{"label":"A","content":"main","correct":true,"sortOrder":1},
                    {"label":"B","content":"start","correct":false,"sortOrder":2}]}]。
                    并至少标记一个 correct=true。落库后返回题库 ID 与题量，须提示教师到题库页面确认，不得声称已正式发布。""")
    public String generateQuestionBank(String bankName, String description, String questionsJson) {
        List<AiQuestionDraft> questions;
        try {
            questions = parseQuestions(questionsJson);
        } catch (JsonProcessingException exception) {
            return "题目 JSON 无法解析，题库草稿未创建。请修复 JSON 数组后重试：" + exception.getOriginalMessage();
        }
        if (questions.isEmpty()) {
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
            emitCreated("course.question-bank.create", result, "/teacher/exams?bankId=" + result.resourceId());
            return "已创建 AI 草稿题库《%s》，题库 ID=%s，含 %d 道题，状态为待确认（AI 草稿）。请提示教师到该课程的题库页面确认后发布。"
                    .formatted(result.title(), result.resourceId(), result.itemCount());
        } catch (RuntimeException exception) {
            log.warn("AI 题库草稿落库失败 courseId={} reason={}", courseId, exception.toString());
            return "题库草稿落库失败：" + exception.getMessage()
                    + "。可能是无该课程权限或题目字段不合法，请核对后重试，切勿声称已成功。";
        }
    }

    @Tool(name = "generateAssignment",
            description = """
                    把依据课程资料生成的作业落库为当前课程的“AI 草稿作业”（文本作答方式），供教师确认后发布。
                    调用前必须先用 searchCourseKnowledge 检索课程资料正文，据此拟题，不得凭空捏造。
                    title 为作业标题；description 为作业要求与题干正文（可含多道小题的文字描述）；
                    maxScore 为总分(>0)；dueInDays 为建议完成天数(默认 7，教师可调整)。
                    落库后返回作业 ID，须提示教师到作业页面确认发布，不得声称已正式发布。""")
    public String generateAssignment(String title, String description, BigDecimal maxScore, Integer dueInDays) {
        if (title == null || title.isBlank()) {
            return "作业标题为空，未创建草稿。请先检索资料并拟定标题与要求后再调用。";
        }
        BigDecimal score = maxScore == null || maxScore.signum() <= 0 ? new BigDecimal("100") : maxScore;
        AiAssignmentDraftRequest request = new AiAssignmentDraftRequest(
                userId, role, courseId, null, title.trim(), description, score, dueInDays, null);
        try {
            ApiResponse<AiAuthoringResultResponse> response =
                    authoringClient.createAssignment(authorization, request);
            AiAuthoringResultResponse result = response == null ? null : response.data();
            if (result == null) {
                return "作业草稿创建请求已发送，但未返回结果，请稍后到作业页面核对。";
            }
            emitCreated("course.assignment.create", result, "/teacher/assignments?assignmentId=" + result.resourceId());
            return "已创建 AI 草稿作业《%s》，作业 ID=%s，作答方式为文本，状态为待确认（DRAFT）。请提示教师到作业页面确认发布。"
                    .formatted(result.title(), result.resourceId());
        } catch (RuntimeException exception) {
            log.warn("AI 作业草稿落库失败 courseId={} reason={}", courseId, exception.toString());
            return "作业草稿落库失败：" + exception.getMessage() + "。请核对后重试，切勿声称已成功。";
        }
    }

    @Tool(name = "generateExam",
            description = """
                    把依据课程情况拟定的考试落库为当前课程的“AI 草稿考试”（DRAFT）。调用前可先用 searchCourseKnowledge 了解课程范围。
                    title 为考试标题；description 为考试说明；durationMinutes 为考试时长（分钟）；totalScore 为总分(>0)。
                    重要：本工具只创建考试草稿，不会自动选题组卷；落库后须提示教师到考试页面自行组卷选题并发布试卷，不得声称已发布。返回考试 ID。""")
    public String generateExam(String title, String description, Integer durationMinutes, BigDecimal totalScore) {
        if (title == null || title.isBlank()) {
            return "考试标题为空，未创建草稿。请先拟定标题后再调用。";
        }
        BigDecimal score = totalScore == null || totalScore.signum() <= 0 ? new BigDecimal("100") : totalScore;
        AiExamDraftRequest request = new AiExamDraftRequest(
                userId, role, courseId, title.trim(), description, durationMinutes, score, null);
        try {
            ApiResponse<AiAuthoringResultResponse> response = authoringClient.createExam(authorization, request);
            AiAuthoringResultResponse result = response == null ? null : response.data();
            if (result == null) {
                return "考试草稿创建请求已发送，但未返回结果，请稍后到考试页面核对。";
            }
            emitCreated("course.exam.create", result, "/teacher/exams?examId=" + result.resourceId());
            return "已创建 AI 草稿考试《%s》，考试 ID=%s，状态为草稿（DRAFT）。请提示教师到考试页面组卷选题并发布，本工具未自动选题。"
                    .formatted(result.title(), result.resourceId());
        } catch (RuntimeException exception) {
            log.warn("AI 考试草稿落库失败 courseId={} reason={}", courseId, exception.toString());
            return "考试草稿落库失败：" + exception.getMessage() + "。请核对后重试，切勿声称已成功。";
        }
    }

    @Tool(name = "draftAnnouncement",
            description = """
                    把依据课程情况拟定的公告落库为当前课程的“AI 草稿公告”（不会立即推送学生）。调用前可先用 searchCourseKnowledge 了解课程情况。
                    title 为公告标题；content 为公告正文；audience 受众取 STUDENT（仅学生）或 ALL（全部成员，默认 ALL）。
                    落库后返回公告 ID，须提示教师到公告页面确认发布，确认后才会通知学生，不得声称已发布。""")
    public String draftAnnouncement(String title, String content, String audience) {
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return "公告标题或正文为空，未创建草稿。请先拟定完整内容后再调用。";
        }
        AiAnnouncementDraftRequest request = new AiAnnouncementDraftRequest(
                userId, role, courseId, title.trim(), content.trim(), audience, null);
        try {
            ApiResponse<AiAuthoringResultResponse> response =
                    authoringClient.createAnnouncement(authorization, request);
            AiAuthoringResultResponse result = response == null ? null : response.data();
            if (result == null) {
                return "公告草稿创建请求已发送，但未返回结果，请稍后到公告页面核对。";
            }
            emitCreated("course.announcement.create", result, "/teacher/forum?announcementId=" + result.resourceId());
            return "已创建 AI 草稿公告《%s》，公告 ID=%s，尚未推送学生。请提示教师到公告页面确认发布后学生才会收到。"
                    .formatted(result.title(), result.resourceId());
        } catch (RuntimeException exception) {
            log.warn("AI 公告草稿落库失败 courseId={} reason={}", courseId, exception.toString());
            return "公告草稿落库失败：" + exception.getMessage() + "。请核对后重试，切勿声称已成功。";
        }
    }

    private List<AiQuestionDraft> parseQuestions(String questionsJson) throws JsonProcessingException {
        if (questionsJson == null || questionsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(questionsJson, new TypeReference<List<AiQuestionDraft>>() {});
    }

    private void emitCreated(String capabilityId, AiAuthoringResultResponse result, String href) {
        actionObserver.accept(new AiActionVO(
                UUID.randomUUID().toString(),
                capabilityId,
                "DRAFT_CREATED",
                result.resourceType(),
                result.resourceId(),
                result.title(),
                "已创建 AI 草稿，正式生效前需要人工确认。",
                href,
                true));
    }
}
