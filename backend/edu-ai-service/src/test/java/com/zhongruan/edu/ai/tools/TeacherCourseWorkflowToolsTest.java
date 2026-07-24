package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongruan.edu.ai.context.AuthorizedAiContextService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.common.api.ApiResponse;
import com.zhongruan.edu.feign.ai.AiAuthoringResultResponse;
import com.zhongruan.edu.feign.ai.AiChapterRef;
import com.zhongruan.edu.feign.ai.AiContextPurpose;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import com.zhongruan.edu.feign.ai.BizAiAuthoringFeignClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class TeacherCourseWorkflowToolsTest {
    @Test
    void requiresAuthorizedInspectionThenCreatesDraftFromAnyPage() {
        AuthorizedAiContextService contextService = mock(AuthorizedAiContextService.class);
        CourseKnowledgeBaseService knowledgeBase = mock(CourseKnowledgeBaseService.class);
        BizAiAuthoringFeignClient authoringClient = mock(BizAiAuthoringFeignClient.class);
        AiCourseContextResponse context = new AiCourseContextResponse(
                21001L, "CS-01", "数据结构", "PUBLISHED", "APPROVED", 1002L, true, false,
                List.of(new AiLessonRef(1L, 2L, "线性表", "PUBLISHED", "TEXT", "顺序表与链表", 45)),
                List.of(new AiMaterialRef(3L, 2L, 1L, "第一章讲义", "DOCUMENT", "k", null,
                        "LESSON", "PUBLISHED", "复杂度与线性结构", "EXTRACTED", null)),
                null, null, null, null, null, null, null, null, null, null,
                List.of(new AiChapterRef(2L, "第一章 线性结构", "覆盖线性表与复杂度", 1, "PUBLISHED")));
        when(contextService.courseContext(any(), any(), any(), eq(21001L), any(), eq(AiContextPurpose.COURSE_QA), any()))
                .thenReturn(context);
        when(knowledgeBase.retrieve(eq(21001L), any(), any()))
                .thenReturn(new CourseKnowledgeBaseService.Retrieval(true, "线性表检索片段", List.of()));
        when(authoringClient.createAssignment(any(), any())).thenReturn(ApiResponse.success(
                new AiAuthoringResultResponse("ASSIGNMENT", "888", "线性表作业", 0), "trace"));
        List<String> toolEvents = new ArrayList<>();
        TeacherCourseWorkflowTools tools = new TeacherCourseWorkflowTools(
                contextService, knowledgeBase, authoringClient, "Bearer teacher", 1002L, "TEACHER",
                "request", "trace", new ObjectMapper(), ignored -> {}, toolEvents::add);

        assertThat(tools.generateAssignment(21001L, "线性表作业", "完成两道题", new BigDecimal("100"), 7))
                .contains("尚未检查");
        verifyNoInteractions(authoringClient);

        assertThat(tools.inspectCourse(21001L, "线性表出题"))
                .contains("数据结构", "第一章 线性结构", "顺序表与链表", "复杂度与线性结构", "线性表检索片段");
        assertThat(tools.generateAssignment(21001L, "线性表作业", "完成两道题", new BigDecimal("100"), 7))
                .contains("888", "AI 草稿");
        assertThat(toolEvents).containsExactly("inspectAuthorizedCourseForAuthoring");

        verify(contextService, times(2)).courseContext(
                any(), any(), any(), eq(21001L), any(), eq(AiContextPurpose.COURSE_QA), any());
        verify(authoringClient).createAssignment(any(), any());
    }
}
