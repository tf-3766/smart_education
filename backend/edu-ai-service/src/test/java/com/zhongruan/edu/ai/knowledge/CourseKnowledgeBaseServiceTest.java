package com.zhongruan.edu.ai.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.ObjectProvider;

class CourseKnowledgeBaseServiceTest {
    @Test
    void syncsAuthorizedLessonChunksAndReturnsFilteredRetrieval() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document(
                "course-21001-lesson-23001-chunk-0",
                "依赖注入通过容器提供对象所需依赖。",
                Map.of(
                        "courseId", 21001L,
                        "lessonId", 23001L,
                        "resourceType", "LESSON",
                        "resourceId", "23001",
                        "title", "依赖注入",
                        "locator", "lesson:23001"))));
        CourseKnowledgeBaseService service = new CourseKnowledgeBaseService(provider);
        AiCourseContextResponse context = new AiCourseContextResponse(
                21001L,
                "COURSE-001",
                "Spring 课程",
                "PUBLISHED",
                "APPROVED",
                1002L,
                true,
                false,
                List.of(new AiLessonRef(
                        23001L,
                        22001L,
                        "依赖注入",
                        "PUBLISHED",
                        "RICH_TEXT",
                        "依赖注入通过容器提供对象所需依赖，从而降低耦合。",
                        30)),
                List.of());

        var status = service.sync(context);
        var retrieval = service.retrieve(21001L, 23001L, "什么是依赖注入？");

        assertThat(status.vectorStoreConfigured()).isTrue();
        assertThat(status.indexedChunks()).isEqualTo(1);
        assertThat(retrieval.matched()).isTrue();
        assertThat(retrieval.context()).contains("依赖注入通过容器提供对象所需依赖");
        assertThat(retrieval.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.resourceId()).isEqualTo("23001");
            assertThat(citation.title()).isEqualTo("依赖注入");
        });
        ArgumentCaptor<Filter.Expression> deleteFilter = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(deleteFilter.capture());
        Filter.Expression deleteCourseFilter = (Filter.Expression) deleteFilter.getValue().left();
        assertThat(((Filter.Value) deleteCourseFilter.right()).value()).isEqualTo("21001");
        verify(vectorStore).add(any());

        ArgumentCaptor<SearchRequest> searchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchRequest.capture());
        Filter.Expression combined = searchRequest.getValue().getFilterExpression();
        Filter.Expression courseFilter = (Filter.Expression) combined.left();
        Filter.Expression lessonFilter = (Filter.Expression) combined.right();
        assertThat(((Filter.Value) courseFilter.right()).value()).isEqualTo("21001");
        assertThat(((Filter.Value) lessonFilter.right()).value()).isEqualTo("23001");
    }
    @Test
    void splitsEmbeddingWritesIntoProviderSafeBatches() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        CourseKnowledgeBaseService service = new CourseKnowledgeBaseService(provider);
        List<AiLessonRef> lessons = java.util.stream.LongStream.rangeClosed(1, 23)
                .mapToObj(id -> new AiLessonRef(
                        id, 22001L, "课时 " + id, "PUBLISHED", "RICH_TEXT", "课时正文 " + id, 30))
                .toList();
        AiCourseContextResponse context = new AiCourseContextResponse(
                21001L, "COURSE-001", "Spring 课程", "PUBLISHED", "APPROVED",
                1002L, true, false, lessons, List.of());

        var status = service.sync(context);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> batches = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(3)).add(batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(10, 10, 3);
        assertThat(status.indexedChunks()).isEqualTo(23);
    }

    @Test
    void keepsPreviousIndexWhenEmbeddingFails() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        doThrow(new IllegalStateException("embedding unavailable")).when(vectorStore).add(any());
        CourseKnowledgeBaseService service = new CourseKnowledgeBaseService(provider);
        AiCourseContextResponse context = new AiCourseContextResponse(
                21001L, "COURSE-001", "Spring 课程", "PUBLISHED", "APPROVED",
                1002L, true, false,
                List.of(new AiLessonRef(23001L, 22001L, "依赖注入", "PUBLISHED",
                        "RICH_TEXT", "依赖注入正文", 30)),
                List.of());

        assertThatThrownBy(() -> service.sync(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("embedding unavailable");
        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }
    @Test
    void fallsBackWhenVectorStoreIsTemporarilyUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("qdrant unavailable"));

        CourseKnowledgeBaseService service = new CourseKnowledgeBaseService(provider);
        var retrieval = service.retrieve(21001L, null, "课程重点是什么？");

        assertThat(retrieval.vectorStoreAvailable()).isFalse();
        assertThat(retrieval.matched()).isFalse();
        assertThat(retrieval.citations()).isEmpty();
    }
}
