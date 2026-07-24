package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.feign.ai.AiAssistantContextResponse;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;

/** Builds an exact read-only tool set for the active role; unauthorized tools are not exposed to the model. */
public final class RoleScopedPlatformTools {
    private RoleScopedPlatformTools() {}

    public static Object[] forRole(AiAssistantContextResponse context, String role) {
        return switch (role) {
            case "STUDENT" -> new Object[] {new CommonTools(context), new StudentTools(context)};
            case "TEACHER" -> new Object[] {new CommonTools(context), new TeacherTools(context)};
            case "ADMIN" -> new Object[] {new CommonTools(context), new AdminTools(context)};
            case "SUPER_ADMIN" -> new Object[] {
                    new CommonTools(context), new AdminTools(context), new SuperAdminTools(context)};
            default -> new Object[] {};
        };
    }

    public static final class CommonTools {
        private final AiAssistantContextResponse context;
        private CommonTools(AiAssistantContextResponse context) { this.context = context; }

        @Tool(name = "getEnrollmentWindows", description = "查询数据库中管理员设置的学期选课开放和截止时间；回答选课时间时必须调用")
        public String enrollmentWindows() { return format(context.enrollmentWindows(), "当前未设置学期选课时间"); }

        @Tool(name = "getMyCourses", description = "查询当前登录用户有权查看的课程；不得用于查询其他用户的课程")
        public String courses() { return format(context.courses(), "当前账号没有可见课程"); }

        @Tool(name = "getMyNotifications", description = "查询当前登录用户本人的通知，不得查询其他用户通知")
        public String notifications() { return format(context.notifications(), "当前账号没有通知"); }
    }

    public static final class StudentTools {
        private final AiAssistantContextResponse context;
        private StudentTools(AiAssistantContextResponse context) { this.context = context; }

        @Tool(name = "getMyLearningWarnings", description = "查询当前学生本人的学习预警；不得查询其他学生数据")
        public String warnings() { return format(context.warnings(), "本人当前没有学习预警"); }

        @Tool(name = "getMyAssignments", description = "查询本人授权课程内已发布的作业和截止时间")
        public String assignments() { return format(context.assignments(), "本人当前没有可见作业"); }

        @Tool(name = "getMyExams", description = "查询本人授权课程内已发布的考试安排")
        public String exams() { return format(context.exams(), "本人当前没有可见考试"); }

        @Tool(name = "getMyLearningProgress", description = "查询当前学生本人的课时学习进度")
        public String progress() { return format(context.learningProgress(), "本人当前没有学习进度记录"); }

        @Tool(name = "getMyPublishedGrades", description = "查询当前学生本人的已发布成绩和教师反馈；未发布成绩不可见")
        public String grades() { return format(context.grades(), "本人当前没有已发布成绩"); }

        @Tool(name = "getMyAnnouncements", description = "查询当前学生可见的已发布系统或课程公告")
        public String announcements() { return format(context.announcements(), "本人当前没有可见公告"); }

        @Tool(name = "getMyForumActivity", description = "查询当前学生有权查看的课程讨论")
        public String forum() { return format(context.forumActivity(), "本人当前没有可见讨论"); }
    }

    public static final class TeacherTools {
        private final AiAssistantContextResponse context;
        private TeacherTools(AiAssistantContextResponse context) { this.context = context; }

        @Tool(name = "getCourseLearningWarnings", description = "查询当前教师负责课程内的学习预警")
        public String warnings() { return format(context.warnings(), "负责课程当前没有学习预警"); }

        @Tool(name = "getCourseAssignments", description = "查询当前教师负责课程内的作业和状态")
        public String assignments() { return format(context.assignments(), "负责课程当前没有作业"); }

        @Tool(name = "getCourseExams", description = "查询当前教师负责课程内的考试安排")
        public String exams() { return format(context.exams(), "负责课程当前没有考试"); }

        @Tool(name = "getCourseLearningProgress", description = "查询当前教师负责课程内的学生学习进度")
        public String progress() { return format(context.learningProgress(), "负责课程当前没有学习进度记录"); }

        @Tool(name = "getCourseGrades", description = "查询当前教师负责课程内的成绩与反馈状态")
        public String grades() { return format(context.grades(), "负责课程当前没有成绩记录"); }

        @Tool(name = "getCourseSubmissions", description = "查询当前教师负责课程内的待批改和已批改提交")
        public String submissions() { return format(context.submissions(), "负责课程当前没有作业提交"); }

        @Tool(name = "getCourseAnnouncements", description = "查询当前教师负责课程内的公告草稿和发布状态")
        public String announcements() { return format(context.announcements(), "负责课程当前没有公告"); }

        @Tool(name = "getCourseForumActivity", description = "查询当前教师负责课程内的讨论与治理状态")
        public String forum() { return format(context.forumActivity(), "负责课程当前没有讨论"); }
    }

    public static final class AdminTools {
        private final AiAssistantContextResponse context;
        private AdminTools(AiAssistantContextResponse context) { this.context = context; }

        @Tool(name = "getPlatformMetrics", description = "查询管理员有权查看的平台用户、课程、作业、考试和预警汇总指标")
        public String metrics() { return format(context.platformMetrics(), "当前没有平台汇总指标"); }

        @Tool(name = "getPlatformAnnouncements", description = "查询管理员有权查看的平台和课程公告治理状态")
        public String announcements() { return format(context.announcements(), "当前没有公告治理数据"); }

        @Tool(name = "getPlatformForumActivity", description = "查询管理员有权查看的讨论与内容治理状态")
        public String forum() { return format(context.forumActivity(), "当前没有讨论治理数据"); }
    }

    public static final class SuperAdminTools {
        private final AiAssistantContextResponse context;
        private SuperAdminTools(AiAssistantContextResponse context) { this.context = context; }

        @Tool(name = "getPendingTeacherRegistrations", description = "仅超级管理员查询待审核教师注册；预审时必须保留用户 ID，不得自动审批")
        public String pendingTeachers() { return format(context.pendingTeacherRegistrations(), "当前没有待审核教师注册"); }

        @Tool(name = "getPlatformUsers", description = "仅超级管理员查询用户账号、状态和版本")
        public String users() { return format(context.users(), "当前没有用户明细"); }
    }

    private static String format(List<String> values, String emptyMessage) {
        return values == null || values.isEmpty() ? emptyMessage : String.join("\n", values);
    }
}
