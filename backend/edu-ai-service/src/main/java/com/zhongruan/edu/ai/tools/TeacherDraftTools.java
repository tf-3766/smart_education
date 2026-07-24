package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.ai.api.vo.AiDraftVO;
import com.zhongruan.edu.ai.api.vo.BatchGradingDraftVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.math.BigDecimal;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Existing safe teacher draft endpoints exposed to the global natural-language assistant. */
public class TeacherDraftTools {
    private final AiApplicationService service;
    private final String authorization;
    private final Long userId;
    private final String role;
    private final String traceId;
    private final Consumer<AiActionVO> observer;

    public TeacherDraftTools(
            AiApplicationService service, String authorization, Long userId, String role, String traceId,
            Consumer<AiActionVO> observer) {
        this.service = service;
        this.authorization = authorization;
        this.userId = userId;
        this.role = role;
        this.traceId = traceId;
        this.observer = observer;
    }

    @Tool(name = "draftLessonSummary", description = "为教师负责课程的指定课时生成摘要草稿；不会修改或发布课时。课程ID和课时ID必须来自授权课程检查。")
    public String lessonSummary(String courseId, String lessonId) {
        AiDraftVO draft = service.lessonSummary(
                authorization, userId, role, id(courseId, "课程"), id(lessonId, "课时"), traceId).block();
        return emit(draft, "course.lesson-summary.generate", "课时摘要草稿", "/teacher/courses/" + courseId + "/content");
    }

    @Tool(name = "draftSubmissionComment", description = "为教师负责课程内的一份作业提交生成评语和分数建议草稿；不会保存或发布成绩。提交ID必须来自 getCourseSubmissions。")
    public String submissionComment(String submissionId, String instruction) {
        AiDraftVO draft = service.submissionComment(
                authorization, userId, role, id(submissionId, "提交"), instruction, traceId).block();
        return emit(draft, "course.submission.comment.generate", "作业评语草稿",
                "/teacher/assignments?submissionId=" + submissionId);
    }

    @Tool(name = "draftBatchGrading", description = "一次为最多50份授权提交生成结构化分数、评语和置信度建议；不会批量保存或发布成绩。低置信度和异常答案自动进入复核。")
    public String batchGrading(
            @ToolParam(description = "提交ID，多个用英文逗号分隔，必须来自 getCourseSubmissions") String submissionIds,
            @ToolParam(description = "教师评分标准") String rubric,
            @ToolParam(description = "人工复核阈值，0到1，建议0.75") Double reviewThreshold,
            @ToolParam(description = "补充要求；没有则传空字符串") String instruction) {
        List<Long> ids = ids(submissionIds);
        BatchGradingDraftVO draft = service.batchGradingDraft(
                authorization, userId, role, ids, rubric, reviewThreshold, instruction, traceId).block();
        if (draft == null) return "批量辅助批改未生成结果，请稍后重试。";
        observer.accept(new AiActionVO(
                draft.requestId(), "course.submission.batch-grade-assist", "DRAFT_CREATED",
                "BATCH_GRADING_DRAFT", draft.requestId(), "批量辅助批改建议",
                "已生成 %s 份建议，其中 %s 份进入人工复核；不会批量保存或发布成绩。"
                        .formatted(draft.totalCount(), draft.reviewCount()),
                "/teacher/assignments?aiBatch=" + draft.requestId(), false));
        return "已生成 %s 份批改建议，%s 份需要人工复核。请逐份回到原始提交确认，系统未批量保存或发布成绩。"
                .formatted(draft.totalCount(), draft.reviewCount());
    }

    @Tool(name = "explainLearningWarning", description = "根据授权预警证据生成教师解读草稿；不会改变预警状态。预警ID必须来自 getCourseLearningWarnings。")
    public String warningExplanation(String warningId, String instruction) {
        AiDraftVO draft = service.warningExplanation(
                authorization, userId, role, id(warningId, "预警"), instruction, traceId).block();
        return emit(draft, "course.warning.explain", "学习预警解读", "/teacher/warnings?warningId=" + warningId);
    }

    @Tool(name = "draftRiskInterventionPlan", description = "根据授权预警生成学生提醒、补救材料、补交任务和复查计划草稿；不会通知学生或创建正式任务。")
    public String riskIntervention(String warningId, String instruction) {
        AiDraftVO draft = service.warningInterventionPlan(
                authorization, userId, role, id(warningId, "预警"), instruction, traceId).block();
        return emit(draft, "course.risk-intervention.plan", "风险干预计划草稿",
                "/teacher/warnings?warningId=" + warningId);
    }

    @Tool(name = "draftTeachingPackagePlan", description = "根据教师负责课程生成资料梳理、教案、摘要、作业、题目和公告的完整执行计划草稿；计划本身不写业务数据。")
    public String teachingPackage(String courseId, String instruction) {
        AiDraftVO draft = service.teachingPackagePlan(
                authorization, userId, role, id(courseId, "课程"), instruction, traceId).block();
        return emit(draft, "course.teaching-package.plan", "教学包执行计划",
                "/teacher/courses?courseId=" + courseId);
    }

    @Tool(name = "draftPaperSuggestion", description = "从教师负责课程的授权题库生成组卷建议草稿；不会创建或发布正式试卷。")
    public String paperSuggestion(
            String courseId,
            @ToolParam(description = "建议题目数量，1到100") Integer questionCount,
            @ToolParam(description = "试卷总分，必须大于0") BigDecimal totalScore,
            @ToolParam(description = "题型、难度、章节覆盖等要求") String requirements) {
        AiDraftVO draft = service.paperSuggestion(
                authorization, userId, role, id(courseId, "课程"), questionCount, totalScore, requirements, traceId).block();
        return emit(draft, "course.paper-suggestion.generate", "组卷建议草稿",
                "/teacher/exams?courseId=" + courseId);
    }

    private String emit(AiDraftVO draft, String capabilityId, String title, String href) {
        if (draft == null) return title + "未生成结果，请稍后重试。";
        observer.accept(new AiActionVO(
                draft.requestId(), capabilityId, "DRAFT_CREATED", draft.draftType(), draft.businessId(),
                title, "AI 草稿已生成，正式业务数据未改变。", href, false));
        return draft.content();
    }

    private Long id(String value, String label) {
        if (value == null || !value.matches("\\d+")) throw new IllegalArgumentException(label + "ID必须为数字");
        return Long.valueOf(value);
    }

    private List<Long> ids(String csv) {
        if (csv == null || csv.isBlank()) throw new IllegalArgumentException("至少提供一个提交ID");
        List<Long> values = Arrays.stream(csv.split(",")).map(String::trim)
                .filter(value -> !value.isBlank()).map(value -> id(value, "提交")).distinct().toList();
        if (values.isEmpty() || values.size() > 50) throw new IllegalArgumentException("一次必须处理1到50份提交");
        return values;
    }
}
