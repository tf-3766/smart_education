package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiAssignmentDraftRequest;
import com.zhongruan.edu.feign.ai.AiAuthoringResultResponse;
import com.zhongruan.edu.feign.ai.AiQuestionBankDraftRequest;
import com.zhongruan.edu.feign.ai.AiQuestionDraft;
import com.zhongruan.edu.feign.ai.AiQuestionOptionDraft;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CourseAuthoringToolsTest {

    private List<AiQuestionDraft> sampleQuestions() {
        return List.of(new AiQuestionDraft(
                "SINGLE_CHOICE", "向量检索的核心思想是？", "见第2章", "EASY", new BigDecimal("2.00"),
                List.of(new AiQuestionOptionDraft("A", "近似最近邻搜索", true, 1),
                        new AiQuestionOptionDraft("B", "全表扫描", false, 2))));
    }

    private String sampleQuestionsJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(sampleQuestions());
    }

    @Test
    void persistsDraftWithCallerIdentityAndReturnsSummary() throws JsonProcessingException {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        when(client.createQuestionBank(any(), any()))
                .thenReturn(ApiResponse.success(
                        new AiAuthoringResultResponse("QUESTION_BANK", "555", "第2章测验", 1), "trace"));
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateQuestionBank("第2章测验", "自动生成", sampleQuestionsJson());

        assertThat(result).contains("555").contains("待确认");
        ArgumentCaptor<AiQuestionBankDraftRequest> captor =
                ArgumentCaptor.forClass(AiQuestionBankDraftRequest.class);
        verify(client).createQuestionBank(eq("Bearer x"), captor.capture());
        AiQuestionBankDraftRequest sent = captor.getValue();
        assertThat(sent.userId()).isEqualTo(99L);
        assertThat(sent.roleCode()).isEqualTo("TEACHER");
        assertThat(sent.courseId()).isEqualTo(7L);
        assertThat(sent.questions()).hasSize(1);
    }

    @Test
    void emptyQuestionsDoesNotCallBiz() {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateQuestionBank("空", "空", "[]");

        assertThat(result).contains("未提供任何题目");
        verifyNoInteractions(client);
    }

    @Test
    void assignmentDraftCallsBizWithCallerIdentity() {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        when(client.createAssignment(any(), any()))
                .thenReturn(ApiResponse.success(
                        new AiAuthoringResultResponse("ASSIGNMENT", "888", "第2章作业", 0), "trace"));
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateAssignment("第2章作业", "完成以下问答题…", new BigDecimal("100"), 5);

        assertThat(result).contains("888").contains("待确认");
        ArgumentCaptor<AiAssignmentDraftRequest> captor =
                ArgumentCaptor.forClass(AiAssignmentDraftRequest.class);
        verify(client).createAssignment(eq("Bearer x"), captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(7L);
        assertThat(captor.getValue().title()).isEqualTo("第2章作业");
        assertThat(captor.getValue().roleCode()).isEqualTo("TEACHER");
    }

    @Test
    void assignmentBlankTitleDoesNotCallBiz() {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateAssignment("   ", "x", new BigDecimal("100"), 5);

        assertThat(result).contains("标题为空");
        verifyNoInteractions(client);
    }

    @Test
    void feignFailureReturnsGracefulMessageInsteadOfThrowing() throws JsonProcessingException {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        when(client.createQuestionBank(any(), any())).thenThrow(new RuntimeException("FORBIDDEN"));
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateQuestionBank("第2章测验", "自动生成", sampleQuestionsJson());

        assertThat(result).contains("落库失败");
    }

    @Test
    void malformedQuestionJsonDoesNotCallBiz() {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        CourseAuthoringTools tools = new CourseAuthoringTools(client, "Bearer x", 99L, "TEACHER", 7L);

        String result = tools.generateQuestionBank("坏数据", "自动生成", "[{\"stem\":\"缺少结尾\"");

        assertThat(result).contains("JSON 无法解析");
        verifyNoInteractions(client);
    }

    @Test
    void emitsStructuredActionAfterDraftCreation() {
        BizAiAuthoringFeignClient client = mock(BizAiAuthoringFeignClient.class);
        when(client.createAssignment(any(), any()))
                .thenReturn(ApiResponse.success(
                        new AiAuthoringResultResponse("ASSIGNMENT", "888", "第2章作业", 0), "trace"));
        var actions = new ArrayList<com.zhongruan.edu.ai.api.vo.AiActionVO>();
        CourseAuthoringTools tools = new CourseAuthoringTools(
                client, "Bearer x", 99L, "TEACHER", 7L, new ObjectMapper(), actions::add);

        tools.generateAssignment("第2章作业", "完成练习", new BigDecimal("100"), 5);

        assertThat(actions).singleElement().satisfies(action -> {
            assertThat(action.capabilityId()).isEqualTo("course.assignment.create");
            assertThat(action.status()).isEqualTo("DRAFT_CREATED");
            assertThat(action.resourceId()).isEqualTo("888");
            assertThat(action.requiresConfirmation()).isTrue();
            assertThat(action.href()).contains("assignmentId=888");
        });
    }
}
