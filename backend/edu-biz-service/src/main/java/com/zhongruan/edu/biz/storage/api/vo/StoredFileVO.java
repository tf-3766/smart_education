package com.zhongruan.edu.biz.storage.api.vo;

import java.time.OffsetDateTime;

public record StoredFileVO(
        String fileId,
        String originalName,
        String objectKey,
        String accessUrl,
        Long fileSize,
        String mimeType,
        String sha256,
        String purpose,
        OffsetDateTime uploadedAt,
        Integer version) {}
