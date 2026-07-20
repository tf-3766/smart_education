package com.zhongruan.edu.ai.tools;

import com.zhongruan.edu.feign.ai.AiCourseContextResponse;
import org.springframework.ai.tool.annotation.Tool;

public class CourseContextTools {
    private final AiCourseContextResponse context;

    public CourseContextTools(AiCourseContextResponse context) {
        this.context = context;
    }

    @Tool(name = "listCourseLessons", description = "列出当前已授权课程的课时标题；只用于回答课程学习问题")
    public String listCourseLessons() {
        return context.lessons().stream()
                .map(lesson -> "%s（lessonId=%s，状态=%s）".formatted(lesson.title(), lesson.lessonId(), lesson.status()))
                .reduce((left, right) -> left + "\n" + right).orElse("当前没有可用课时");
    }

    @Tool(name = "listCourseMaterials", description = "列出当前已授权课程的学习资料；只返回资料名称和定位信息")
    public String listCourseMaterials() {
        return context.materials().stream()
                .map(material -> "%s（materialId=%s，类型=%s）".formatted(
                        material.name(), material.materialId(), material.materialType()))
                .reduce((left, right) -> left + "\n" + right).orElse("当前没有可用资料");
    }
}
