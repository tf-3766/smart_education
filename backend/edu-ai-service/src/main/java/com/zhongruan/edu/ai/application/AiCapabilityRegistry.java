package com.zhongruan.edu.ai.application;

import com.zhongruan.edu.ai.api.vo.AiCapabilityVO;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiCapabilityRegistry {
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final Set<String> ALL_ROLES = Set.of("STUDENT", "TEACHER", "ADMIN", "SUPER_ADMIN");

    private final List<Definition> definitions = List.of(
            definition("platform.authorized-context", "授权数据问答", "按当前账号权限查询真实业务数据",
                    ALL_ROLES, "ANSWER", "READ_ONLY", Set.of(), "NONE", null),
            definition("course.knowledge.qa", "课程知识问答", "检索已授权课时和资料正文并附引用",
                    ALL_ROLES, "ANSWER", "READ_ONLY", Set.of("courseId"), "NONE", null),

            definition("student.learning-overview.query", "学习概览查询", "查询本人课程、进度、成绩和预警",
                    Set.of("STUDENT"), "ANSWER", "READ_ONLY", Set.of(), "NONE", "/student/dashboard"),
            definition("student.task.query", "作业考试查询", "查询本人待办作业和考试安排",
                    Set.of("STUDENT"), "ANSWER", "READ_ONLY", Set.of(), "NONE", "/student/assignments"),
            definition("student.study-plan.generate", "复习计划生成", "基于本人课程任务生成可调整的学习计划",
                    Set.of("STUDENT"), "DRAFT", "LOW", Set.of(), "DRAFT_REVIEW", "/student/dashboard"),

            definition("teacher.teaching-overview.query", "教学任务查询", "查询负责课程、作业、考试和学习预警",
                    Set.of("TEACHER"), "ANSWER", "READ_ONLY", Set.of(), "NONE", "/teacher/dashboard"),
            definition("course.teaching-package.plan", "教学包自动流", "基于课程资料规划教案、摘要、作业、题目和公告草稿",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/courses"),
            definition("course.risk-intervention.plan", "风险干预计划", "根据学习预警生成提醒、补救材料和补交任务计划草稿",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/warnings"),
            definition("course.question-bank.create", "生成题库草稿", "基于课程资料创建待审核题库与题目",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/exams"),
            definition("course.assignment.create", "生成作业草稿", "基于课程上下文创建待发布作业",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/assignments"),
            definition("course.exam.create", "生成考试草稿", "创建待编排和发布的考试外壳",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/exams"),
            definition("course.announcement.create", "生成公告草稿", "创建待确认发布的课程公告",
                    Set.of("TEACHER"), "DRAFT", "MEDIUM", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/forum"),
            definition("course.assignment.publish", "确认发布作业", "为已有草稿作业生成发布预览并等待确认",
                    Set.of("TEACHER"), "ACTION", "MEDIUM", Set.of("assignmentId"), "EXPLICIT_CONFIRM", "/teacher/assignments"),
            definition("course.submission.batch-grade-assist", "批量辅助批改", "按评分标准生成分数与评语建议，低置信度和异常答案进入人工复核",
                    Set.of("TEACHER"), "DRAFT", "HIGH", Set.of("courseId"), "DRAFT_REVIEW", "/teacher/assignments"),
            definition("course.submission.grade", "评分与反馈", "为授权提交生成分数和评语预览，确认后保存或发布",
                    Set.of("TEACHER"), "ACTION", "HIGH", Set.of("submissionId"), "EXPLICIT_CONFIRM", "/teacher/assignments"),

            definition("admin.platform-overview.query", "平台运营查询", "查询用户、课程、审核和教学运营指标",
                    ADMIN_ROLES, "ANSWER", "READ_ONLY", Set.of(), "NONE", "/admin/dashboard"),
            definition("platform.term-enrollment-window.upsert", "设置学期选课时间", "预览并确认学期统一选课窗口变更",
                    ADMIN_ROLES, "ACTION", "HIGH", Set.of(), "STRONG_CONFIRM", "/admin/term-enrollment"),
            definition("admin.operations-brief.generate", "运营简报生成", "汇总平台指标、异常信号和可确认的处置建议",
                    ADMIN_ROLES, "DRAFT", "LOW", Set.of(), "DRAFT_REVIEW", "/admin/dashboard"),
            definition("admin.teacher-registration.review", "教师注册审核", "预览并确认待审核教师的通过或驳回",
                    ADMIN_ROLES, "ACTION", "HIGH", Set.of("userId"), "STRONG_CONFIRM", "/admin/users"));

    public List<AiCapabilityVO> available(String role, Long courseId) {
        return definitions.stream()
                .filter(definition -> definition.roles().contains(role))
                .map(definition -> toView(definition, courseId))
                .toList();
    }

    public Definition require(String capabilityId) {
        return definitions.stream()
                .filter(item -> item.capabilityId().equals(capabilityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI capability: " + capabilityId));
    }

    private AiCapabilityVO toView(Definition definition, Long courseId) {
        boolean missingCourse = definition.requiredContext().contains("courseId") && courseId == null;
        return new AiCapabilityVO(
                definition.capabilityId(), definition.name(), definition.description(), definition.roles(),
                definition.mode(), definition.riskLevel(), definition.requiredContext(),
                definition.confirmationPolicy(), definition.deepLinkTemplate(), !missingCourse,
                missingCourse ? "进入具体课程后可用" : null);
    }

    private static Definition definition(
            String capabilityId,
            String name,
            String description,
            Set<String> roles,
            String mode,
            String riskLevel,
            Set<String> requiredContext,
            String confirmationPolicy,
            String deepLinkTemplate) {
        return new Definition(
                capabilityId, name, description, roles, mode, riskLevel,
                requiredContext, confirmationPolicy, deepLinkTemplate);
    }

    public record Definition(
            String capabilityId,
            String name,
            String description,
            Set<String> roles,
            String mode,
            String riskLevel,
            Set<String> requiredContext,
            String confirmationPolicy,
            String deepLinkTemplate) {}
}
