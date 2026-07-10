package com.zhongruan.edu.biz.assignment.api.vo;

public record AssignmentAttachmentVO(
        String attachmentId,
        String name,
        String fileId,
        String fileKey,
        String fileUrl,
        Long fileSize,
        String mimeType,
        Integer sortOrder) {}
