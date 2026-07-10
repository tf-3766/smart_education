package com.zhongruan.edu.biz.warning.api.dto.request;

import com.zhongruan.edu.biz.warning.domain.enums.WarningType;
import java.util.List;

public record GenerateCourseWarningsRequest(
        List<WarningType> warningTypes,
        Long studentId,
        Boolean dryRun) {}
