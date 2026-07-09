package com.zhongruan.edu.biz.forum.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.forum.infrastructure.persistence.entity.ForumTopicEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForumTopicMapper extends BaseMapper<ForumTopicEntity> {}
