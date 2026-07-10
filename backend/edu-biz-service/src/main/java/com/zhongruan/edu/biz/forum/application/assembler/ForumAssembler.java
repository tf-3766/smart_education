package com.zhongruan.edu.biz.forum.application.assembler;

import com.zhongruan.edu.biz.forum.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumReplyVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicDetailVO;
import com.zhongruan.edu.biz.forum.api.vo.ForumTopicListItemVO;
import com.zhongruan.edu.biz.forum.domain.enums.ForumContentStatus;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumReplyEntity;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumTopicEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class ForumAssembler {
    public ForumTopicListItemVO toListItem(ForumTopicEntity topic, String authorName) {
        return new ForumTopicListItemVO(
                id(topic.getId()),
                id(topic.getCourseId()),
                topic.getTitle(),
                id(topic.getAuthorId()),
                authorName,
                CodeLabelVO.of(ForumContentStatus.valueOf(topic.getStatus())),
                Integer.valueOf(1).equals(topic.getPinned()),
                topic.getReplyCount() == null ? 0 : topic.getReplyCount(),
                time(topic.getLastRepliedAt()),
                time(topic.getCreatedAt()),
                topic.getVersion());
    }

    public ForumTopicDetailVO toDetail(ForumTopicEntity topic, String authorName) {
        return new ForumTopicDetailVO(
                id(topic.getId()),
                id(topic.getCourseId()),
                topic.getTitle(),
                topic.getContent(),
                id(topic.getAuthorId()),
                authorName,
                CodeLabelVO.of(ForumContentStatus.valueOf(topic.getStatus())),
                time(topic.getCreatedAt()),
                topic.getVersion());
    }

    public ForumReplyVO toReply(ForumReplyEntity reply, String authorName) {
        return new ForumReplyVO(
                id(reply.getId()),
                id(reply.getTopicId()),
                id(reply.getCourseId()),
                id(reply.getAuthorId()),
                authorName,
                id(reply.getParentReplyId()),
                reply.getContent(),
                CodeLabelVO.of(ForumContentStatus.valueOf(reply.getStatus())),
                time(reply.getCreatedAt()),
                reply.getVersion());
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
