package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.feign.ai.AiContextPurpose;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 教师从任意页面启动课程创作自动流时使用的工具。
 * 每个生成操作都会重新向 Biz 校验课程归属，且同一轮必须先检查课程正文，避免凭空创作。
 */
public class TeacherCourseWorkflowTools {
    private final AuthorizedAiContextService contextService;
    private final CourseKnowledgeBaseService knowledgeBase;
    private final BizAiAuthoringFeignClient authoringClient;
    private final String authorization;
    private final Long userId;
    private final String role;
    private final String traceId;
    private final ObjectMapper objectMapper;
    private final Consumer<AiActionVO> actionObserver;
    private final Consumer<String> toolObserver;
    private final Set<Long> inspectedCourses = new HashSet<>();

    public TeacherCourseWorkflowTools(
            AuthorizedAiContextService contextService,
            CourseKnowledgeBaseService knowledgeBase,
            BizAiAuthoringFeignClient authoringClient,
            String authorization,
            Long userId,
            String role,
            String requestId,
            String traceId,
            ObjectMapper objectMapper,
            Consumer<AiActionVO> actionObserver,
            Consumer<String> toolObserver) {
        this.contextService = contextService;
        this.knowledgeBase = knowledgeBase;
        this.authoringClient = authoringClient;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.traceId = traceId == null || traceId.isBlank() ? requestId : traceId;
        this.objectMapper = objectMapper;
        this.actionObserver = actionObserver;
        this.toolObserver = toolObserver == null ? ignored -> {} : toolObserver;
    }

    @Tool(name = "inspectAuthorizedCourseForAuthoring", description = "教师从任意页面生成题库、作业、考试或公告前必须调用。传入 getMyCourses 返回的课程ID和创作重点；工具会重新校验课程归属并读取章节、课时、资料正文。")
    public String inspectCourse(Long courseId, String focus) {
        AiCourseContextResponse context = authorize(courseId);
        knowledgeBase.syncIfStale(context);
        CourseKnowledgeBaseService.Retrieval retrieval = knowledgeBase.retrieve(
                courseId, null, focus == null || focus.isBlank() ? context.courseName() : focus.trim());
        inspectedCourses.add(courseId);
        toolObserver.accept("inspectAuthorizedCourseForAuthoring");
        String chapters = context.chapters().stream().limit(30)
                .map(item -> "章节ID %s《%s》（状态 %s）：%s".formatted(
                        item.chapterId(), item.title(), item.status(), clip(item.description())))
                .reduce((left, right) -> left + "\n" + right).orElse("无可用章节");
        String lessons = context.lessons().stream().limit(20)
                .map(item -> "课时ID %s《%s》：%s".formatted(item.lessonId(), item.title(), clip(item.content())))
                .reduce((left, right) -> left + "\n" + right).orElse("无可用课时正文");
        String materials = context.materials().stream().limit(20)
                .map(item -> "资料ID %s《%s》：%s".formatted(item.materialId(), item.name(), clip(item.extractedText())))
                .reduce((left, right) -> left + "\n" + right).orElse("无可用资料正文");
        String retrieved = retrieval.matched() && !retrieval.context().isBlank()
                ? "\n相关检索片段：\n" + clip(retrieval.context()) : "";
        return "已授权课程 %s（%s，课程ID %s）。\n课程章节：\n%s\n课时正文：\n%s\n课程资料：\n%s%s"
                .formatted(context.courseName(), context.courseCode(), courseId, chapters, lessons, materials, retrieved);
    }

    @Tool(name = "generateQuestionBankForCourse", description = "为指定已检查课程创建 AI 草稿题库。必须先调用 inspectAuthorizedCourseForAuthoring；questionsJson 格式与 generateQuestionBank 相同。")
    public String generateQuestionBank(Long courseId, String bankName, String description, String questionsJson) {
        CourseAuthoringTools tools = authoringTools(courseId);
        if (tools == null) return notInspected();
        String result = tools.generateQuestionBank(bankName, description, questionsJson);
        toolObserver.accept("generateQuestionBankForCourse");
        return result;
    }

    @Tool(name = "generateAssignmentForCourse", description = "为指定已检查课程创建 AI 草稿作业。必须先调用 inspectAuthorizedCourseForAuthoring。")
    public String generateAssignment(Long courseId, String title, String description, BigDecimal maxScore, Integer dueInDays) {
        CourseAuthoringTools tools = authoringTools(courseId);
        return tools == null ? notInspected() : tools.generateAssignment(title, description, maxScore, dueInDays);
    }

    @Tool(name = "generateExamForCourse", description = "为指定已检查课程创建 AI 草稿考试框架。必须先调用 inspectAuthorizedCourseForAuthoring。")
    public String generateExam(Long courseId, String title, String description, Integer durationMinutes, BigDecimal totalScore) {
        CourseAuthoringTools tools = authoringTools(courseId);
        return tools == null ? notInspected() : tools.generateExam(title, description, durationMinutes, totalScore);
    }

    @Tool(name = "draftAnnouncementForCourse", description = "为指定已检查课程创建 AI 草稿公告，不会直接通知学生。必须先调用 inspectAuthorizedCourseForAuthoring。")
    public String draftAnnouncement(Long courseId, String title, String content, String audience) {
        CourseAuthoringTools tools = authoringTools(courseId);
        return tools == null ? notInspected() : tools.draftAnnouncement(title, content, audience);
    }

    private CourseAuthoringTools authoringTools(Long courseId) {
        if (courseId == null || !inspectedCourses.contains(courseId)) return null;
        authorize(courseId);
        return new CourseAuthoringTools(
                authoringClient, authorization, userId, role, courseId, objectMapper, actionObserver);
    }

    private AiCourseContextResponse authorize(Long courseId) {
        if (!"TEACHER".equals(role)) throw new IllegalStateException("仅教师可启动课程创作自动流");
        if (courseId == null) throw new IllegalArgumentException("必须提供课程ID");
        return contextService.courseContext(
                authorization, userId, role, courseId, null, AiContextPurpose.COURSE_QA, traceId);
    }

    private String notInspected() {
        return "尚未检查该课程的授权资料，未创建草稿。请先调用 inspectAuthorizedCourseForAuthoring。";
    }

    private String clip(String value) {
        if (value == null || value.isBlank()) return "无正文";
        String normalized = value.trim();
        return normalized.length() <= 2000 ? normalized : normalized.substring(0, 2000) + "…";
    }
}
