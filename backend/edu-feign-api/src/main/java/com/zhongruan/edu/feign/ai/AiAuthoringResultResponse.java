package com.zhongruan.edu.feign.ai;

/** AI 自动流落库结果。resourceId 为雪花 ID 字符串，避免精度丢失。 */
public record AiAuthoringResultResponse(
        String resourceType,
        String resourceId,
        String title,
        int itemCount) {}
