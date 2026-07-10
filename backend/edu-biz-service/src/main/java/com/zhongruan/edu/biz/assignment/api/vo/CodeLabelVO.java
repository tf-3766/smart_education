package com.zhongruan.edu.biz.assignment.api.vo;

import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentAvailabilityStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;

public record CodeLabelVO(String code, String label) {
    public static CodeLabelVO of(AssignmentStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }

    public static CodeLabelVO of(AssignmentAvailabilityStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }

    public static CodeLabelVO of(SubmissionStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }
}
