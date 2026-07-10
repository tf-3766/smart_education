package com.zhongruan.edu.biz.forum.api.vo;

import com.zhongruan.edu.biz.forum.domain.enums.ForumContentStatus;

public record CodeLabelVO(String code, String label) {
    public static CodeLabelVO of(ForumContentStatus status) {
        return new CodeLabelVO(status.name(), status.label());
    }
}
