package com.zhongruan.edu.feign.ai;

/** AI 生成的题目选项草稿（跨服务契约，枚举以字符串传递）。 */
public record AiQuestionOptionDraft(
        String label,
        String content,
        Boolean correct,
        Integer sortOrder) {}
