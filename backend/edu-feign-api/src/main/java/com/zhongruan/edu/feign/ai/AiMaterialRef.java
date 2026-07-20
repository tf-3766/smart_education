package com.zhongruan.edu.feign.ai;

public record AiMaterialRef(
        Long materialId,
        Long chapterId,
        Long lessonId,
        String name,
        String materialType,
        String fileKey,
        String fileUrl,
        String visibility,
        String status,
        String extractedText,
        String extractionStatus,
        String extractionMessage) {}
