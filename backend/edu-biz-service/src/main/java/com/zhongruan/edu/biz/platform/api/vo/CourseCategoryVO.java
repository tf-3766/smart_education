package com.zhongruan.edu.biz.platform.api.vo;

public record CourseCategoryVO(
        String categoryId,
        String name,
        Integer sortOrder,
        boolean enabled,
        Integer version) {}
