package com.zhongruan.edu.biz.assignment.domain.enums;

public enum SubmissionStatus {
    DRAFT("草稿"),
    SUBMITTED("已提交"),
    GRADED("已批改"),
    RETURNED("已退回");

    private final String label;

    SubmissionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isFinalSubmission() {
        return this == SUBMITTED || this == GRADED;
    }
}
