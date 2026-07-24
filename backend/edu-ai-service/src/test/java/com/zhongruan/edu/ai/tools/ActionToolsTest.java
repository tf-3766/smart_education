package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.api.vo.AiActionVO;
import com.zhongruan.edu.ai.api.vo.AdminGovernanceDraftVO;
import com.zhongruan.edu.ai.api.vo.AiServiceStatusVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiActionPlanRequest;
import com.zhongruan.edu.feign.ai.AiActionResponse;
import com.zhongruan.edu.feign.ai.BizAiActionFeignClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Arrays;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Mono;

class ActionToolsTest {
    @Test
    void administratorToolCreatesPersistentWaitingConfirmationPlan() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        when(client.plan(any(), any())).thenReturn(ApiResponse.success(action(
                "91", "platform.term-enrollment-window.upsert", "更新“2031 春季”选课时间"), "trace"));
        var events = new ArrayList<AiActionVO>();
        AdminActionTools tools = new AdminActionTools(
                client, "Bearer admin", 1003L, "ADMIN", "request-1", new ObjectMapper(), events::add);

        String result = tools.planTermEnrollmentWindow(
                "2031 春季", "2031-02-20T09:00:00+08:00", "2031-03-05T17:00:00+08:00");

        assertThat(result).contains("actionId=91").contains("正式确认卡");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.status()).isEqualTo("WAITING_CONFIRMATION");
            assertThat(event.riskLevel()).isEqualTo("HIGH");
            assertThat(event.preview()).containsEntry("学期", "2031 春季");
        });
        ArgumentCaptor<AiActionPlanRequest> captor = ArgumentCaptor.forClass(AiActionPlanRequest.class);
        verify(client).plan(eq("Bearer admin"), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1003L);
        assertThat(captor.getValue().roleCode()).isEqualTo("ADMIN");
        assertThat(captor.getValue().idempotencyKey()).startsWith("ai:").hasSize(67);
        assertThat(captor.getValue().parametersJson())
                .contains("2031 春季")
                .contains("enrollmentOpenAt");
    }

    @Test
    void teacherToolUsesStringBusinessIdWithoutPrecisionLoss() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        when(client.plan(any(), any())).thenReturn(ApiResponse.success(action(
                "92", "course.assignment.publish", "发布作业"), "trace"));
        TeacherActionTools tools = new TeacherActionTools(
                client, "Bearer teacher", 1002L, "TEACHER", "request-2", new ObjectMapper(), ignored -> {});

        tools.planAssignmentPublication("2079753875052605442");

        ArgumentCaptor<AiActionPlanRequest> captor = ArgumentCaptor.forClass(AiActionPlanRequest.class);
        verify(client).plan(eq("Bearer teacher"), captor.capture());
        assertThat(captor.getValue().parametersJson()).contains("2079753875052605442");
    }

    @Test
    void teacherToolRejectsMissingBusinessIdBeforeCallingBiz() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        TeacherActionTools tools = new TeacherActionTools(
                client, "Bearer teacher", 1002L, "TEACHER", "request-3", new ObjectMapper(), ignored -> {});

        String result = tools.planAssignmentPublication("作业一");

        assertThat(result).contains("缺少有效作业 ID");
        verifyNoInteractions(client);
    }

    @Test
    void administratorReviewToolCreatesStrongConfirmationPlan() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        when(client.plan(any(), any())).thenReturn(ApiResponse.success(new AiActionResponse(
                "93", "admin.teacher-registration.review", "WAITING_CONFIRMATION", "HIGH", "STRONG_CONFIRM",
                "USER", "9001", 0, "通过教师申请", "确认后改变账号状态",
                Map.of("申请人", "李老师"), null, null, null, true,
                null, null, OffsetDateTime.now().plusMinutes(30), null, null, OffsetDateTime.now()), "trace"));
        var events = new ArrayList<AiActionVO>();
        AdminActionTools tools = new AdminActionTools(
                client, "Bearer admin", 1003L, "SUPER_ADMIN", "request-4", new ObjectMapper(), events::add);

        tools.planTeacherRegistrationReview("9001", "approve");

        ArgumentCaptor<AiActionPlanRequest> captor = ArgumentCaptor.forClass(AiActionPlanRequest.class);
        verify(client).plan(eq("Bearer admin"), captor.capture());
        assertThat(captor.getValue().capabilityId()).isEqualTo("admin.teacher-registration.review");
        assertThat(captor.getValue().parametersJson()).contains("APPROVE").contains("9001");
        assertThat(events).singleElement().satisfies(action ->
                assertThat(action.confirmationPolicy()).isEqualTo("STRONG_CONFIRM"));
    }

    @Test
    void teacherGradeToolPreservesDecimalScoreAndPublishChoice() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        when(client.plan(any(), any())).thenReturn(ApiResponse.success(action(
                "94", "course.submission.grade", "确认评分"), "trace"));
        TeacherActionTools tools = new TeacherActionTools(
                client, "Bearer teacher", 1002L, "TEACHER", "request-5", new ObjectMapper(), ignored -> {});

        tools.planSubmissionGrade("932001", new java.math.BigDecimal("86.50"), "建议补充测试", false);

        ArgumentCaptor<AiActionPlanRequest> captor = ArgumentCaptor.forClass(AiActionPlanRequest.class);
        verify(client).plan(eq("Bearer teacher"), captor.capture());
        assertThat(captor.getValue().capabilityId()).isEqualTo("course.submission.grade");
        assertThat(captor.getValue().parametersJson())
                .contains("932001")
                .contains("86.50")
                .contains("\"publishNow\":false");
    }

    @Test
    void ordinaryAdministratorCannotPlanTeacherRegistrationReview() {
        BizAiActionFeignClient client = mock(BizAiActionFeignClient.class);
        AdminActionTools tools = new AdminActionTools(
                client, "Bearer admin", 1005L, "ADMIN", "request-admin", new ObjectMapper(), ignored -> {});

        assertThat(tools.planTeacherRegistrationReview("9001", "APPROVE"))
                .contains("仅限超级管理员");
        verifyNoInteractions(client);
    }

    @Test
    void ordinaryAdministratorToolSetDoesNotRegisterUserGovernanceMethods() {
        List<String> toolNames = Arrays.stream(AdminPlatformTools.class.getMethods())
                .map(method -> method.getAnnotation(Tool.class))
                .filter(annotation -> annotation != null)
                .map(Tool::name)
                .toList();

        assertThat(toolNames)
                .contains("planTermEnrollmentWindow", "draftCourseComplianceReview", "draftAdminOperationsBrief")
                .doesNotContain("planTeacherRegistrationReview", "draftAdminGovernanceReview");
    }

    @Test
    void administratorCanQueryLiveAiServiceStatus() {
        AiApplicationService service = mock(AiApplicationService.class);
        when(service.status()).thenReturn(new AiServiceStatusVO(
                "UP", "Spring AI", "1.1.8", "openai", "qwen-plus", true, true, OffsetDateTime.now()));

        assertThat(new AdminAiStatusTools(service).status())
                .contains("服务=UP", "qwen-plus", "模型已配置=true", "向量库已配置=true");
    }

    @Test
    void administratorCanTriggerGovernanceDraftFromNaturalLanguageTool() {
        AiApplicationService service = mock(AiApplicationService.class);
        AdminGovernanceDraftVO draft = new AdminGovernanceDraftVO(
                "draft-1", "DRAFT", 1, 1, 0, 1,
                List.of(new AdminGovernanceDraftVO.TeacherReviewItem(
                        "9001", 2, "teacher_li", "李老师", OffsetDateTime.now(),
                        "李老师", "MANUAL_REVIEW", 0.7, true,
                        List.of("IDENTITY_EVIDENCE_REQUIRED"), List.of("需要人工核验"), List.of("待审核快照"))),
                List.of(), OffsetDateTime.now());
        when(service.adminGovernanceDraft(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(draft));
        var events = new ArrayList<AiActionVO>();
        AdminGovernanceTools tools = new AdminGovernanceTools(
                service, "Bearer admin", 1003L, "SUPER_ADMIN", "trace-1", new ObjectMapper(), events::add);

        String result = tools.draftAdminGovernanceReview("9001", "", "核验教师身份");

        assertThat(result).contains("draft-1").contains("MANUAL_REVIEW");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.capabilityId()).isEqualTo("admin.teacher-registration.batch-precheck");
            assertThat(event.status()).isEqualTo("DRAFT_CREATED");
            assertThat(event.requiresConfirmation()).isFalse();
        });
    }

    private AiActionResponse action(String id, String capabilityId, String title) {
        return new AiActionResponse(
                id, capabilityId, "WAITING_CONFIRMATION", "HIGH", "EXPLICIT_CONFIRM",
                "TERM_ENROLLMENT_WINDOW", null, null, title, "确认后执行",
                Map.of("学期", "2031 春季"), null, null, null, true,
                null, null, OffsetDateTime.now().plusMinutes(30), null, null, OffsetDateTime.now());
    }
}
