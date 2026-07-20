package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;

/** Read-only tools backed by a role-scoped snapshot returned by the business service. */
public class RoleScopedPlatformTools {
    private final AiAssistantContextResponse context;

    public RoleScopedPlatformTools(AiAssistantContextResponse context) {
        this.context = context;
    }

    @Tool(name = "getEnrollmentWindows", description = "查询数据库中管理员设置的学期选课开放和截止时间；回答选课时间时必须调用")
    public String enrollmentWindows() {
        return format(context.enrollmentWindows(), "当前未设置学期选课时间");
    }

    @Tool(name = "getMyCourses", description = "查询当前登录用户有权查看的课程；不得用于查询其他用户的课程")
    public String courses() {
        return format(context.courses(), "当前账号没有可见课程");
    }

    @Tool(name = "getMyLearningWarnings", description = "查询当前学生本人的学习预警，或当前教师负责课程内的预警；不得查询越权学生数据")
    public String warnings() {
        return format(context.warnings(), "当前授权范围内没有学习预警");
    }

    @Tool(name = "getMyAssignments", description = "查询当前账号授权课程内的作业和截止时间")
    public String assignments() {
        return format(context.assignments(), "当前授权范围内没有作业");
    }

    @Tool(name = "getMyExams", description = "查询当前账号授权课程内的考试安排")
    public String exams() {
        return format(context.exams(), "当前授权范围内没有考试安排");
    }

    @Tool(name = "getPlatformMetrics", description = "查询管理员有权查看的平台汇总指标；非管理员返回空范围")
    public String platformMetrics() {
        return format(context.platformMetrics(), "当前角色没有平台汇总指标权限");
    }

    private String format(List<String> values, String emptyMessage) {
        return values == null || values.isEmpty() ? emptyMessage : String.join("\n", values);
    }
}