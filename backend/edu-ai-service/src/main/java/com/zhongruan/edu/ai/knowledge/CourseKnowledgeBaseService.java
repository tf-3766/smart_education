package com.zhongruan.edu.ai.knowledge;

import com.zhongruan.edu.ai.api.vo.AiCitationVO;
import com.zhongruan.edu.ai.api.vo.AiKnowledgeBaseStatusVO;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import com.zhongruan.edu.feign.ai.AiLessonRef;
import com.zhongruan.edu.feign.ai.AiMaterialRef;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CourseKnowledgeBaseService {
    private static final Logger log = LoggerFactory.getLogger(CourseKnowledgeBaseService.class);
    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;
    // DashScope text-embedding-v3 accepts at most 10 inputs per request. Spring AI's
    // Qdrant store embeds the entire add(...) list in one request, so keep each add
    // below that provider limit instead of relying on a provider-specific setting.
    private static final int EMBEDDING_BATCH_SIZE = 10;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final Map<Long, SyncState> syncStates = new ConcurrentHashMap<>();

    public CourseKnowledgeBaseService(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public boolean configured() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    public AiKnowledgeBaseStatusVO status(Long courseId) {
        SyncState state = syncStates.get(courseId);
        return new AiKnowledgeBaseStatusVO(
                String.valueOf(courseId), configured(), state == null ? 0 : state.indexedChunks(),
                state == null ? null : state.lastSyncedAt());
    }

    public AiKnowledgeBaseStatusVO sync(AiCourseContextResponse context) {
        VectorStore store = requireVectorStore();
        FilterExpressionBuilder filters = new FilterExpressionBuilder();
        store.delete(filters.eq("courseId", context.courseId()).build());
        List<Document> documents = documents(context);
        for (int start = 0; start < documents.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, documents.size());
            store.add(documents.subList(start, end));
        }
        syncStates.put(context.courseId(), new SyncState(documents.size(), OffsetDateTime.now(ZoneOffset.UTC)));
        return status(context.courseId());
    }

    public Retrieval retrieve(Long courseId, Long lessonId, String question) {
        VectorStore store = vectorStoreProvider.getIfAvailable();
        if (store == null) {
            return Retrieval.unavailable();
        }
        FilterExpressionBuilder filters = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = filters.eq("courseId", courseId);
        if (lessonId != null) {
            filter = filters.and(filter, filters.eq("lessonId", lessonId));
        }
        List<Document> documents;
        try {
            documents = store.similaritySearch(SearchRequest.builder()
                    .query(question).topK(6).similarityThreshold(0.45)
                    .filterExpression(filter.build()).build());
        } catch (RuntimeException exception) {
            log.warn("Course knowledge retrieval failed; falling back to authorized lesson context. courseId={} lessonId={} reason={}",
                    courseId, lessonId, exception.getClass().getSimpleName());
            return Retrieval.unavailable();
        }
        if (documents == null || documents.isEmpty()) {
            return new Retrieval(true, "", List.of());
        }
        List<AiCitationVO> citations = documents.stream().map(this::citation).toList();
        String context = documents.stream()
                .map(document -> "【%s】\n%s".formatted(
                        document.getMetadata().getOrDefault("title", "课程知识片段"), document.getText()))
                .reduce((left, right) -> left + "\n\n" + right).orElse("");
        return new Retrieval(true, context, citations);
    }

    private VectorStore requireVectorStore() {
        VectorStore store = vectorStoreProvider.getIfAvailable();
        if (store == null) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "知识库尚未配置，请先配置嵌入模型与 Qdrant");
        }
        return store;
    }

    private List<Document> documents(AiCourseContextResponse context) {
        List<Document> documents = new ArrayList<>();
        for (AiLessonRef lesson : context.lessons()) {
            if (lesson.content() == null || lesson.content().isBlank()) continue;
            List<String> chunks = chunks(lesson.content().trim());
            for (int index = 0; index < chunks.size(); index++) {
                String id = documentId("course-%d-lesson-%d-chunk-%d".formatted(context.courseId(), lesson.lessonId(), index));
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("courseId", context.courseId());
                metadata.put("lessonId", lesson.lessonId());
                metadata.put("resourceType", "LESSON");
                metadata.put("resourceId", String.valueOf(lesson.lessonId()));
                metadata.put("title", lesson.title());
                metadata.put("locator", "lesson:" + lesson.lessonId());
                documents.add(new Document(id, chunks.get(index), metadata));
            }
        }
        for (AiMaterialRef material : context.materials()) {
            if (!"PUBLISHED".equals(material.status())) continue;
            String metadataText = "课程资料：%s\n类型：%s\n归属课时：%s\n正文抽取状态：%s".formatted(
                    material.name(), material.materialType(),
                    material.lessonId() == null ? "课程/章节级" : material.lessonId(),
                    material.extractionStatus() == null ? "UNKNOWN" : material.extractionStatus());
            String sourceText = material.extractedText() == null || material.extractedText().isBlank()
                    ? metadataText + "\n说明：" + (material.extractionMessage() == null ? "暂无可索引正文" : material.extractionMessage())
                    : metadataText + "\n\n资料正文：\n" + material.extractedText();
            List<String> materialChunks = chunks(sourceText);
            for (int index = 0; index < materialChunks.size(); index++) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("courseId", context.courseId());
                metadata.put("lessonId", material.lessonId() == null ? 0L : material.lessonId());
                metadata.put("resourceType", "MATERIAL");
                metadata.put("resourceId", String.valueOf(material.materialId()));
                metadata.put("title", material.name());
                metadata.put("locator", "material:" + material.materialId() + "#chunk-" + index);
                metadata.put("extractionStatus", material.extractionStatus() == null ? "UNKNOWN" : material.extractionStatus());
                documents.add(new Document(
                        documentId("course-%d-material-%d-chunk-%d".formatted(context.courseId(), material.materialId(), index)),
                        materialChunks.get(index), metadata));
            }
        }
        return documents;
    }

    private String documentId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<String> chunks(String content) {
        if (content.length() <= CHUNK_SIZE) return List.of(content);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            chunks.add(content.substring(start, end));
            if (end == content.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private AiCitationVO citation(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new AiCitationVO(
                String.valueOf(metadata.getOrDefault("resourceType", "LESSON")),
                String.valueOf(metadata.getOrDefault("resourceId", document.getId())),
                String.valueOf(metadata.getOrDefault("title", "课程知识片段")),
                String.valueOf(metadata.getOrDefault("locator", "document:" + document.getId())));
    }

    private record SyncState(int indexedChunks, OffsetDateTime lastSyncedAt) {}

    public record Retrieval(boolean vectorStoreAvailable, String context, List<AiCitationVO> citations) {
        static Retrieval unavailable() { return new Retrieval(false, "", List.of()); }
        public boolean matched() { return context != null && !context.isBlank(); }
    }
}
