package com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamAttemptEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamAttemptMapper extends BaseMapper<ExamAttemptEntity> {}
