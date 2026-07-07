package com.zhongruan.edu.biz.course.api.vo;

public record MaterialAccessVO(
        String materialId,
        String name,
        CodeLabelVO materialType,
        Long fileSize,
        String mimeType,
        String accessMode,
        String accessUrl) {}
