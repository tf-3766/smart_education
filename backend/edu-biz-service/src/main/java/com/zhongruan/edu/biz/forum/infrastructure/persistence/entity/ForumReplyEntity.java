package com.zhongruan.edu.biz.forum.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_forum_reply")
public class ForumReplyEntity extends BaseAuditEntity {
    private Long topicId;
    private Long courseId;
    private Long authorId;
    private Long parentReplyId;
    private String content;
    private String status;
    private String moderationReason;
    private Long moderatedBy;
    private LocalDateTime moderatedAt;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
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
}
