package com.zhongruan.edu.biz.course.api.vo;

public record CourseMaterialVO(
        String materialId,
        String courseId,
        String chapterId,
        String lessonId,
        String name,
        CodeLabelVO materialType,
        String fileKey,
        String fileUrl,
        Long fileSize,
        String mimeType,
        CodeLabelVO visibility,
        CodeLabelVO status,
        Integer sortOrder,
        Integer version) {}
