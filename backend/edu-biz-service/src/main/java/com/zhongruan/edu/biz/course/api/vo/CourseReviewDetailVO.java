package com.zhongruan.edu.biz.course.api.vo;

import java.util.List;

public record CourseReviewDetailVO(CourseDetailVO course, List<CourseReviewVO> history) {
    public CourseReviewDetailVO {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
