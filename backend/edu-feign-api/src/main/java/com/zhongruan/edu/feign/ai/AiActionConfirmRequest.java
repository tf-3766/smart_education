package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.Size;

/** 高风险动作的强确认文本；普通显式确认可以不传。 */
public record AiActionConfirmRequest(@Size(max = 32) String confirmationText) {}
