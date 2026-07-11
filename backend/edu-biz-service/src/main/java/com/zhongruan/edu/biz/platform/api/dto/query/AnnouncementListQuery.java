package com.zhongruan.edu.biz.platform.api.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class AnnouncementListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
