package com.zhongruan.edu.biz.platform.api.controller;

import com.zhongruan.edu.biz.platform.api.vo.CourseCategoryVO;
import com.zhongruan.edu.biz.platform.application.service.CourseCategoryService;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/course-categories")
public class CourseCategoryController {
    private final CourseCategoryService service;
    private final RequestContextFactory contextFactory;

    public CourseCategoryController(CourseCategoryService service, RequestContextFactory contextFactory) {
        this.service = service;
        this.contextFactory = contextFactory;
    }

    @GetMapping
    public ApiResponse<List<CourseCategoryVO>> list(HttpServletRequest request) {
        return ApiResponse.success(service.listEnabled(), contextFactory.current(request).traceId());
    }
}
