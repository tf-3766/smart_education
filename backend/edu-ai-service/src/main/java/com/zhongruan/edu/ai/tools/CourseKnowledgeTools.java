package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService;
import com.zhongruan.edu.ai.knowledge.CourseKnowledgeBaseService.Retrieval;
import org.springframework.ai.tool.annotation.Tool;

/** 课程资料正文检索工具：让 LLM 按需从已授权课程的知识库中检索上传资料的正文片段。 */
public class CourseKnowledgeTools {
    private final CourseKnowledgeBaseService knowledgeBase;
    private final Long courseId;
    private final Long lessonId;

    public CourseKnowledgeTools(CourseKnowledgeBaseService knowledgeBase, Long courseId, Long lessonId) {
        this.knowledgeBase = knowledgeBase;
        this.courseId = courseId;
        this.lessonId = lessonId;
    }

    @Tool(name = "searchCourseKnowledge",
            description = "从当前已授权课程上传的资料正文中检索与问题相关的片段；回答涉及资料具体内容（而非仅文件名）的问题时必须调用，参数为检索关键词或问题")
    public String searchCourseKnowledge(String query) {
        Retrieval retrieval = knowledgeBase.retrieve(courseId, lessonId, query == null ? "" : query.trim());
        if (!retrieval.vectorStoreAvailable()) {
            return "课程知识库尚未配置向量检索，无法按资料正文回答。";
        }
        if (!retrieval.matched()) {
            return "未检索到与该问题相关的资料正文片段，请结合课程目录或提示用户上传相关资料。";
        }
        return retrieval.context();
    }
}
