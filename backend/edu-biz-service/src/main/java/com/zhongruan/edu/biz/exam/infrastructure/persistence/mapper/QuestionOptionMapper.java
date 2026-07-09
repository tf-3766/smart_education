package com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.QuestionOptionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOptionEntity> {}
