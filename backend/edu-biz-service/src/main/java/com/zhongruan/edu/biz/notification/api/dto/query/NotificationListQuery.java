package com.zhongruan.edu.biz.notification.api.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class NotificationListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Pattern(regexp = "COURSE|ASSIGNMENT|EXAM|WARNING|SYSTEM", message = "category must be a supported notification category")
    private String category;
    private Boolean unread;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getUnread() { return unread; }
    public void setUnread(Boolean unread) { this.unread = unread; }
}
