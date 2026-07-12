package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.Size;

public record AiDraftInstructionRequest(@Size(max = 500) String instruction) {}
