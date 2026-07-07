package com.zhongruan.edu.biz.course.domain.enums;

import java.util.Set;

public enum CourseStatus implements LabeledEnum {
    DRAFT("草稿"),
    PENDING_REVIEW("待审核"),
    PUBLISHED("已发布"),
    ONGOING("进行中"),
    FINISHED("已结束"),
    OFFLINE("已下线");

    private final String label;

    CourseStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean canTransitionTo(CourseStatus target) {
        if (target == OFFLINE && this != OFFLINE) {
            return true;
        }
        return switch (this) {
            case DRAFT -> target == PENDING_REVIEW;
            case PENDING_REVIEW -> Set.of(DRAFT, PUBLISHED).contains(target);
            case PUBLISHED -> target == ONGOING;
            case ONGOING -> target == FINISHED;
            case FINISHED, OFFLINE -> false;
        };
    }

    public boolean isStudentVisible() {
        return this == PUBLISHED || this == ONGOING || this == FINISHED;
    }

    public boolean allowsNewEnrollment() {
        return this == PUBLISHED || this == ONGOING;
    }
}
