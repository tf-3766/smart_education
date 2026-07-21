package com.zhongruan.edu.ai.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

class CourseKnowledgeAutoSyncTest {

    @SuppressWarnings("unchecked")
    private CourseKnowledgeBaseService serviceWith(VectorStore store) {
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return new CourseKnowledgeBaseService(provider);
    }

    private AiCourseContextResponse context(String materialBody) {
        AiMaterialRef material = new AiMaterialRef(
                10L, null, 1L, "第2章.pdf", "PDF", "key", "url",
                "COURSE", "PUBLISHED", materialBody, "EXTRACTED", null);
        AiLessonRef lesson = new AiLessonRef(1L, null, "第2章", "PUBLISHED", "TEXT", "章节说明", 45);
        return new AiCourseContextResponse(
                7L, "CS101", "课程", "PUBLISHED", "APPROVED", 99L, true, true,
                List.of(lesson), List.of(material));
    }

    @Test
    void firstCallIndexesAndSecondIdenticalCallSkips() {
        VectorStore store = mock(VectorStore.class);
        CourseKnowledgeBaseService service = serviceWith(store);

        boolean first = service.syncIfStale(context("向量检索基于近似最近邻搜索"));
        boolean second = service.syncIfStale(context("向量检索基于近似最近邻搜索"));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void changedContentReindexes() {
        VectorStore store = mock(VectorStore.class);
        CourseKnowledgeBaseService service = serviceWith(store);

        service.syncIfStale(context("旧正文"));
        boolean afterChange = service.syncIfStale(context("新正文，内容已更新"));

        assertThat(afterChange).isTrue();
    }
}
