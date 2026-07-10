package com.zhongruan.edu.biz.grade.api.vo;

import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import com.zhongruan.edu.biz.grade.domain.enums.GradeStatus;

public record CodeLabelVO(String code, String label) {
    public static CodeLabelVO of(SubmissionStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }

    public static CodeLabelVO of(GradeStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }
}
