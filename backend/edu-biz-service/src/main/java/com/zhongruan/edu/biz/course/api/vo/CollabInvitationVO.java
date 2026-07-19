package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record CollabInvitationVO(
        String courseId,
        String courseName,
        String inviterId,
        String inviterName,
        OffsetDateTime invitedAt) {}
