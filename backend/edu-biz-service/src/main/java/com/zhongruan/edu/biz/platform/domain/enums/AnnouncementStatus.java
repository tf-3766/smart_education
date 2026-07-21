package com.zhongruan.edu.biz.platform.domain.enums;

public enum AnnouncementStatus {
    /** AI 自动流生成、尚未推送学生的草稿公告；教师确认后转 PUBLISHED 才通知学生。 */
    DRAFT,
    PUBLISHED,
    WITHDRAWN
}
