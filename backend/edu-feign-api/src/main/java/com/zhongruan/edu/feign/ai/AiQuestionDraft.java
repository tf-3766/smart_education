package com.zhongruan.edu.feign.ai;

import java.math.BigDecimal;
import java.util.List;

/** AI 生成的单道题目草稿。questionType/difficulty 用字符串，由 biz 侧映射为领域枚举。 */
public record AiQuestionDraft(
        String questionType,
        String stem,
        String analysis,
        String difficulty,
        BigDecimal score,
        List<AiQuestionOptionDraft> options) {}
