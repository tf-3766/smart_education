package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService.Retrieval;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseKnowledgeToolsTest {

    @Test
    void returnsRetrievedBodyWhenMatched() {
        CourseKnowledgeBaseService kb = mock(CourseKnowledgeBaseService.class);
        when(kb.retrieve(eq(7L), eq(3L), eq("向量检索原理")))
                .thenReturn(new Retrieval(true, "【第2章】向量检索基于近似最近邻...", List.of()));

        CourseKnowledgeTools tools = new CourseKnowledgeTools(kb, 7L, 3L);

        String result = tools.searchCourseKnowledge("向量检索原理");

        assertThat(result).contains("向量检索基于近似最近邻");
    }

    @Test
    void tellsModelWhenNoMatchInsteadOfThrowing() {
        CourseKnowledgeBaseService kb = mock(CourseKnowledgeBaseService.class);
        when(kb.retrieve(eq(7L), eq(null), eq("不存在的知识")))
                .thenReturn(new Retrieval(true, "", List.of()));

        CourseKnowledgeTools tools = new CourseKnowledgeTools(kb, 7L, null);

        String result = tools.searchCourseKnowledge("不存在的知识");

        assertThat(result).contains("未检索到");
    }
}
