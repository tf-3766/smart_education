package com.zhongruan.edu.biz.forum.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_forum_topic")
public class ForumTopicEntity extends BaseAuditEntity {
    private Long courseId;
    private Long authorId;
    private String title;
    private String content;
    private String status;
    private String moderationReason;
    private Long moderatedBy;
    private LocalDateTime moderatedAt;
    private Integer pinned;
    private Integer replyCount;
    private LocalDateTime lastRepliedAt;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public Long getModeratedBy() { return moderatedBy; }
    public void setModeratedBy(Long moderatedBy) { this.moderatedBy = moderatedBy; }
    public LocalDateTime getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(LocalDateTime moderatedAt) { this.moderatedAt = moderatedAt; }
    public Integer getPinned() { return pinned; }
    public void setPinned(Integer pinned) { this.pinned = pinned; }
    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
    public LocalDateTime getLastRepliedAt() { return lastRepliedAt; }
    public void setLastRepliedAt(LocalDateTime lastRepliedAt) { this.lastRepliedAt = lastRepliedAt; }
}
