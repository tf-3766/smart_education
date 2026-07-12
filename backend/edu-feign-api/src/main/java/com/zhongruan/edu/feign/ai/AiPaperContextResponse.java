package com.zhongruan.edu.feign.ai;

import java.util.List;

public record AiPaperContextResponse(
        Long courseId,
        String courseCode,
        String courseName,
        List<AiQuestionRef> questions) {
    public AiPaperContextResponse {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
